package org.jwu.javadepend.resources;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.jwu.javadepend.App;
import org.jwu.javadepend.resources.data.GitHubSearchReturn;
import org.jwu.javadepend.resources.data.PackageDependents;
import org.jwu.javadepend.resources.data.PomInfo;
import org.jwu.javadepend.resources.data.ProjectInfo;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;

import java.sql.*;


/**
 * The Package class adds a set of /package/... endpoints, which allow a client to
 *  search for dependent repositories which depend on a specified Java artifact. 
 * 
 * This search can either be carried out using the latest data available on GitHub, with the
 * GitHub REST API. Or the search can be carried out using data held in the MySQL Cache.
 */
@Path("package")
public class Package {

    /**
     * getPackageDependencies adds an endpoint {groupId}/{artifactId}/dependents/remote which
     * searches pom.xml files hosted on Github to identify those dependent on the arfifact
     * specified in the URL string. 
     * 
     * TODO: add support for pagination of search results (if Github's API allows it)
     */
    @GET
    @Path("{groupId}/{artifactId}/dependents/remote")
    @Produces("application/json")
    public Response getPackageDependencies(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId) throws IOException {

        Client client = Client.create();

        // build a query string to search pom.xml files in github
        WebResource.Builder webResourceBuilder = client
                .resource("https://api.github.com/search/code?q=%22groupId+" + groupId +"+groupId%22%2B%22artifactId+" + artifactId + "+artifactid%22+in%3Afile+filename%3Apom+path%3A%2F")
                .header("Authorization", String.format("token %s", App.GIT_KEY))
                .accept("application/json");

        // execute the query and validate the response
        ClientResponse response = webResourceBuilder.get(ClientResponse.class);
        if (response.getStatus() != 200) {
            return Response.status(response.getStatus())
                    .build();
        }

        String output = response.getEntity(String.class);

        Gson g = new Gson();
        GitHubSearchReturn data = g.fromJson(output, GitHubSearchReturn.class);

        // analyse all objects returned from the HTTP query, and extract the listed repositories
        PackageDependents dependentResponse = new PackageDependents(data.total_count, data.incomplete_results);
        for (GitHubSearchReturn.SearchItem searchItem: data.items) {
            String org = searchItem.repository.owner.login;
            String project = searchItem.repository.name;
            String repo_name = org + "/" + project;
            String gitUrl = searchItem.repository.html_url;

            dependentResponse.addProject(new ProjectInfo(repo_name, gitUrl, null));
        }

        // return the identified repositories
        return Response.status(Response.Status.OK)
            .entity(dependentResponse)
            .build();
    }

    /**
     * getPackageDependencies adds an endpoint {groupId}/{artifactId}/dependents/remote which
     * searches pom.xml files hosted in the MySQL cache.
     */
    @GET
    @Path("{groupId}/{artifactId}/dependents/local")
    @Produces("application/json")
    public Response getPackageDependenciesFromMySQL(@PathParam("groupId") String groupId,
                                                    @PathParam("artifactId") String artifactId,
                                                    @DefaultValue("0") @QueryParam("start") int results_start,
                                                    @DefaultValue("10") @QueryParam("end") int results_end,
                                                    @DefaultValue("true") @QueryParam("projects") boolean fetchProjects,
                                                    @DefaultValue("false") @QueryParam("all") boolean fetchAllResults,
                                                    @DefaultValue("false") @QueryParam("pom") boolean parsePom) throws IOException {

        // construct a MySQL boolean string to idenfiy pom files which contain the specified artifact
        String booleanOperator = String.format("content LIKE '%%<dependency>%%<groupId>%s</groupId>%%<artifactId>%s</artifactId>%%</dependency>%%'", groupId, artifactId);

        try{
            // connect to the MySQL driver
            Class.forName("com.mysql.jdbc.Driver");
            Connection con= DriverManager.getConnection("jdbc:mysql://" + App.MYSQL_URL, App.MYSQL_USER,App.MYSQL_PASSWORD);

            Statement stmt=con.createStatement();

            int totalResults = 0;
            ResultSet rs=stmt.executeQuery("SELECT count(*) FROM `github-pom` WHERE " + booleanOperator);
            while(rs.next()) {
                totalResults = rs.getInt(1);
            }

            if (fetchProjects && fetchAllResults) {
                results_start = 1;
                results_end = totalResults;
            } else if (!fetchProjects) {
                results_start = 0;
                results_end = 0;
            }

            System.out.println("retrieved count");

            PackageDependents dependentResponse = new PackageDependents(totalResults, true);

            if (results_start >= 0 && results_end >= results_start) {
                rs = stmt.executeQuery(String.format("SELECT bigtable_id, content, repo_name FROM `github-pom` WHERE %s  ORDER BY id ASC LIMIT %d, %d", booleanOperator, results_start, results_end));
                while (rs.next()) {
                    String repoName = rs.getString("repo_name");
                    String gitUrl = String.format("https://github.com/%s.git", repoName);

                    PomInfo pomInfo = null;

                    if (parsePom) {
                        // if any error occurs when parsing the pom (i.e. because it is invalid) ensure the rest of the results are not affected
                        try {
                            pomInfo = SharedParsers.parsePom(rs.getAsciiStream("content"));
                        } catch (Throwable t){
                            System.out.println("Error occured with parsing POM " + t.toString());
                        }
                    }

                    ProjectInfo projectInfo = new ProjectInfo(repoName, gitUrl, pomInfo);
                    dependentResponse.addProject(projectInfo);
                }
            }

            con.close();

            return Response.status(Response.Status.OK)
                    .entity(dependentResponse)
                    .build();
        } catch(Exception e){
            System.out.println(e);
        }

        // fetch a list of projects that depend on the specified package
        // parse pom for their defined package names and return those too
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .build();
    }
}


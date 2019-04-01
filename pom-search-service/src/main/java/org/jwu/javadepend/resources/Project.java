package org.jwu.javadepend.resources;

import com.jcabi.github.*;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jwu.javadepend.App;
import org.jwu.javadepend.resources.data.PomInfo;
import org.jwu.javadepend.resources.data.ProjectInfo;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Project class manages api endpoints under /project/. It is responsible
 * for retrieving information about a specified github repository, and can retrieve
 * this information via either the github API, or the pom.xml cache hosted in the MySQL 
 * server.
 */
@Path("project")
public class Project {

    /**
     * 
     * retrieveProjectInfoFromGithub retrieves information about a repository from github, using the github API.
     * 
     * This information includes the artifacts produced by the project, and any dependencies the project declares
     * in its pom.xml file.
     *  */
    public ProjectInfo retrieveProjectInfoFromGithub(String org, String project) throws IOException, XmlPullParserException {
        Github github = new RtGithub(App.GIT_KEY);

        // fetch the repository from GitHub. If it doesn't exits,
        // an exception will be thrown
        Repo repo = github.repos().get( 
                new Coordinates.Simple(org + "/" + project)
        );

        // fetch the pom file from the top level directory of the repo. If it doesn't exist, an exception will be thrown
        Content pomFile = repo.contents().get("pom.xml"); // TODO: find POM anywhere in repo
        PomInfo pomInfo = SharedParsers.parsePom(pomFile.raw());

        // extract projectInfo  from the retrieved pom file
        ProjectInfo projectInfo = new ProjectInfo(SharedParsers.buildRepoName(org, project), repo.json().getString("clone_url"), pomInfo);

        return projectInfo;
    }

    /**
     * 
     * retrieveProjectInfoFromGithub retrieves information about a repository from the mysql cache.
     * 
     * This information includes the artifacts produced by the project, and any dependencies the project declares
     * in its pom.xml file.
     *  */
    public ProjectInfo retrieveProjectInfoFromMySQL(String org, String project) throws IOException, XmlPullParserException {
        ProjectInfo projectInfo = null;
        try{
            // connect to the mysql instance
            Class.forName("com.mysql.jdbc.Driver");
            Connection con= DriverManager.getConnection("jdbc:mysql://" + App.MYSQL_URL, App.MYSQL_USER,App.MYSQL_PASSWORD);

            Statement stmt=con.createStatement();

            String repoName = SharedParsers.buildRepoName(org, project);
            String gitUrl = String.format("https://github.com/%s.git", repoName);

            projectInfo = new ProjectInfo(SharedParsers.buildRepoName(org, project), gitUrl);
            
            // execute a query to identify the repository in MySQL and extract its pom.xml file content
            ResultSet rs=stmt.executeQuery(String.format("SELECT bigtable_id, content, repo_name FROM `github-pom` WHERE repo_name='%s'", SharedParsers.buildRepoName(org, project)));
            
            // for each pom file identified (there may be multiple in a single repository), parse it and
            // store the produced information
            while(rs.next()) {
                try {
                    // parse the retrieved pom file to extract information from it (artifacts produced, dependencies)
                    PomInfo pomInfo = SharedParsers.parsePom(rs.getAsciiStream("content"));
                    if (pomInfo != null) {
                        // add the pom to the project
                        projectInfo.addPom(pomInfo);
                    }
                } catch (Throwable t){
                    System.out.println("Error occured with parsing POM " + t.toString());
                }
            }

            con.close();
        } catch(Exception e){
            System.out.println(e);
        }

        if (projectInfo == null || projectInfo.pomInfo.size() == 0) {
            return null;
        }

        return projectInfo;
    }


    /**
     * getProjectInfo sets up an endpoint for users to request analysis
     * of a repository
     * 
     * The user specified the short url of the repository as part of the query URL - 
     * i.e. service:8080/java/project/repo_owner/repo_name
     * 
     * An optional parameter 'remote' can be used to specify whether the repository should be analysed
     * using data retrieved from GitHub, or held in the cache. This defaults to fetching from the cache
     */
    @GET
    @Path("{org}/{project}")
    @Produces("application/json")
    public Response getProjectInfo(@PathParam("org") String org,
                                   @PathParam("project") String project,
                                   @DefaultValue("false") @QueryParam("remote") boolean fetchFromRemote) {
        try {
            ProjectInfo retrievedProject = null;

            // fetch project data from either github or the mysql cache
            if (fetchFromRemote) {
                retrievedProject = this.retrieveProjectInfoFromGithub(org, project);
            } else {
                retrievedProject = this.retrieveProjectInfoFromMySQL(org, project);
            }

            // respond with the retrieved data
            return Response.status(Response.Status.OK)
                    .entity(retrievedProject)
                    .build();
        } catch (Throwable t) {
            // if any exception occurs, log it out, and return a NOT_FOUND
            // error to the user.
            // TODO: Return more specific error messages based on exception reason

            System.out.println(t.toString());

            return Response.status(Response.Status.NOT_FOUND)
                .build();
        }
    }
}

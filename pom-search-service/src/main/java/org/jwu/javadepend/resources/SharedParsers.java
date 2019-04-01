package org.jwu.javadepend.resources;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jwu.javadepend.resources.data.PackageInfo;
import org.jwu.javadepend.resources.data.PomInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SharedParsers contains a collection of shared functions related to 
 * pom file analysis, shared by a number of endpoints.
 */
public class SharedParsers {
    /**
     * buildRepoName constructs a repository short url from its org and repo name
     */
    public static String buildRepoName(String org, String repo) {
        return org + "/" + repo;
    }

    /**
     * resolvePomVar attempts to resolve a variable found within a pom.xml file,
     * which has been defined in another section of the file.
     */
    public static String resolvePomVar (String var, Properties modelProperties) {
        String result = var;

        if (var == null) {
            return null;
        }

        // if the string is actually a pom variable (i.e. ${version}) look
        // in the model properties of the pom file to attempt resolution. If this
        // fails, set the string to null
        Matcher matcher = Pattern.compile("\\$\\{(.*?)}").matcher(var);
        while(matcher.find()) {
            if (modelProperties.contains(matcher.group(1))) {
                result = modelProperties.get(matcher.group(1)).toString();
            } else {
                result = null;
            }
        }

        return result;
    }

    /**
     * parsePom uses the MavenXpp3Reader to parse a pom.xml file, to extract a list of dependencies
     * defined, as well as all artifacts produced. This information is returned as a PomInfo object.
     */
    public static PomInfo parsePom(InputStream pomFile) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();

            Model model = reader.read(pomFile);

            PomInfo pomInfo = new PomInfo();
            Properties modelProperties = model.getProperties();

            // extract all packages this maven project relies on as defined in the dependency managment section
            DependencyManagement dependencyManagement = model.getDependencyManagement();
            if (dependencyManagement != null) {
                List<Dependency> dependencyList = dependencyManagement.getDependencies();
                for (Dependency dependency : dependencyList) {
                    String groupId = resolvePomVar(dependency.getGroupId(), modelProperties);
                    String artifactId = resolvePomVar(dependency.getArtifactId(), modelProperties);
                    String version = resolvePomVar(dependency.getVersion(), modelProperties);

                    pomInfo.addDependency(new PackageInfo(groupId, artifactId, version));
                }
            }

            // extract all packages this maven project relies on as defined in the dependencies section
            for (Dependency dependency : model.getDependencies()) {


                String groupId = resolvePomVar(dependency.getGroupId(), modelProperties);
                String artifactId = resolvePomVar(dependency.getArtifactId(), modelProperties);
                String version = resolvePomVar(dependency.getVersion(), modelProperties);

                pomInfo.addDependency(new PackageInfo(groupId, artifactId, version));

            }

            // extract all modules this maven project may have
            // (maven allows for a number of different modules under a single pom file - see https://spring.io/guides/gs/multi-module/).
            List<String> modules = model.getModules();
            for(String module: modules) {
                String groupId = resolvePomVar(model.getGroupId(), modelProperties);
                String version = resolvePomVar(model.getVersion(), modelProperties);

                pomInfo.addPackage(new PackageInfo(groupId, module, version));
            }

            // if the maven file has a parent specified, some fields like groupId and version should be inherited
            // from the parent if they're not defined elsewhere
            String groupId = model.getGroupId();
            String version = model.getVersion();

            Parent parent = model.getParent();
            if (parent != null) {
                if (groupId == null) {
                    groupId = parent.getGroupId();
                }

                if (version == null) {
                    version = parent.getVersion();
                }
            }

            // add the parent package defined in this pom file
            pomInfo.addPackage(new PackageInfo(groupId, model.getArtifactId(), version));

            return pomInfo;
    }
}

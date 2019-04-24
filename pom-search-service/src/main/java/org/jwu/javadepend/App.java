/*
This work is Copyright 2019 Jacob Unwin, and released under the MIT license.

The original template Grizzly2 Server application used to produce this 
work is Copyright 2016 Janus Friis Nielsen.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jwu.javadepend;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import org.jwu.javadepend.resources.Parse;
import org.jwu.javadepend.resources.Project;
import org.jwu.javadepend.resources.Package;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extended from https://github.com/janusdn/jersey-server-grizzly2.
 * 
 * This webserver provides a search service, which can identify repositories
 * dependent on a specified Maven artifact. The identified repositories are searched
 * from a cache of repositories hosting Java Maven projects, held in a MySQL server.
 * 
 * This server must have a table, with one row per pom.xml file found,
 *  which contains the following columns:
 * 
 *      * bigtable_id - an identifier for the repository 
 *      * id - a unique integer id for the row
 *      * content - the content of the pom.xml file for the repository
 *      * repo_name - the name of the repository (its short URL, i.e. javaparser/javaparser)
 *      * path - the relative file path of the pom.xml file in the repository
 *      * ref
 *      * size - the size of the file in bytes
 * 
 */
public class App {

    private static final URI BASE_URI = URI.create("http://0.0.0.0:8082/java/"); // the api is accessible on 0.0.0.0:8082/java/

    public static final String GIT_KEY = System.getenv("GIT_KEY");
    public static final String MYSQL_URL = System.getenv("MYSQL_URL");
    public static final String MYSQL_USER = System.getenv("MYSQL_USER");
    public static final String MYSQL_PASSWORD = System.getenv("MYSQL_PASSWORD");

    /**
     * Main application entry point. This starts the server, and adds 
     * all endpoints.
     *
     * @param args application arguments.
     */
    public static void main(String[] args) {
        try {
            // create the http server
            final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, create(), false);
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    server.shutdownNow();
                }
            }));

            // start the server
            server.start();

            System.out.println(String.format("Application started."));

            Thread.currentThread().join();
        } catch (IOException | InterruptedException ex) {
            // log out any errors that occur
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * create() is called to specify the different resources to use, 
     * and the classes which define available endpoints. 
     * */
    public static ResourceConfig create() {
        // add the different endpoints
        final ResourceConfig resourceConfig = new ResourceConfig(Project.class, Package.class, Parse.class);
        resourceConfig.register(JacksonFeature.class);  //add the JacksonFeature resource required for JSON output
        resourceConfig.register(MultiPartFeature.class); // add the MultiPartFeature resource required to accept multi-part-form file uploads

        return resourceConfig;
    }
}

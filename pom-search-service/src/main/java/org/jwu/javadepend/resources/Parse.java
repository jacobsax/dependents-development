package org.jwu.javadepend.resources;


import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jwu.javadepend.resources.data.PomInfo;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * The parse class creates a /parse/ endpoint for users to request the parsing of a supplied 
 * build configuration file. Currently the only supported build configuration file is a pom.xml
 * file, but this could be expanded in the future.
 */
@Path("parse")
public class Parse {

    /**
     * uploadPom creates a /parse/pom HTTP endpoint. A user can POST a pom file
     * to this endpoint using multipart_form_data. 
     * 
     * The pom file is parsed, and artifacts produced, and dependencies identified,
     * are returned to the user
     */
    @POST
    @Path("/pom")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadPom(  @FormDataParam("file") InputStream fileInputStream) throws Exception {
        // confirm that a file has been uploaded
        if (fileInputStream == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            // parse the pom file
            PomInfo pomInfo = SharedParsers.parsePom(fileInputStream);

            // return the information retrieved from parsing
            return Response.status(Response.Status.OK)
                    .entity(pomInfo)
                    .build();
        } catch (Throwable t) {
            System.out.println(t.toString());
            t.printStackTrace();
            System.out.println(t.getStackTrace());
            // TODO: Improve error handling, provide detailed response of error cause, including run out of requests errors
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}

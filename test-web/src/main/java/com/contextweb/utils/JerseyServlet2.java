package com.contextweb.utils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Pavel Moukhataev
 */
@Path("/endpoint1")
public class JerseyServlet2 {

    @GET
    @Path("/name2")
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
        return "Jersey.endpoint1.test2";
    }
}

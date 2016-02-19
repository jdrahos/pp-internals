package com.contextweb.utils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Pavel Moukhataev
 */
@Path("/")
public class JerseyServlet {

    @GET
    @Path("/adn")
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
        return "Jersey.adn";
    }
}

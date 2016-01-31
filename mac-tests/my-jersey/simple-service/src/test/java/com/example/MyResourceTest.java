package com.example;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;

public class MyResourceTest {

    private HttpServer server;

    @Before
    public void setUp() throws Exception {
        // start the server
        server = Main.startServer();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    /**
     * Test to see that the message "Got it!" is sent in the response.
     */
    @Test
    public void testGetIt() {
        Client c = Client.create();
        WebResource r = c.resource(Main.BASE_URI);

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("p1", "v1");
        params.add("p2", "v2");

        ClientResponse response = r.path("/myresource")
                .queryParams(params)
                .get(ClientResponse.class);

        Assert.assertEquals("Got it! v1/v2", response.getEntity(String.class));
    }

}

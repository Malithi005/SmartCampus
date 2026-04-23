package com.smartcampus.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDiscovery(@javax.ws.rs.core.Context javax.ws.rs.core.UriInfo uriInfo) {
        Map<String, Object> discovery = new HashMap<>();
        discovery.put("version", "1.0.0");
        discovery.put("admin", "admin@smartcampus.com");
        
        String baseUri = uriInfo.getBaseUri().toString();
        
        Map<String, String> links = new HashMap<>();
        links.put("rooms", baseUri + "rooms");
        links.put("sensors", baseUri + "sensors");
        
        discovery.put("links", links);
        
        return Response.ok(discovery).build();
    }
}

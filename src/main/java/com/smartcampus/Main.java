package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;

public class Main {
    // Base URI the Grizzly HTTP server will listen on
    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    public static HttpServer startServer() {
        // Create a resource config that scans for JAX-RS resources and providers
        final ResourceConfig rc = new SmartCampusApplication();

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);

        // Add a shutdown hook to release the port when the JVM stops
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdownNow();
        }));

        return server;
    }

    public static void main(String[] args) throws IOException {
        final HttpServer server = startServer();
        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl\nHit enter to stop it...", BASE_URI));
        System.in.read();
        server.shutdownNow();
    }
}

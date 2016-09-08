package org.odata4j.test.integration;

import javax.ws.rs.core.UriBuilder;
import java.net.ServerSocket;

public final class UriProvider {
    public static String getEndpointUri(final String path) {
        try {
            ServerSocket socket = new ServerSocket(0);
            int port = socket.getLocalPort();
            socket.close();

            return UriBuilder.fromUri("http://localhost/")
                    .port(port)
                    .path(path)
                    .build()
                    .toString();
        } catch (Exception e){
            throw new RuntimeException("Failed to generate endpoint uri: " + e.getMessage(), e);
        }
    }
}

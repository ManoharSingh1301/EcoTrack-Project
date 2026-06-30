package com.ecotrack.communication.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Slf4j
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        String query = request.getURI().getQuery();
        if (query != null && query.contains("userId=")) {
            String userId = query.substring(query.indexOf("userId=") + 7);
            if (userId.contains("&")) {
                userId = userId.substring(0, userId.indexOf("&"));
            }
            try {
                // Validate the userId is a valid Long before accepting it.
                Long.parseLong(userId);
                final String finalUserId = userId;
                return () -> finalUserId;
            } catch (NumberFormatException e) {
                log.warn("Invalid userId '{}' supplied in WS handshake query — treating as anonymous.", userId);
            }
        }

        // No valid userId provided — return null so the connection is treated as
        // anonymous. The controller's principal null-check handles this safely,
        // and no NumberFormatException will occur.
        return null;
    }
}

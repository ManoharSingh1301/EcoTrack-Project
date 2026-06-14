package com.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;

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
            final String finalUserId = userId;
            return () -> finalUserId;
        }

        return () -> UUID.randomUUID().toString();
    }
}

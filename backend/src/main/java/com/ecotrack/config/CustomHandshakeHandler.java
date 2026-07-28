package com.ecotrack.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Derives the STOMP session Principal from the {@code userId} query parameter of
 * the WebSocket handshake URL (e.g. {@code /ws-chat?userId=42}). Invalid or
 * missing ids result in an anonymous connection (null principal), which the
 * chat controller handles safely.
 */
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
                Long.parseLong(userId);
                final String finalUserId = userId;
                return () -> finalUserId;
            } catch (NumberFormatException e) {
                log.warn("Invalid userId '{}' in WS handshake — treating as anonymous.", userId);
            }
        }
        return null;
    }
}

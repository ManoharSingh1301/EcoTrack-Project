package com.ecotrack.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Gateway-wide JWT authentication filter.
 *
 * Runs at order -1 (before routing) and enforces:
 *   • Public paths → pass through without any token check
 *   • All other paths → require a valid  Authorization: Bearer <token>  header
 *
 * On success the filter injects trusted headers downstream:
 *   • X-User-Id   — the user's database ID
 *   • X-Username  — the user's username
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    // ── Public paths that bypass JWT validation ────────────────────────────────
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/users/register",
            "/api/users/login",
            // Swagger / OpenAPI (accessed directly via service port, but allow here too)
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars",
            // Eureka / Actuator
            "/actuator",
            // WebSocket chat (authentication handled separately at the WS level)
            "/ws-chat",
            "/api/chat"
    );

    @Override
    public int getOrder() {
        return -1; // Run before all other filters
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // ── 1. Skip public paths ───────────────────────────────────────────────
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // ── 2. Extract Authorization header ───────────────────────────────────
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7); // strip "Bearer "

        // ── 3. Validate token ──────────────────────────────────────────────────
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid or expired JWT for path: {}", path);
            return unauthorized(exchange, "Invalid or expired token");
        }

        // ── 4. Extract claims and inject trusted headers downstream ────────────
        Claims claims = jwtUtil.extractClaims(token);
        String userId   = String.valueOf(claims.get("userId"));
        String username = claims.getSubject();

        log.debug("Authenticated request — userId: {}, username: {}, path: {}", userId, username, path);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id",  userId)
                .header("X-Username", username)
                // Strip any client-supplied X-User-Id to prevent header injection attacks
                .headers(headers -> headers.remove("X-Forwarded-User"))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Writes a structured JSON 401 response and short-circuits the filter chain.
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "status",    401,
                "error",     "Unauthorized",
                "message",   message,
                "path",      exchange.getRequest().getURI().getPath(),
                "timestamp", LocalDateTime.now().toString()
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException ex) {
            bytes = "{\"error\":\"Unauthorized\"}".getBytes();
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}

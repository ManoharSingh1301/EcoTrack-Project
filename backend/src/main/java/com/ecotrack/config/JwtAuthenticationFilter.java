package com.ecotrack.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * In-process JWT authentication filter — the monolith replacement for the old
 * API-gateway global filter.
 *
 * <p>For protected requests it validates the {@code Authorization: Bearer} token
 * and wraps the request so downstream controllers see trusted headers:</p>
 * <ul>
 *   <li>{@code X-User-Id}   — the authenticated user's database id</li>
 *   <li>{@code X-Username}  — the authenticated user's username</li>
 * </ul>
 *
 * <p>Any client-supplied {@code X-User-Id}/{@code X-Username} headers are ignored
 * (the wrapper only ever returns values derived from the verified token), which
 * prevents header-injection spoofing. Public paths and CORS pre-flight
 * ({@code OPTIONS}) requests pass through untouched.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    /** Paths that never require a token. */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/users/register",
            "/api/users/login",
            "/api/chat",      // REST chat history
            "/ws-chat",       // WebSocket/SockJS handshake
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars"
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // CORS pre-flight and public paths pass straight through.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            unauthorized(request, response, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid or expired JWT for path: {}", path);
            unauthorized(request, response, "Invalid or expired token");
            return;
        }

        Claims claims = jwtUtil.extractClaims(token);
        String userId = String.valueOf(claims.get("userId"));
        String username = claims.getSubject();

        // Populate the Spring Security context (so the request is authenticated)...
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                username, null, AuthorityUtils.NO_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ...and inject trusted headers the controllers read via @RequestHeader("X-User-Id").
        HttpServletRequest wrapped = new TrustedHeaderRequest(request, userId, username);
        filterChain.doFilter(wrapped, response);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private void unauthorized(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", message,
                "path", request.getRequestURI(),
                "timestamp", LocalDateTime.now().toString()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * Request wrapper that overrides the two trusted identity headers with
     * token-derived values and hides any client-supplied copies.
     */
    private static class TrustedHeaderRequest extends HttpServletRequestWrapper {

        private final String userId;
        private final String username;

        TrustedHeaderRequest(HttpServletRequest request, String userId, String username) {
            super(request);
            this.userId = userId;
            this.username = username;
        }

        @Override
        public String getHeader(String name) {
            if ("X-User-Id".equalsIgnoreCase(name)) {
                return userId;
            }
            if ("X-Username".equalsIgnoreCase(name)) {
                return username;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if ("X-User-Id".equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(userId));
            }
            if ("X-Username".equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(username));
            }
            return super.getHeaders(name);
        }
    }
}

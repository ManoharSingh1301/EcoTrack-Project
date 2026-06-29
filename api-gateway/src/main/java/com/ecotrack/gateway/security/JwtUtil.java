package com.ecotrack.gateway.security;

import com.ecotrack.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT utility for the API Gateway — validates token signatures and extracts claims.
 * No token generation here; only user-service generates tokens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns true if the token has a valid signature and is not expired.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Gateway JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Extracts all claims from a valid token.
     * Call only after validateToken() returns true.
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

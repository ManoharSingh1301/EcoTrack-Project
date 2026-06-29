package com.ecotrack.user.security;

import com.ecotrack.user.config.JwtConfig;
import com.ecotrack.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT utility — handles token generation and validation for user-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;

    // ── Key helper ─────────────────────────────────────────────────────────────
    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    // ── Token generation ───────────────────────────────────────────────────────

    /**
     * Generates a signed HS256 JWT containing userId, username, and email claims.
     *
     * @param user authenticated user entity
     * @return compact JWT string
     */
    public String generateToken(User user) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getExpirationMs());

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId",   user.getId())
                .claim("email",    user.getEmail())
                .claim("fullName", user.getFullName())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    // ── Token validation & extraction ──────────────────────────────────────────

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
            log.warn("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Extracts all claims from a validated token.
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

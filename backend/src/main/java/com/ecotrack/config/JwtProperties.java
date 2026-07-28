package com.ecotrack.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds {@code jwt.*} properties (sourced from environment variables / .env).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** Signing secret — loaded from the JWT_SECRET env variable. Must be at least 32 chars for HS256. */
    private String secret;

    /** Token lifetime in milliseconds (default 24h = 86_400_000 ms). */
    private long expirationMs = 86_400_000L;
}

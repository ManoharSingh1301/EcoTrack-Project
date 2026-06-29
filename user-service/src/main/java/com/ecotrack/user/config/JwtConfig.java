package com.ecotrack.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds 'jwt.*' properties (sourced from .env via spring-dotenv).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /** Signing secret — loaded from JWT_SECRET env variable. */
    private String secret;

    /** Token lifetime in milliseconds (default 24 h = 86 400 000 ms). */
    private long expirationMs = 86_400_000L;
}

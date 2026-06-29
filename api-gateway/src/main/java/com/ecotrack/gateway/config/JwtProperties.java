package com.ecotrack.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds 'jwt.*' from application.properties (values sourced from .env via spring-dotenv).
 * Must use the identical secret as user-service to verify tokens.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** Signing secret — must match the value used by user-service to sign tokens. */
    private String secret;

    /** Token lifetime in milliseconds (informational; gateway only validates expiry). */
    private long expirationMs = 86_400_000L;
}

package com.ecotrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single entry point for the consolidated EcoTrack backend.
 *
 * <p>This monolith replaces the previous discovery-server + api-gateway +
 * user-service + item-service + communication-service topology. All domains
 * (users, items, borrow requests, favorites, chat) now live in one Spring Boot
 * application backed by a single MySQL database, and JWT authentication is
 * enforced in-process by {@code JwtAuthenticationFilter}.</p>
 */
@SpringBootApplication
public class EcoTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcoTrackApplication.class, args);
    }
}

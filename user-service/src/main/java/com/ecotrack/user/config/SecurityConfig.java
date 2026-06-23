package com.ecotrack.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        // Allow public access to Swagger UI and OpenAPI docs
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/webjars/**")
                        .permitAll()
                        // Allow public access to user registration and login
                        .requestMatchers(
                                "/api/users/register",
                                "/api/users/login")
                        .permitAll()
                        // Allow internal service-to-service calls (e.g., Feign from item-service)
                        .requestMatchers(
                                "/api/users/{id}",
                                "/api/users/username/**")
                        .permitAll()
                        // Require authentication for all other endpoints
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}

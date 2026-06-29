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
                        // All JWT enforcement is handled at the API Gateway (JwtAuthenticationFilter).
                        // user-service itself permits all requests so that:
                        //   • Feign calls from item-service pass through without credentials
                        //   • Internal service-to-service traffic is never blocked here
                        .anyRequest().permitAll())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}


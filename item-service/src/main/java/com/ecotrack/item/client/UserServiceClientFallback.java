package com.ecotrack.item.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for UserServiceClient.
 * Called when user-service is unavailable or circuit breaker is open.
 */
@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public UserDto getUserById(Long id) {
        log.warn("⚠️ User-service unavailable. Returning fallback user for id: {}", id);
        return UserDto.builder()
                .id(id)
                .username("Unknown")
                .email("unknown@ecotrack.com")
                .fullName("Unknown User")
                .build();
    }
}

package com.ecotrack.item.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Isolates the Feign call to user-service in its own bean so that the
 * Resilience4j {@code @CircuitBreaker} proxy is actually applied.
 * (When this lived inside ItemService it was invoked via {@code this},
 * bypassing the proxy — the fallback never ran.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OwnerVerifier {

    private final UserServiceClient userServiceClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "verifyOwnerFallback")
    public UserDto verifyOwnerExists(Long ownerId) {
        log.info("Verifying owner exists via user-service: {}", ownerId);
        return userServiceClient.getUserById(ownerId);
    }

    /**
     * Graceful degradation: the owner is always the authenticated caller (already
     * verified at login), so we allow creation to proceed if user-service is down,
     * but log it loudly rather than silently pretending the call succeeded.
     */
    public UserDto verifyOwnerFallback(Long ownerId, Throwable ex) {
        log.warn("user-service unavailable ({}); proceeding without owner re-verification for ownerId {}",
                ex.getMessage(), ownerId);
        return UserDto.builder().id(ownerId).username("unverified").build();
    }
}

package com.ecotrack.user.service;

import com.ecotrack.user.dto.LoginRequest;
import com.ecotrack.user.dto.LoginResponse;
import com.ecotrack.user.dto.UserResponse;
import com.ecotrack.user.exception.AuthenticationException;
import com.ecotrack.user.exception.DuplicateResourceException;
import com.ecotrack.user.exception.ResourceNotFoundException;
import com.ecotrack.user.model.User;
import com.ecotrack.user.repository.UserRepository;
import com.ecotrack.user.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ── Mapper ─────────────────────────────────────────────────────────────────
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .address(user.getAddress())
                .phone(user.getPhone())
                .bio(user.getBio())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    // ── Queries ────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "users", key = "#id")
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserById(Long id) {
        log.debug("Fetching user by id: {}", id);
        return userRepository.findById(id).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByUsername(String username) {
        log.debug("Fetching user by username: {}", username);
        return userRepository.findByUsername(username).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email).map(this::toResponse);
    }

    @Transactional
    public UserResponse createUser(User user) {
        log.info("Creating new user with username: {}", user.getUsername());

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateResourceException("User", "username", user.getUsername());
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("User", "email", user.getEmail());
        }

        // Hash the password with BCrypt
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        log.info("User created successfully with id: {}", savedUser.getId());
        return toResponse(savedUser);
    }

    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public UserResponse updateUser(Long id, User userDetails) {
        log.info("Updating user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Validate email uniqueness only if it has changed
        if (!user.getEmail().equals(userDetails.getEmail()) &&
                userRepository.existsByEmail(userDetails.getEmail())) {
            throw new DuplicateResourceException("User", "email", userDetails.getEmail());
        }

        user.setFullName(userDetails.getFullName());
        user.setEmail(userDetails.getEmail());
        user.setAddress(userDetails.getAddress());
        user.setPhone(userDetails.getPhone());
        user.setBio(userDetails.getBio());

        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            // Hash the password with BCrypt before updating
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with id: {}", id);
        return toResponse(updatedUser);
    }

    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        userRepository.delete(user);
        log.info("User deleted successfully with id: {}", id);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest loginRequest) {
        log.info("Login attempt for username: {}", loginRequest.getUsername());

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));

        // Verify the password with BCrypt
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid username or password");
        }

        // Generate a signed JWT for the authenticated user
        String token = jwtUtil.generateToken(user);

        log.info("Login successful for user: {}", loginRequest.getUsername());
        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                "Login successful",
                token);
    }
}

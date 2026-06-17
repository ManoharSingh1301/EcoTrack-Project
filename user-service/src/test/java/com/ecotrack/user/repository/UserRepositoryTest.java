package com.ecotrack.user.repository;

import com.ecotrack.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@ecotrack.com");
        testUser.setPassword("hashedPassword");
        testUser.setFullName("Test User");
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("should find user by username")
    void shouldFindByUsername() {
        Optional<User> found = userRepository.findByUsername("testuser");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@ecotrack.com");
    }

    @Test
    @DisplayName("should return empty for non-existent username")
    void shouldReturnEmptyForNonExistentUsername() {
        Optional<User> found = userRepository.findByUsername("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should find user by email")
    void shouldFindByEmail() {
        Optional<User> found = userRepository.findByEmail("test@ecotrack.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("should check if username exists")
    void shouldCheckUsernameExists() {
        assertThat(userRepository.existsByUsername("testuser")).isTrue();
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("should check if email exists")
    void shouldCheckEmailExists() {
        assertThat(userRepository.existsByEmail("test@ecotrack.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@ecotrack.com")).isFalse();
    }

    @Test
    @DisplayName("should save and retrieve user with all fields")
    void shouldSaveAndRetrieveUser() {
        User newUser = new User();
        newUser.setUsername("fulluser");
        newUser.setEmail("full@ecotrack.com");
        newUser.setPassword("password");
        newUser.setFullName("Full User");
        newUser.setAddress("123 Eco Street");
        newUser.setPhone("+1234567890");
        newUser.setBio("Eco enthusiast");

        User saved = userRepository.save(newUser);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getAddress()).isEqualTo("123 Eco Street");
        assertThat(saved.getBio()).isEqualTo("Eco enthusiast");
    }
}

package com.ecotrack.user.service;

import com.ecotrack.user.dto.LoginRequest;
import com.ecotrack.user.dto.LoginResponse;
import com.ecotrack.user.dto.UserResponse;
import com.ecotrack.user.exception.AuthenticationException;
import com.ecotrack.user.exception.DuplicateResourceException;
import com.ecotrack.user.exception.ResourceNotFoundException;
import com.ecotrack.user.model.User;
import com.ecotrack.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@ecotrack.com");
        testUser.setPassword("hashedPassword");
        testUser.setFullName("Test User");
        testUser.setPhone("+1234567890");
    }

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsersTests {

        @Test
        @DisplayName("should return all users")
        void shouldReturnAllUsers() {
            User user2 = new User();
            user2.setId(2L);
            user2.setUsername("user2");

            when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));

            List<UserResponse> result = userService.getAllUsers();

            assertThat(result).hasSize(2);
            verify(userRepository).findAll();
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserByIdTests {

        @Test
        @DisplayName("should return user when found")
        void shouldReturnUserWhenFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            Optional<UserResponse> result = userService.getUserById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should return empty when user not found")
        void shouldReturnEmptyWhenNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<UserResponse> result = userService.getUserById(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createUser")
    class CreateUserTests {

        @Test
        @DisplayName("should create user successfully")
        void shouldCreateUserSuccessfully() {
            User newUser = new User();
            newUser.setUsername("newuser");
            newUser.setEmail("new@ecotrack.com");
            newUser.setPassword("plainPassword");
            newUser.setFullName("New User");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@ecotrack.com")).thenReturn(false);
            when(passwordEncoder.encode("plainPassword")).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            UserResponse result = userService.createUser(newUser);

            assertThat(result.getId()).isNotNull();
            assertThat(result.getUsername()).isEqualTo("newuser");
            verify(passwordEncoder).encode("plainPassword");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when username exists")
        void shouldThrowWhenUsernameExists() {
            User newUser = new User();
            newUser.setUsername("testuser");
            newUser.setEmail("new@ecotrack.com");
            newUser.setPassword("password");

            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(newUser))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("username");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when email exists")
        void shouldThrowWhenEmailExists() {
            User newUser = new User();
            newUser.setUsername("newuser");
            newUser.setEmail("test@ecotrack.com");
            newUser.setPassword("password");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@ecotrack.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(newUser))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email");
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUserTests {

        @Test
        @DisplayName("should update user successfully")
        void shouldUpdateUserSuccessfully() {
            User updateDetails = new User();
            updateDetails.setFullName("Updated Name");
            updateDetails.setEmail("updated@ecotrack.com");
            updateDetails.setPhone("+9876543210");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail("updated@ecotrack.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse result = userService.updateUser(1L, updateDetails);

            assertThat(result.getFullName()).isEqualTo("Updated Name");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            User updateDetails = new User();
            updateDetails.setFullName("Updated Name");

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(999L, updateDetails))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("should hash password when password is provided")
        void shouldHashPasswordWhenProvided() {
            User updateDetails = new User();
            updateDetails.setFullName("Same Name");
            updateDetails.setPassword("newPassword");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("newPassword")).thenReturn("newHashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.updateUser(1L, updateDetails);

            verify(passwordEncoder).encode("newPassword");
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUserTests {

        @Test
        @DisplayName("should delete user successfully")
        void shouldDeleteUserSuccessfully() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            userService.deleteUser(1L);

            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("should login successfully with correct credentials")
        void shouldLoginSuccessfully() {
            LoginRequest request = new LoginRequest("testuser", "correctPassword");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("correctPassword", "hashedPassword")).thenReturn(true);

            LoginResponse response = userService.login(request);

            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getMessage()).isEqualTo("Login successful");
        }

        @Test
        @DisplayName("should throw AuthenticationException for invalid username")
        void shouldThrowForInvalidUsername() {
            LoginRequest request = new LoginRequest("nonexistent", "password");

            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Invalid username or password");
        }

        @Test
        @DisplayName("should throw AuthenticationException for wrong password")
        void shouldThrowForWrongPassword() {
            LoginRequest request = new LoginRequest("testuser", "wrongPassword");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Invalid username or password");
        }
    }
}

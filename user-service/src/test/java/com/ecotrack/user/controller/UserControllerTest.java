package com.ecotrack.user.controller;

import com.ecotrack.user.dto.LoginRequest;
import com.ecotrack.user.dto.LoginResponse;
import com.ecotrack.user.dto.UserResponse;
import com.ecotrack.user.exception.AuthenticationException;
import com.ecotrack.user.exception.DuplicateResourceException;
import com.ecotrack.user.config.SecurityConfig;
import com.ecotrack.user.exception.GlobalExceptionHandler;
import com.ecotrack.user.exception.ResourceNotFoundException;
import com.ecotrack.user.model.User;
import com.ecotrack.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@DisplayName("UserController Integration Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        testUserResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@ecotrack.com")
                .fullName("Test User")
                .build();
    }

    @Nested
    @DisplayName("GET /api/users")
    class GetAllUsersTests {

        @Test
        @WithMockUser
        @DisplayName("should return all users")
        void shouldReturnAllUsers() throws Exception {
            when(userService.getAllUsers()).thenReturn(Arrays.asList(testUserResponse));

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].username", is("testuser")));
        }
    }

    @Nested
    @DisplayName("GET /api/users/{id}")
    class GetUserByIdTests {

        @Test
        @WithMockUser
        @DisplayName("should return user when found")
        void shouldReturnUserWhenFound() throws Exception {
            when(userService.getUserById(1L)).thenReturn(Optional.of(testUserResponse));

            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username", is("testuser")))
                    .andExpect(jsonPath("$.email", is("test@ecotrack.com")));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 404 when user not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(userService.getUserById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/users/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/users/register")
    class RegisterUserTests {

        @Test
        @DisplayName("should register user successfully")
        void shouldRegisterUserSuccessfully() throws Exception {
            when(userService.createUser(any(User.class))).thenReturn(testUserResponse);

            String requestJson = "{\"username\":\"newuser\",\"email\":\"new@ecotrack.com\",\"password\":\"password123\",\"fullName\":\"New User\"}";

            mockMvc.perform(post("/api/users/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username", is("testuser")));
        }

        @Test
        @DisplayName("should return 409 when username already exists")
        void shouldReturn409WhenDuplicate() throws Exception {
            when(userService.createUser(any(User.class)))
                    .thenThrow(new DuplicateResourceException("User", "username", "existing"));

            String requestJson = "{\"username\":\"existing\",\"email\":\"new@ecotrack.com\",\"password\":\"password123\",\"fullName\":\"New User\"}";

            mockMvc.perform(post("/api/users/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error", is("Conflict")));
        }
    }

    @Nested
    @DisplayName("POST /api/users/login")
    class LoginTests {

        @Test
        @DisplayName("should login successfully")
        void shouldLoginSuccessfully() throws Exception {
            LoginRequest request = new LoginRequest("testuser", "password123");
            LoginResponse response = new LoginResponse(1L, "testuser", "test@ecotrack.com", "Test User", "Login successful");

            when(userService.login(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/users/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("Login successful")));
        }

        @Test
        @DisplayName("should return 401 for invalid credentials")
        void shouldReturn401ForInvalidCredentials() throws Exception {
            LoginRequest request = new LoginRequest("testuser", "wrongpassword");

            when(userService.login(any(LoginRequest.class)))
                    .thenThrow(new AuthenticationException("Invalid username or password"));

            mockMvc.perform(post("/api/users/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error", is("Unauthorized")));
        }
    }
}

package com.ecotrack.user.dto;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class LoginResponse {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String message;

    public LoginResponse(Long userId, String username, String email, String fullName, String message) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

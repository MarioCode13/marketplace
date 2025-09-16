package dev.marketplace.marketplace.dto;

import java.util.UUID;

public class AuthResponseDto {
    private String token;
    private String email;
    private String role;
    private UUID userId;

    public AuthResponseDto(String token, String email, String role, UUID userId) {
        this.token = token;
        this.email = email;
        this.role = role;
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}

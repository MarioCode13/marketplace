package dev.marketplace.marketplace.dto;

public class AuthResponseDto {
    private String token;
    private String email;
    private String role;
    private Long userId; // Add userId here

    public AuthResponseDto(String token, String email, String role, Long userId) {
        this.token = token;
        this.email = email;
        this.role = role;
        this.userId = userId;
    }

    // Getters and setters

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}


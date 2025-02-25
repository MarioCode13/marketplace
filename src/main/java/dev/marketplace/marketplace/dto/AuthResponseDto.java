package dev.marketplace.marketplace.dto;

public class AuthResponseDto {
    private String token;
    private String email;
    private String role;

    public AuthResponseDto(String token, String email, String role) {
        this.token = token;
        this.email = email;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}

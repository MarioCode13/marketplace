package dev.marketplace.marketplace.controllers;

import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.security.JwtUtil;
import dev.marketplace.marketplace.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response) {
        String providedToken = body.get("token");
        String emailOrUsername = body.get("emailOrUsername");
        String password = body.get("password");

        String token;

        if (providedToken != null && !providedToken.isBlank()) {
            // Validate provided token
            if (!jwtUtil.validateToken(providedToken)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
            }
            token = providedToken;
        } else {
            // Fallback: authenticate with credentials and generate token
            Optional<User> userOpt = userService.authenticateUser(emailOrUsername, password);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
            }
            User user = userOpt.get();
            token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        }

        boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));

        ResponseCookie authCookie = ResponseCookie.from("auth-token", token)
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(isSecure ? "None" : "Lax")
                .maxAge(60 * 60 * 24 * 7)
                .path("/")
                .build();
        response.addHeader("Set-Cookie", authCookie.toString());

        // Set CSRF cookie
        String csrfToken = UUID.randomUUID().toString();
        ResponseCookie csrfCookie = ResponseCookie.from("XSRF-TOKEN", csrfToken)
                .httpOnly(false)
                .secure(isSecure)
                .sameSite(isSecure ? "None" : "Lax")
                .maxAge(60 * 60 * 24 * 7)
                .path("/")
                .build();
        response.addHeader("Set-Cookie", csrfCookie.toString());

        // Clear legacy cookie if present
        ResponseCookie legacy = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(isSecure ? "None" : "Lax")
                .maxAge(0)
                .path("/")
                .build();
        response.addHeader("Set-Cookie", legacy.toString());

        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response) {
        boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        // Clear auth-token
        ResponseCookie clearAuth = ResponseCookie.from("auth-token", "")
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(isSecure ? "None" : "Lax")
                .maxAge(0)
                .path("/")
                .build();
        response.addHeader("Set-Cookie", clearAuth.toString());

        // Clear XSRF token
        ResponseCookie clearXsrf = ResponseCookie.from("XSRF-TOKEN", "")
                .httpOnly(false)
                .secure(isSecure)
                .sameSite(isSecure ? "None" : "Lax")
                .maxAge(0)
                .path("/")
                .build();
        response.addHeader("Set-Cookie", clearXsrf.toString());

        return ResponseEntity.ok(Map.of("success", true));
    }
}



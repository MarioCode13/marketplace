package dev.marketplace.marketplace.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import dev.marketplace.marketplace.service.UserService;
import dev.marketplace.marketplace.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.http.Cookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public static class LoginRequest {
        public String emailOrUsername;
        public String password;
    }

    @PostMapping(value = "/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        if (loginRequest.emailOrUsername == null || loginRequest.emailOrUsername.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email or username is required");
        }
        if (loginRequest.password == null || loginRequest.password.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password is required");
        }
        var userOpt = userService.authenticateUser(loginRequest.emailOrUsername, loginRequest.password);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email/username or password");
        }
        var user = userOpt.get();
        String jwt = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        Cookie cookie = new Cookie("jwt", jwt);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to false for local development (change to true for production HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 1 week
        response.addCookie(cookie);
        return ResponseEntity.ok().body("Login successful");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to false for local development (change to true for production HTTPS)
        cookie.setPath("/");
        // If you set a domain for the login cookie, set it here as well
        // cookie.setDomain("yourdomain.com");
        cookie.setMaxAge(0); // Expire immediately
        response.addCookie(cookie);
        return ResponseEntity.ok().body("Logged out successfully");
    }

    @GetMapping("/csrf")
    public ResponseEntity<?> getCsrfToken(CsrfToken csrfToken) {
        System.out.println("DEBUG: /api/csrf endpoint hit, token=" + csrfToken.getToken());
        // The CSRF cookie will be set automatically by Spring Security
        return ResponseEntity.ok().body(csrfToken.getToken());
    }
}
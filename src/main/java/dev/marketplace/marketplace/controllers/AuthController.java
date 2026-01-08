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
@RequestMapping({"/api/auth", "/graphql/api/auth"})
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response) {
        // Debug: log incoming request details to trace which client is calling this endpoint
        org.slf4j.LoggerFactory.getLogger(AuthController.class).debug("Login request received: URL={}, Host={}, Origin={}, Referer={}, X-Forwarded-For={}, RemoteAddr={}",
                request.getRequestURL(), request.getHeader("Host"), request.getHeader("Origin"), request.getHeader("Referer"), request.getHeader("X-Forwarded-For"), request.getRemoteAddr());

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
        String host = request.getServerName();
        boolean isLocalhost = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);

        // Only set SameSite=None when the cookie will also be Secure (addSecure=true).
        // For localhost or non-secure requests use Lax so browsers accept the cookie in dev.
        boolean addSecure = isSecure && !isLocalhost;
        String sameSite = addSecure ? "None" : "Lax";

        ResponseCookie authCookie = ResponseCookie.from("auth-token", token)
                .httpOnly(true)
                .secure(addSecure)
                .sameSite(sameSite)
                .maxAge(60 * 60 * 24 * 7)
                .path("/")
                .build();
        String authHeader = authCookie.toString();
        // Log cookie header for local debugging
        org.slf4j.LoggerFactory.getLogger(AuthController.class).debug("Emitting Set-Cookie for auth-token: {}", authHeader);
        response.addHeader("Set-Cookie", authHeader);

        // For local dev, also set a non-HttpOnly cookie so SPA can read the token if browser blocked HttpOnly cookie
        if (isLocalhost) {
            ResponseCookie devCookie = ResponseCookie.from("auth-token-dev", token)
                    .httpOnly(false)
                    .secure(false)
                    // Use Lax for dev devCookie to avoid SameSite=None+Secure requirement in some browsers
                    .sameSite("Lax")
                    .maxAge(60 * 60 * 24 * 7)
                    .path("/")
                    .build();
            String devHeaderStr = devCookie.toString();
            org.slf4j.LoggerFactory.getLogger(AuthController.class).debug("Emitting Set-Cookie for auth-token-dev: {}", devHeaderStr);
            response.addHeader("Set-Cookie", devHeaderStr);
            // additionally include header so frontend can capture and set Authorization header if needed
            response.addHeader("X-DEV-AUTH-TOKEN", token);
        }

        // Set CSRF cookie
        String csrfToken = UUID.randomUUID().toString();
        ResponseCookie csrfCookie = ResponseCookie.from("XSRF-TOKEN", csrfToken)
                .httpOnly(false)
                .secure(addSecure)
                .sameSite(sameSite)
                .maxAge(60 * 60 * 24 * 7)
                .path("/")
                .build();
        String csrfHeaderStr = csrfCookie.toString();
        org.slf4j.LoggerFactory.getLogger(AuthController.class).debug("Emitting Set-Cookie for XSRF-TOKEN: {}", csrfHeaderStr);
        response.addHeader("Set-Cookie", csrfHeaderStr);
        // Also expose the CSRF token in a response header so frontend can read it when cookie isn't available
        response.addHeader("X-XSRF-TOKEN", csrfToken);

        // Clear legacy cookie if present
        ResponseCookie legacy = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(addSecure)
                .sameSite(sameSite)
                .maxAge(0)
                .path("/")
                .build();
        String legacyStr = legacy.toString();
        org.slf4j.LoggerFactory.getLogger(AuthController.class).debug("Emitting Set-Cookie for legacy jwt clear: {}", legacyStr);
        response.addHeader("Set-Cookie", legacyStr);

        // For dev/local, explicitly allow credentials from the frontend origin so browsers accept Set-Cookie on cross-port requests
        String origin = request.getHeader("Origin");
        if (origin != null && origin.contains("localhost")) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            // Allow the frontend to read our custom dev auth header
            // Expose both the dev auth token and XSRF token so frontend can read them in dev
            response.setHeader("Access-Control-Expose-Headers", "X-DEV-AUTH-TOKEN, X-XSRF-TOKEN");
        }

        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response) {
        boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        String host = request.getServerName();
        boolean isLocalhost = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
        boolean addSecure = isSecure && !isLocalhost;
        String sameSite = isSecure ? "None" : "Lax";

        // Clear auth-token
        ResponseCookie clearAuth = ResponseCookie.from("auth-token", "")
                .httpOnly(true)
                .secure(addSecure)
                .sameSite(sameSite)
                .maxAge(0)
                .path("/")
                .build();
        response.addHeader("Set-Cookie", clearAuth.toString());

        // Clear dev cookie on localhost
        if (isLocalhost) {
            ResponseCookie clearDev = ResponseCookie.from("auth-token-dev", "")
                    .httpOnly(false)
                    .secure(false)
                    .sameSite("Lax")
                    .maxAge(0)
                    .path("/")
                    .build();
            response.addHeader("Set-Cookie", clearDev.toString());
        }

        // Clear XSRF token
        ResponseCookie clearXsrf = ResponseCookie.from("XSRF-TOKEN", "")
                .httpOnly(false)
                .secure(addSecure)
                .sameSite(sameSite)
                .maxAge(0)
                .path("/")
                .build();
        response.addHeader("Set-Cookie", clearXsrf.toString());

        return ResponseEntity.ok(Map.of("success", true));
    }
}

package dev.marketplace.marketplace.controllers;

import dev.marketplace.marketplace.service.B2StorageService;
import dev.marketplace.marketplace.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/store")
public class StoreBrandingController {

    private static final Logger log = LoggerFactory.getLogger(StoreBrandingController.class);

    private final B2StorageService b2StorageService;
    private final JwtUtil jwtUtil;

    public StoreBrandingController(B2StorageService b2StorageService, JwtUtil jwtUtil) {
        this.b2StorageService = b2StorageService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/upload-logo")
    public ResponseEntity<String> uploadLogo(@RequestParam("image") MultipartFile image,
                                             @RequestHeader(value = "Authorization", required = false) String authHeader,
                                             HttpServletRequest request) {
        try {
            UUID userId = extractUserId(authHeader, request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: no token provided");
            }

            String fileName = "store-logos/" + UUID.randomUUID() + "_" + image.getOriginalFilename();
            String uploadedFileName = b2StorageService.uploadImage(fileName, image.getBytes());
            String preSignedUrl = b2StorageService.generatePreSignedUrl(uploadedFileName);

            return ResponseEntity.ok(preSignedUrl);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload logo: " + e.getMessage());
        }
    }

    @PostMapping("/upload-banner")
    public ResponseEntity<String> uploadBanner(@RequestParam("image") MultipartFile image,
                                               @RequestHeader(value = "Authorization", required = false) String authHeader,
                                               HttpServletRequest request) {
        try {
            UUID userId = extractUserId(authHeader, request);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: no token provided");
            }

            String fileName = "store-banners/" + UUID.randomUUID() + "_" + image.getOriginalFilename();
            String uploadedFileName = b2StorageService.uploadImage(fileName, image.getBytes());
            String preSignedUrl = b2StorageService.generatePreSignedUrl(uploadedFileName);

            return ResponseEntity.ok(preSignedUrl);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload banner: " + e.getMessage());
        }
    }

    /**
     * Extract userId from Authorization header or cookies.
     */
    private UUID extractUserId(String authHeader, HttpServletRequest request) {
        try {
            String token = null;
            String tokenSource = null;

            // 1️⃣ Try Authorization header
            if (authHeader != null && !authHeader.isBlank()) {
                token = authHeader.replaceFirst("(?i)Bearer\\s+", "");
                tokenSource = "header";
            }

            // 2️⃣ Fallback to cookies
            if (token == null || token.isBlank()) {
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie c : cookies) {
                        String name = c.getName();
                        if ("jwt".equals(name) || "auth-token".equals(name) || "auth-token-dev".equals(name)) {
                            token = c.getValue();
                            tokenSource = "cookie(" + name + ")";
                            break;
                        }
                    }
                }
            }

            log.debug("extractUserId tokenSource={} tokenPresent={}", tokenSource, token != null && !token.isBlank());

            // 3️⃣ If token exists, extract userId using JwtUtil which now returns UUID
            if (token != null && !token.isBlank()) {
                return jwtUtil.extractUserId(token);
            }

            return null; // no token found
        } catch (Exception e) {
            log.warn("Failed to extract userId from token: {}", e.getMessage());
            return null; // invalid token
        }
    }
}

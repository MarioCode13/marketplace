package dev.marketplace.marketplace.controllers;

import com.backblaze.b2.client.exceptions.B2Exception;
import dev.marketplace.marketplace.service.B2StorageService;
import dev.marketplace.marketplace.service.UserService;
import dev.marketplace.marketplace.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/users")
public class UserFileUploadController {

    private static final Logger log = LoggerFactory.getLogger(UserFileUploadController.class);

    private final UserService userService;
    private final B2StorageService b2StorageService;
    private final JwtUtil jwtUtil;

    public UserFileUploadController(UserService userService, B2StorageService b2StorageService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.b2StorageService = b2StorageService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile image) {
        try {
            String fileName = "listings/" + UUID.randomUUID() + "_" + image.getOriginalFilename();
            String uploadedFileName = b2StorageService.uploadImage(fileName, image.getBytes());
            String preSignedUrl = b2StorageService.generatePreSignedUrl(uploadedFileName);

            return ResponseEntity.ok(preSignedUrl);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload image: " + e.getMessage());
        }
    }



    @PostMapping("/upload-profile-image")
    public ResponseEntity<String> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request
    ) {
        try {
            // Extract JWT
            String token = null;
            String tokenSource = null;
            if (authHeader != null && !authHeader.isBlank()) {
                token = authHeader.replaceFirst("(?i)Bearer\\s+", "");
                tokenSource = "header";
            } else if (request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                    if ("auth-token".equals(c.getName()) || "jwt".equals(c.getName()) || "auth-token-dev".equals(c.getName())) {
                        token = c.getValue();
                        tokenSource = "cookie(" + c.getName() + ")";
                        break;
                    }
                }
            }

            log.debug("uploadProfileImage tokenSource={} tokenPresent={}", tokenSource, token != null);

            if (token == null || token.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: no token provided");
            }

            UUID userId = jwtUtil.extractUserId(token);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: invalid token (no userId)");
            }

            // Upload image via service (same as listings)
            userService.uploadProfileImage(userId, file);

            return ResponseEntity.ok("Profile image uploaded successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload profile image: " + e.getMessage());
        }
    }
}

package dev.marketplace.marketplace.controllers;

import dev.marketplace.marketplace.service.B2StorageService;
import dev.marketplace.marketplace.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequestMapping("/api/store")
public class StoreBrandingController {
    private final B2StorageService b2StorageService;
    private final JwtUtil jwtUtil;

    public StoreBrandingController(B2StorageService b2StorageService, JwtUtil jwtUtil) {
        this.b2StorageService = b2StorageService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/upload-logo")
    public ResponseEntity<String> uploadLogo(@RequestParam("image") MultipartFile image, @RequestHeader("Authorization") String authHeader) {
        try {
            // Optionally, validate user from authHeader
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
    public ResponseEntity<String> uploadBanner(@RequestParam("image") MultipartFile image, @RequestHeader("Authorization") String authHeader) {
        try {
            // Optionally, validate user from authHeader
            String fileName = "store-banners/" + UUID.randomUUID() + "_" + image.getOriginalFilename();
            String uploadedFileName = b2StorageService.uploadImage(fileName, image.getBytes());
            String preSignedUrl = b2StorageService.generatePreSignedUrl(uploadedFileName);
            return ResponseEntity.ok(preSignedUrl);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload banner: " + e.getMessage());
        }
    }
} 
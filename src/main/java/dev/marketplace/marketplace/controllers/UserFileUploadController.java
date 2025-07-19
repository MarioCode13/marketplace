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

@RestController
@RequestMapping("/api/users")
public class UserFileUploadController {

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

    @PostMapping("/upload-id-photo")
    public ResponseEntity<String> uploadIdPhoto(@RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtUtil.extractUserId(token);
            userService.uploadIdPhoto(userId, file);
            return ResponseEntity.ok("ID photo uploaded successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload ID photo: " + e.getMessage());
        }
    }

    @PostMapping("/upload-drivers-license")
    public ResponseEntity<String> uploadDriversLicense(@RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtUtil.extractUserId(token);
            userService.uploadDriversLicense(userId, file);
            return ResponseEntity.ok("Driver's license uploaded successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload driver's license: " + e.getMessage());
        }
    }

    @PostMapping("/upload-proof-of-address")
    public ResponseEntity<String> uploadProofOfAddress(@RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtUtil.extractUserId(token);
            userService.uploadProofOfAddress(userId, file);
            return ResponseEntity.ok("Proof of address uploaded successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload proof of address: " + e.getMessage());
        }
    }

    @PostMapping("/upload-profile-image")
    public ResponseEntity<String> uploadProfileImage(@RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtUtil.extractUserId(token);
            userService.uploadProfileImage(userId, file);
            return ResponseEntity.ok("Profile image uploaded successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload profile image: " + e.getMessage());
        }
    }
}

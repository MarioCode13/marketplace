package dev.marketplace.marketplace.controllers;

import com.backblaze.b2.client.exceptions.B2Exception;
import dev.marketplace.marketplace.service.B2StorageService;
import dev.marketplace.marketplace.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/users")
public class FileUploadController {

    private final UserService userService;
    private final B2StorageService b2StorageService;

    public FileUploadController(UserService userService, B2StorageService b2StorageService) {
        this.userService = userService;
        this.b2StorageService = b2StorageService;
    }

    @PostMapping("/{userId}/profile-image")
    public ResponseEntity<?> uploadProfileImage(
            @PathVariable Long userId,
            @RequestParam("image") MultipartFile image) {
        try {
            // Upload image and get the file path (e.g., listings/uuid_filename.png)
            String filePath = b2StorageService.uploadImage(image);

            // Generate a pre-signed URL to access it (optional: use permanent public link instead if you prefer)
            String imageUrl = b2StorageService.generatePreSignedUrl(filePath);

            // Save the image URL in your DB (not byte[])
            userService.saveUserProfileImage(userId, imageUrl);

            return ResponseEntity.ok("Profile image uploaded successfully");

        } catch (IOException | B2Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading profile image");
        }
    }
}

package dev.marketplace.marketplace.controllers;

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

    public FileUploadController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/{userId}/profile-image")
    public ResponseEntity<?> uploadProfileImage(
            @PathVariable Long userId,
            @RequestParam("image") MultipartFile image) {
        System.out.println("========= Inside uploadProfileImage =========");
        System.out.println("Received request for user ID: " + userId);
        System.out.println("File Name: " + image.getOriginalFilename());
        System.out.println("File Size: " + image.getSize());

        try {
            byte[] imageData = image.getBytes();
            userService.saveUserProfileImage(userId, imageData);
            return ResponseEntity.ok("Profile image uploaded successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading profile image");
        }
    }

}

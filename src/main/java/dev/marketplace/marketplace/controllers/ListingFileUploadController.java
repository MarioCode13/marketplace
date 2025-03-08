package dev.marketplace.marketplace.controllers;

import com.backblaze.b2.client.exceptions.B2Exception;
import dev.marketplace.marketplace.service.B2StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/listings")
public class ListingFileUploadController {

    private final B2StorageService b2StorageService;

    public ListingFileUploadController(B2StorageService b2StorageService) {
        this.b2StorageService = b2StorageService;
    }

    @PostMapping("/upload-images")
    public ResponseEntity<?> uploadListingImages(@RequestParam("images") MultipartFile[] images) {
        if (images == null || images.length == 0) {
            System.out.println("❌ No images received in the request!");
            return ResponseEntity.badRequest().body("No images uploaded!");
        }

        System.out.println("✅ Received " + images.length + " images for upload.");

        List<String> imageUrls = new ArrayList<>();
        try {
            for (MultipartFile image : images) {
                System.out.println("➡ Uploading file: " + image.getOriginalFilename() + " (Size: " + image.getSize() + " bytes)");
                String imageUrl = b2StorageService.uploadImage(image);
                System.out.println("✅ Successfully uploaded: " + imageUrl);
                imageUrls.add(imageUrl);
            }
            return ResponseEntity.ok(imageUrls);
        } catch (Exception e) {
            System.err.println("🔥 Image upload failed!");
            e.printStackTrace();  // Full error log
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading images: " + e.getMessage());
        }
    }
}

package dev.marketplace.marketplace.controllers;

import com.backblaze.b2.client.exceptions.B2Exception;
import dev.marketplace.marketplace.service.B2StorageService;
import dev.marketplace.marketplace.service.ListingService;
import dev.marketplace.marketplace.model.Listing;
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
    private final ListingService listingService;

    public ListingFileUploadController(B2StorageService b2StorageService, ListingService listingService) {
        this.b2StorageService = b2StorageService;
        this.listingService = listingService;
    }

    @PostMapping("/upload-images")
    public ResponseEntity<?> uploadImages(@RequestParam("images") MultipartFile[] images) {
        if (images == null || images.length == 0) {
            return ResponseEntity.badRequest().body("No images uploaded!");
        }
        List<String> uploadedUrls = new ArrayList<>();
        try {
            for (int i = 0; i < images.length; i++) {
                MultipartFile image = images[i];
                String fileName = "listings/temp/" + System.currentTimeMillis() + "_" + image.getOriginalFilename();
                String uploadedFileName = b2StorageService.uploadImage(fileName, image.getBytes());
                String preSignedUrl = b2StorageService.generatePreSignedUrl(uploadedFileName);
                uploadedUrls.add(preSignedUrl);
            }
            return ResponseEntity.ok(uploadedUrls);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading image: " + e.getMessage());
        }
    }

    @PostMapping("/{listingId}/upload-images")
    public ResponseEntity<?> uploadListingImages(@PathVariable Long listingId, @RequestParam("images") MultipartFile[] images) {
        if (images == null || images.length == 0) {
            return ResponseEntity.badRequest().body("No images uploaded!");
        }
        List<String> uploadedUrls = new ArrayList<>();

        try {
            for (MultipartFile image : images) {
                // Use the service, service returns the full URL
                String publicUrl = b2StorageService.uploadPublicImage("listings/" + listingId, image);
                uploadedUrls.add(publicUrl); // Store URL directly or also store filename if needed
            }

            // Optionally update listing with filenames if your DB stores them
            Listing listing = listingService.getListingByIdRaw(listingId);
            List<String> currentImages = listing.getImages();
            if (currentImages == null) currentImages = new ArrayList<>();
            // If you want to store filenames, you can strip folder prefix from URLs
            currentImages.addAll(uploadedUrls.stream().map(url -> url.substring(url.lastIndexOf("/") + 1)).toList());
            listing.setImages(currentImages);
            listingService.save(listing);

            return ResponseEntity.ok(uploadedUrls);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading image: " + e.getMessage());
        }
    }
}

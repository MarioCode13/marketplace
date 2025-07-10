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

    @PostMapping("/{listingId}/upload-images")
    public ResponseEntity<?> uploadListingImages(@PathVariable Long listingId, @RequestParam("images") MultipartFile[] images) {
        if (images == null || images.length == 0) {
            return ResponseEntity.badRequest().body("No images uploaded!");
        }
        List<String> uploadedFilenames = new ArrayList<>();
        try {
            for (int i = 0; i < images.length; i++) {
                MultipartFile image = images[i];
                String fileName = "listings/" + listingId + "/image" + (i + 1) + "_" + image.getOriginalFilename();
                String uploadedFileName = b2StorageService.uploadImage(fileName, image.getBytes());
                uploadedFilenames.add(uploadedFileName);
            }
            // Update the listing's images
            Listing listing = listingService.getListingByIdRaw(listingId);
            List<String> currentImages = listing.getImages();
            if (currentImages == null) currentImages = new ArrayList<>();
            currentImages.addAll(uploadedFilenames);
            listing.setImages(currentImages);
            listingService.save(listing);
            return ResponseEntity.ok(uploadedFilenames);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading image: " + e.getMessage());
        }
    }
}

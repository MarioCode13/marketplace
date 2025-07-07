package dev.marketplace.marketplace.service;

import com.backblaze.b2.client.exceptions.B2Exception;
import dev.marketplace.marketplace.model.Listing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ListingImageService {
    
    private final B2StorageService b2StorageService;
    private static final Logger logger = LoggerFactory.getLogger(ListingImageService.class);

    public ListingImageService(B2StorageService b2StorageService) {
        this.b2StorageService = b2StorageService;
    }

    /**
     * Converts a list of image filenames to pre-signed URLs
     */
    public List<String> generatePreSignedUrls(List<String> imageFilenames) {
        return imageFilenames.stream()
                .map(this::generatePreSignedUrl)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Generates a pre-signed URL for a single image filename
     */
    public String generatePreSignedUrl(String fileName) {
        try {
            return b2StorageService.generatePreSignedUrl(fileName);
        } catch (B2Exception e) {
            logger.warn("Failed to generate pre-signed URL for image: {}", fileName, e);
            return null;
        }
    }

    /**
     * Validates image filenames
     */
    public void validateImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("At least one image is required");
        }
        
        if (images.size() > 10) {
            throw new IllegalArgumentException("Maximum 10 images allowed per listing");
        }
        
        images.forEach(this::validateImageFilename);
    }

    /**
     * Validates a single image filename
     */
    private void validateImageFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Image filename cannot be empty");
        }
        
        if (!filename.matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException("Invalid image filename format");
        }
    }
} 
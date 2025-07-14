package dev.marketplace.marketplace.service;

import com.backblaze.b2.client.exceptions.B2Exception;
import dev.marketplace.marketplace.model.Listing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

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
     * Generates a pre-signed URL for a single image filename or URL
     */
    public String generatePreSignedUrl(String fileName) {
        // If it's already a pre-signed URL, return it as-is
        if (fileName != null && (fileName.startsWith("http://") || fileName.startsWith("https://"))) {
            return fileName;
        }
        
        // If it's a relative filename, generate a pre-signed URL
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
     * Validates a single image filename or URL
     */
    private void validateImageFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Image filename cannot be empty");
        }
        
        // Check if it's a URL (starts with http/https)
        if (filename.startsWith("http://") || filename.startsWith("https://")) {
            // Validate URL format
            try {
                new java.net.URL(filename);
                return; // Valid URL
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid image URL format");
            }
        }
        
        // Check if it's a valid filename format
        if (!filename.matches("^[a-zA-Z0-9._/-]+$")) {
            throw new IllegalArgumentException("Invalid image filename format");
        }
    }

    /**
     * Extracts filename from a B2 pre-signed URL
     * Example: https://f003.backblazeb2.com/file/bucket-name/listings/temp/123456_image.jpg?Authorization=...
     * Returns: listings/temp/123456_image.jpg
     */
    public String extractFilenameFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }
        
        // If it's already a filename (not a URL), return it as-is
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return url;
        }
        
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            String path = parsedUrl.getPath();
            
            // Remove the leading slash and bucket name
            // Path format: /file/bucket-name/filename
            String[] pathParts = path.split("/");
            if (pathParts.length >= 4) {
                // Skip "file" and bucket name, take the rest
                StringBuilder filename = new StringBuilder();
                for (int i = 3; i < pathParts.length; i++) {
                    if (i > 3) filename.append("/");
                    filename.append(pathParts[i]);
                }
                return filename.toString();
            }
            
            throw new IllegalArgumentException("Invalid B2 URL format");
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL format: " + e.getMessage());
        }
    }

    /**
     * Converts a list of URLs to filenames for database storage
     */
    public List<String> convertUrlsToFilenames(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return new ArrayList<>();
        }
        
        return urls.stream()
                .map(this::extractFilenameFromUrl)
                .toList();
    }
} 
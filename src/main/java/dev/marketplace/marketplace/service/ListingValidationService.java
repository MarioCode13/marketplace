package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.enums.Condition;
import org.springframework.stereotype.Service;

@Service
public class ListingValidationService {

    /**
     * Validates listing creation data (excluding images)
     */
    public void validateListingCreation(String title, String description, double price, 
                                       String location, Condition condition, String userId) {
        validateTitle(title);
        validateDescription(description);
        validatePrice(price);
        validateLocation(location);
        validateCondition(condition);
        validateUserId(userId);
    }

    /**
     * Validates listing update data
     */
    public void validateListingUpdate(Long listingId, Long userId) {
        validateListingId(listingId);
        validateUserId(userId);
    }

    /**
     * Validates price updates
     */
    public void validatePriceUpdate(double newPrice) {
        validatePrice(newPrice);
    }

    /**
     * Validates title updates
     */
    public void validateTitleUpdate(String newTitle) {
        validateTitle(newTitle);
    }

    /**
     * Validates description updates
     */
    public void validateDescriptionUpdate(String newDescription) {
        validateDescription(newDescription);
    }

    // Private validation methods
    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        
        if (title.length() < 3) {
            throw new IllegalArgumentException("Title must be at least 3 characters long");
        }
        
        if (title.length() > 100) {
            throw new IllegalArgumentException("Title must be less than 100 characters");
        }
    }

    private void validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        
        if (description.length() < 10) {
            throw new IllegalArgumentException("Description must be at least 10 characters long");
        }
        
        if (description.length() > 1000) {
            throw new IllegalArgumentException("Description must be less than 1000 characters");
        }
    }

    private void validatePrice(double price) {
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        
        if (price > 1000000) {
            throw new IllegalArgumentException("Price cannot exceed $1,000,000");
        }
    }

    private void validateLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("Location is required");
        }
        
        if (location.length() > 200) {
            throw new IllegalArgumentException("Location must be less than 200 characters");
        }
    }

    private void validateCondition(Condition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Condition is required");
        }
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        
        try {
            Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID format");
        }
    }

    private void validateListingId(Long listingId) {
        if (listingId == null) {
            throw new IllegalArgumentException("Listing ID is required");
        }
        
        if (listingId <= 0) {
            throw new IllegalArgumentException("Invalid listing ID");
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
    }
} 
package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.enums.Condition;
import org.springframework.stereotype.Service;

@Service
public class ListingValidationService {

    public void validateListingCreation(String title, String description, double price, 
                                       Long cityId, Condition condition, Long userId, String customCity) {
        validateTitle(title);
        validateDescription(description);
        validatePrice(price);
        validateCity(cityId, customCity);
        validateCondition(condition);
        validateUserId(userId);
    }

    public void validateListingUpdate(Long listingId, Long userId) {
        validateListingId(listingId);
        validateUserId(userId);
    }

    public void validatePriceUpdate(double newPrice) {
        validatePrice(newPrice);
    }

    public void validateTitleUpdate(String newTitle) {
        validateTitle(newTitle);
    }

    public void validateDescriptionUpdate(String newDescription) {
        validateDescription(newDescription);
    }

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

    private void validateListingId(Long listingId) {
        if (listingId == null) {
            throw new IllegalArgumentException("Listing ID is required");
        }

        if (listingId <= 0) {
            throw new IllegalArgumentException("Invalid listing ID");
        }
    }

    private void validateCity(Long cityId, String customCity) {
        boolean hasCityId = cityId != null;
        boolean hasCustomCity = customCity != null && !customCity.trim().isEmpty();

        if (!hasCityId && !hasCustomCity) {
            throw new IllegalArgumentException("Either a city or a custom city must be provided.");
        }

        if (hasCityId && hasCustomCity) {
            throw new IllegalArgumentException("Provide either a city or a custom city, not both.");
        }

        if (hasCustomCity && customCity.length() > 100) {
            throw new IllegalArgumentException("Custom city must be less than 100 characters.");
        }
    }

    private void validateCondition(Condition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Condition is required");
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
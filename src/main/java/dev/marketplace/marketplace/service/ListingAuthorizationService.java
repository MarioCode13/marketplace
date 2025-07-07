package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.ListingRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ListingAuthorizationService {
    
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public ListingAuthorizationService(ListingRepository listingRepository, UserRepository userRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    /**
     * Checks if a user can update a listing
     */
    public Listing checkUpdatePermission(Long listingId, Long userId) {
        Listing listing = findListingOrThrow(listingId);
        checkOwnership(listing, userId);
        return listing;
    }

    /**
     * Checks if a user can delete a listing
     */
    public Listing checkDeletePermission(Long listingId, Long userId) {
        Listing listing = findListingOrThrow(listingId);
        checkOwnership(listing, userId);
        return listing;
    }

    /**
     * Checks if a user can view a listing (for future use)
     */
    public Listing checkViewPermission(Long listingId, Long userId) {
        Listing listing = findListingOrThrow(listingId);
        
        // For now, anyone can view listings
        // In the future, you might add private listings or other restrictions
        return listing;
    }

    /**
     * Validates that a user exists
     */
    public User validateUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    }

    /**
     * Validates that a user exists by string ID
     */
    public User validateUserExists(String userId) {
        try {
            Long id = Long.parseLong(userId);
            return validateUserExists(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID format: " + userId);
        }
    }

    // Private helper methods
    private Listing findListingOrThrow(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found with ID: " + listingId));
    }

    private void checkOwnership(Listing listing, Long userId) {
        if (!listing.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only modify your own listings");
        }
    }
} 
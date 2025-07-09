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

    public Listing checkUpdatePermission(Long listingId, Long userId) {
        Listing listing = findListingOrThrow(listingId);
        checkOwnership(listing, userId);
        return listing;
    }

    public Listing checkDeletePermission(Long listingId, Long userId) {
        Listing listing = findListingOrThrow(listingId);
        checkOwnership(listing, userId);
        return listing;
    }

    public Listing checkViewPermission(Long listingId, Long userId) {
        Listing listing = findListingOrThrow(listingId);
        
        return listing;
    }

    public User validateUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    }

    public User validateUserExists(String userId) {
        try {
            Long id = Long.parseLong(userId);
            return validateUserExists(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID format: " + userId);
        }
    }

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
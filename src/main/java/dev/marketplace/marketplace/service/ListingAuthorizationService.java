package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.ListingRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import dev.marketplace.marketplace.repository.BusinessUserRepository;
import dev.marketplace.marketplace.model.BusinessUser;
import dev.marketplace.marketplace.model.Business;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ListingAuthorizationService {
    
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final BusinessUserRepository businessUserRepository;

    public ListingAuthorizationService(ListingRepository listingRepository, UserRepository userRepository, BusinessUserRepository businessUserRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.businessUserRepository = businessUserRepository;
    }

    public Listing checkUpdatePermission(UUID listingId, UUID userId) {
        Listing listing = findListingOrThrow(listingId);
        checkOwnership(listing, userId);
        return listing;
    }

    public Listing checkDeletePermission(UUID listingId, UUID userId) {
        Listing listing = findListingOrThrow(listingId);
        checkOwnership(listing, userId);
        return listing;
    }

    public Listing checkViewPermission(UUID listingId, UUID userId) {
        Listing listing = findListingOrThrow(listingId);
        
        return listing;
    }

    public User validateUserExists(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    }

    public User validateUserExists(String userId) {
        try {
            UUID id = UUID.fromString(userId);
            return validateUserExists(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user ID format: " + userId);
        }
    }

    private Listing findListingOrThrow(UUID listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found with ID: " + listingId));
    }

    private void checkOwnership(Listing listing, UUID userId) {
        if (!listing.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only modify your own listings");
        }
    }

    public Business getBusinessForUser(UUID userId) {
        User user = validateUserExists(userId);
        java.util.List<BusinessUser> associations = businessUserRepository.findByUser(user);
        if (associations == null || associations.isEmpty()) {
            return null;
        }
        // Return the first associated business (could be enhanced to select by role)
        return associations.get(0).getBusiness();
    }
}

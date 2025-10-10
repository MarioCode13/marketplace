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
        // Listing can be owned by a user OR a business. Handle both cases.
        if (listing.getUser() != null) {
            if (!listing.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("You can only modify your own listings");
            }
            return;
        }

        if (listing.getBusiness() != null) {
            // Validate user exists and check business ownership/association
            User user = validateUserExists(userId);
            Business business = listing.getBusiness();
            if (business.isOwner(user) || business.hasUser(user)) {
                return;
            }
            throw new AccessDeniedException("You are not authorized to modify listings for this business");
        }

        // No owner information on listing
        throw new AccessDeniedException("You can only modify your own listings");
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

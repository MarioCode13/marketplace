package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.enums.BusinessType;
import dev.marketplace.marketplace.enums.BusinessUserRole;
import dev.marketplace.marketplace.model.*;
import dev.marketplace.marketplace.repository.BusinessRepository;
import dev.marketplace.marketplace.repository.BusinessTrustRatingRepository;
import dev.marketplace.marketplace.repository.BusinessUserRepository;
import dev.marketplace.marketplace.repository.ListingRepository;
import dev.marketplace.marketplace.repository.ReviewRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {
    
    private final BusinessRepository businessRepository;
    private final BusinessUserRepository businessUserRepository;
    private final ListingRepository listingRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ListingImageService listingImageService;
    private final B2StorageService b2StorageService;
    private final ReviewRepository reviewRepository;
    private final BusinessTrustRatingRepository businessTrustRatingRepository;
    private final SubscriptionService subscriptionService;

    public Optional<Business> findById(UUID id) {
        return businessRepository.findById(id);
    }
    
    public Optional<Business> findBySlug(String slug) {
        return businessRepository.findBySlug(slug);
    }

    @Transactional
    public Business createBusiness(Business business) {
        log.info("Creating business: {}", business.getName());

        if (businessRepository.existsByEmail(business.getEmail())) {
            throw new IllegalArgumentException("Business email already exists: " + business.getEmail());
        }

        if (business.getBusinessEmail() == null || business.getBusinessEmail().isBlank()) {
            throw new IllegalArgumentException("Business email is required for verification.");
        }

        business.setEmailVerificationToken(UUID.randomUUID().toString());
        business.setEmailVerified(false);
        // If businessType is not set, default to RESELLER
        if (business.getBusinessType() == null) {
            business.setBusinessType(BusinessType.RESELLER);
        }
        Business savedBusiness = businessRepository.save(business);

        BusinessUser ownerRelation = new BusinessUser();
        ownerRelation.setBusiness(savedBusiness);
        ownerRelation.setUser(business.getOwner());
        ownerRelation.setRole(BusinessUserRole.OWNER);
        businessUserRepository.save(ownerRelation);

        // Delete the owner's personal listings because the user account has been converted to a business account
        try {
            UUID ownerId = business.getOwner() != null ? business.getOwner().getId() : null;
            if (ownerId != null) {
                log.info("Deleting personal listings for user {} as they become a business owner", ownerId);
                java.util.List<dev.marketplace.marketplace.model.Listing> ownerListings = listingRepository.findAllByUserId(ownerId);
                if (!ownerListings.isEmpty()) {
                    // First remove images from B2 for each listing
                    ownerListings.forEach(l -> {
                        if (l.getImages() != null) {
                            l.getImages().forEach(img -> {
                                try {
                                    String filename = listingImageService.extractFilenameFromUrl(img);
                                    b2StorageService.deleteImage(filename);
                                } catch (Exception e) {
                                    log.warn("Failed to delete listing image from B2: {} (listing={}), error={}", img, l.getId(), e.getMessage());
                                }
                            });
                        }
                    });
                     listingRepository.deleteAll(ownerListings);
                     log.info("Deleted {} listings for user {}", ownerListings.size(), ownerId);
                 }
             }
         } catch (Exception ex) {
             log.warn("Failed to delete personal listings for new business owner: {}", ex.getMessage());
             // Swallowing exception to avoid failing business creation, but log it for investigation
         }

        notificationService.sendBusinessVerificationEmail(
            business.getBusinessEmail(),
            business.getEmailVerificationToken(),
            business.getName()
        );
        return savedBusiness;
    }
    
    @Transactional
    public void inviteUserToBusiness(UUID businessId, String userEmail, User inviter) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new IllegalArgumentException("Business not found"));
        if (business.getBusinessType() == BusinessType.RESELLER) {
            throw new IllegalArgumentException("Reseller businesses cannot have team members.");
        }
        User userToInvite = userService.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("User with email does not exist: " + userEmail));
        if (businessUserRepository.existsByBusinessAndUser(business, userToInvite)) {
            throw new IllegalArgumentException("User is already linked to this business");
        }
        notificationService.sendBusinessInviteNotification(userToInvite, business, inviter);
    }

    @Transactional
    public BusinessUser linkUserToBusiness(UUID businessId, User user, BusinessUserRole role, User requestingUser) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));
        if (business.getBusinessType() == BusinessType.RESELLER) {
            throw new IllegalArgumentException("Reseller businesses cannot have team members.");
        }
        if (!business.canUserEditBusiness(requestingUser)) {
            throw new IllegalArgumentException("User does not have permission to manage this business");
        }
        if (businessUserRepository.existsByBusinessAndUser(business, user)) {
            throw new IllegalArgumentException("User is already linked to this business");
        }
        BusinessUser businessUser = new BusinessUser();
        businessUser.setBusiness(business);
        businessUser.setUser(user);
        businessUser.setRole(role);
        return businessUserRepository.save(businessUser);
    }

    @Transactional
    public void changeUserRole(UUID businessId, UUID userId, BusinessUserRole newRole, User changer) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));
        if (business.getBusinessType() == BusinessType.RESELLER) {
            throw new IllegalArgumentException("Reseller businesses cannot have team members.");
        }
        User user = userService.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Optional<BusinessUser> buOpt = businessUserRepository.findByBusinessAndUser(business, user);
        if (buOpt.isEmpty()) {
            throw new IllegalArgumentException("User not linked to business");
        }
        BusinessUser bu = buOpt.get();
        bu.setRole(newRole);
        businessUserRepository.save(bu);
        notificationService.sendRoleChangeNotification(bu.getUser(), bu.getBusiness(), newRole, changer);
    }

    @Transactional
    public Business updateBusiness(Business business, User requestingUser) {
        
        Business existingBusiness = businessRepository.findById(business.getId())
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + business.getId()));
        

        if (!existingBusiness.canUserEditBusiness(requestingUser)) {
            throw new IllegalArgumentException("User does not have permission to edit this business");
        }

        if (!existingBusiness.getEmail().equals(business.getEmail()) && 
            businessRepository.existsByEmailAndIdNot(business.getEmail(), business.getId())) {
            throw new IllegalArgumentException("Business email already exists: " + business.getEmail());
        }
        // Slug uniqueness validation
        if (business.getSlug() != null && !business.getSlug().equals(existingBusiness.getSlug()) &&
            businessRepository.existsBySlugAndIdNot(business.getSlug(), business.getId())) {
            throw new IllegalArgumentException("Business slug already exists: " + business.getSlug());
        }
        existingBusiness.setName(business.getName());
        existingBusiness.setEmail(business.getEmail());
        existingBusiness.setContactNumber(business.getContactNumber());
        existingBusiness.setAddressLine1(business.getAddressLine1());
        existingBusiness.setAddressLine2(business.getAddressLine2());
        existingBusiness.setCity(business.getCity());
        existingBusiness.setPostalCode(business.getPostalCode());
        if (business.getSlug() != null) {
            existingBusiness.setSlug(business.getSlug());
        }
        return businessRepository.save(existingBusiness);
    }
    
    @Transactional
    public void unlinkUserFromBusiness(UUID businessId, User user, User requestingUser) {
        log.info("Unlinking user {} from business {}", user.getId(), businessId);
        
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));

        if (!business.canUserEditBusiness(requestingUser)) {
            throw new IllegalArgumentException("User does not have permission to manage this business");
        }

        // Prevent removing the last owner
        if (business.getOwner().getId().equals(user.getId())) {
            long ownerCount = businessUserRepository.countOwnersByBusiness(business);
            if (ownerCount <= 1) {
                throw new IllegalArgumentException("Cannot remove the last owner from business");
            }
        }
        
        businessUserRepository.deleteByBusinessAndUser(business, user);
    }
    
    @Transactional
    public void transferOwnership(UUID businessId, User newOwner, User requestingUser) {
        log.info("Transferring ownership of business {} to user {}", businessId, newOwner.getId());
        
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));
        
        // Only current owner can transfer ownership
        if (!business.isOwner(requestingUser)) {
            throw new IllegalArgumentException("Only the current owner can transfer ownership");
        }
        
        // Check if new owner is already linked to business
        Optional<BusinessUser> existingRelation = businessUserRepository.findByBusinessAndUser(business, newOwner);
        if (existingRelation.isPresent()) {
            // Update existing relation to owner
            existingRelation.get().setRole(BusinessUserRole.OWNER);
            businessUserRepository.save(existingRelation.get());
        } else {
            // Create new owner relation
            BusinessUser newOwnerRelation = new BusinessUser();
            newOwnerRelation.setBusiness(business);
            newOwnerRelation.setUser(newOwner);
            newOwnerRelation.setRole(BusinessUserRole.OWNER);
            businessUserRepository.save(newOwnerRelation);
        }
        
        // Update business owner
        business.setOwner(newOwner);
        businessRepository.save(business);
    }
    
    public List<BusinessUser> getBusinessUsers(UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));
        return businessUserRepository.findByBusiness(business);
    }
    
    public boolean canUserCreateListingsForBusiness(User user, UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));
        return business.canUserCreateListings(user);
    }

    public Optional<Business> findOwnedByUser(User user) {
        return businessRepository.findOwnedByUser(user);
    }

    public List<Business> findByUser(User user) {
        return businessRepository.findByUser(user);
    }

    public BusinessTrustRating getBusinessTrustRating(UUID businessId) {
        Optional<BusinessTrustRating> trustRatingOpt = businessTrustRatingRepository.findByBusinessId(businessId);
        if (trustRatingOpt.isPresent()) {
            return trustRatingOpt.get();
        }
        // Fallback: calculate from reviews if not found in DB
        BigDecimal avgRating = reviewRepository.getAverageRatingByBusinessId(businessId);
        Long reviewCount = reviewRepository.countReviewsByBusinessId(businessId);
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));
        return BusinessTrustRating.builder()
                .business(business)
                .overallScore(avgRating != null ? avgRating : BigDecimal.ZERO)
                .totalReviews(reviewCount != null ? reviewCount.intValue() : 0)
                .averageRating(avgRating != null ? avgRating.doubleValue() : 0.0)
                .reviewCount(reviewCount != null ? reviewCount.intValue() : 0)
                .build();
    }

    public List<Listing> getListingsForBusiness(UUID businessId) {
        return listingRepository.findByBusinessId(businessId);
    }

    public Optional<dev.marketplace.marketplace.model.Subscription> getActiveSubscriptionForBusiness(UUID businessId) {
        return subscriptionService.getActiveSubscriptionForBusiness(businessId);
    }
}

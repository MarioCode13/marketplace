package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.enums.BusinessType;
import dev.marketplace.marketplace.enums.BusinessUserRole;
import dev.marketplace.marketplace.model.*;
import dev.marketplace.marketplace.repository.BusinessRepository;
import dev.marketplace.marketplace.repository.BusinessTrustRatingRepository;
import dev.marketplace.marketplace.repository.BusinessUserRepository;
import dev.marketplace.marketplace.repository.ListingRepository;
import dev.marketplace.marketplace.repository.ReviewRepository;
import dev.marketplace.marketplace.repository.SubscriptionRepository;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final SubscriptionRepository subscriptionRepository;

    public Optional<Business> findById(UUID id) {
        return businessRepository.findById(id);
    }
    
    public Optional<Business> findBySlug(String slug) {
        return businessRepository.findBySlug(slug);
    }

    @Transactional
    public Business createBusiness(Business business) {
        return createBusiness(business, true);
    }

    /**
     * Create a business. If persistFullProfile is false, create as a draft: don't delete personal listings or send verification.
     */
    @Transactional
    public Business createBusiness(Business business, boolean persistFullProfile) {
        log.info("Creating business: {} (persistFullProfile={})", business.getName(), persistFullProfile);

        if (businessRepository.existsByEmail(business.getEmail())) {
            throw new IllegalArgumentException("Business email already exists: " + business.getEmail());
        }

        if (business.getBusinessEmail() == null || business.getBusinessEmail().isBlank()) {
            throw new IllegalArgumentException("Business email is required for verification.");
        }

        // Ensure we have a slug (generate if missing) to satisfy DB non-null/unique constraints
        if (business.getSlug() == null || business.getSlug().isBlank()) {
            String base = business.getName() != null && !business.getName().isBlank() ? business.getName() : business.getEmail();
            String candidate = generateSlug(base);
            int attempts = 0;
            while (businessRepository.findBySlug(candidate).isPresent() && attempts < 10) {
                candidate = generateSlug(base + " " + randomSuffix());
                attempts++;
            }
            // if still exists after attempts, append UUID
            if (businessRepository.findBySlug(candidate).isPresent()) {
                candidate = generateSlug(base + " " + UUID.randomUUID().toString().substring(0, 8));
            }
            business.setSlug(candidate);
            log.info("Generated slug for business draft: {}", candidate);
        }

        business.setEmailVerificationToken(UUID.randomUUID().toString());
        business.setEmailVerified(false);
        business.setProfileCompleted(persistFullProfile);
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

        if (persistFullProfile) {
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
        } else {
            log.info("Created business as draft (profileCompleted=false) for owner {}. Verification email suppressed.", business.getOwner() != null ? business.getOwner().getId() : null);
        }

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
        List<BusinessUser> users = businessUserRepository.findByBusiness(business);
        // Log details to help debug null email issue during GraphQL resolution
        for (BusinessUser bu : users) {
            User u = bu.getUser();
            log.debug("Fetched BusinessUser id={} user.id={} user.email={}", bu.getId(), u != null ? u.getId() : null, u != null ? u.getEmail() : null);
        }
        return users;
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
        return subscriptionRepository.findByBusinessIdAndStatusIn(
                businessId,
                List.of(dev.marketplace.marketplace.model.Subscription.SubscriptionStatus.ACTIVE, dev.marketplace.marketplace.model.Subscription.SubscriptionStatus.TRIAL)
        );
    }

    // Helper to make a URL-friendly slug from a string
    private String generateSlug(String input) {
        if (input == null) return "business" + System.currentTimeMillis();
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        // remove diacritics
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // replace non-alphanumeric with hyphens
        normalized = normalized.replaceAll("[^\\p{Alnum}]+", "-");
        // collapse hyphens
        normalized = normalized.replaceAll("-+", "-");
        // trim hyphens
        normalized = normalized.replaceAll("^-|-$", "");
        String slug = normalized.toLowerCase();
        if (slug.length() > 50) slug = slug.substring(0, 50);
        if (slug.isBlank()) slug = "business-" + System.currentTimeMillis();
        return slug;
    }

    private String randomSuffix() {
        int leftLimit = 48; // '0'
        int rightLimit = 122; // 'z'
        int targetStringLength = 4;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        while (buffer.length() < targetStringLength) {
            int randomLimitedInt = leftLimit + (int) (random.nextFloat() * (rightLimit - leftLimit + 1));
            char c = (char) randomLimitedInt;
            if (Character.isLetterOrDigit(c)) buffer.append(c);
        }
        return buffer.toString().toLowerCase();
    }
}

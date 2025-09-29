package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.dto.BusinessTrustRatingDTO;
import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.BusinessTrustRating;
import dev.marketplace.marketplace.model.BusinessUser;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.service.BusinessService;
import dev.marketplace.marketplace.service.UserService;
import dev.marketplace.marketplace.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BusinessQueryResolver {
    
    private final BusinessService businessService;
    private final UserService userService;
    private final TransactionService transactionService;

    @QueryMapping
    public Business business(@Argument UUID id) {
        log.info("Fetching business with ID: {}", id);
        return businessService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + id));
    }
    
    @QueryMapping
    public Business myBusiness() {
        User currentUser = getCurrentUser();
        log.info("Fetching business for user: {}", currentUser.getId());
        return businessService.findOwnedByUser(currentUser)
                .orElseThrow(() -> new IllegalArgumentException("No business found for user"));
    }
    
    @QueryMapping
    public List<Business> myBusinesses() {
        User currentUser = getCurrentUser();
        log.info("Fetching all businesses for user: {}", currentUser.getId());
        return businessService.findByUser(currentUser);
    }
    
    @QueryMapping
    public List<BusinessUser> getBusinessUsers(@Argument UUID businessId) {
        log.info("Fetching users for business: {}", businessId);
        return businessService.getBusinessUsers(businessId);
    }
    
    @QueryMapping
    public BusinessTrustRatingDTO businessTrustRating(@Argument UUID businessId) {
        BusinessTrustRating entity = businessService.getBusinessTrustRating(businessId);
        double averageRating = entity.getOverallScore() != null ? entity.getOverallScore().doubleValue() : 0.0;
        int reviewCount = entity.getTotalReviews();
        return new BusinessTrustRatingDTO(averageRating, reviewCount);
    }

    @QueryMapping
    public Business getBusinessBySlug(@Argument String slug) {
        return businessService.findBySlug(slug).orElse(null);
    }

    @SchemaMapping
    public List<BusinessUser> getBusinessUsers(Business business) {
        return business.getBusinessUsers();
    }
    
    @SchemaMapping
    public User user(BusinessUser businessUser) {
        return businessUser.getUser();
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("User not authenticated");
        }
        
        String email = authentication.getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    @QueryMapping
    public List<dev.marketplace.marketplace.model.Transaction> getBusinessTransactions(@Argument UUID businessId) {
        // Find all listings for the business
        List<dev.marketplace.marketplace.model.Listing> listings = businessService.getListingsForBusiness(businessId);
        if (listings == null || listings.isEmpty()) return List.of();
        // Collect all listing IDs
        List<UUID> listingIds = listings.stream().map(dev.marketplace.marketplace.model.Listing::getId).toList();
        // Fetch all transactions for these listings
        return transactionService.getTransactionsByListingIds(listingIds);
    }

    @SchemaMapping(typeName = "Business", field = "trustRating")
    public BusinessTrustRatingDTO trustRating(Business business) {
        BusinessTrustRating entity = businessService.getBusinessTrustRating(business.getId());
        double averageRating = entity.getOverallScore() != null ? entity.getOverallScore().doubleValue() : 0.0;
        int reviewCount = entity.getTotalReviews();
        return new BusinessTrustRatingDTO(averageRating, reviewCount);
    }

    @SchemaMapping(typeName = "Business", field = "planType")
    public dev.marketplace.marketplace.enums.PlanType resolvePlanType(Business business) {
        // Fetch active subscription for business
        return businessService.getActiveSubscriptionForBusiness(business.getId())
            .map(sub -> {
                // Convert Subscription.PlanType to dev.marketplace.marketplace.enums.PlanType
                try {
                    return dev.marketplace.marketplace.enums.PlanType.valueOf(sub.getPlanType().name());
                } catch (Exception e) {
                    return null;
                }
            })
            .orElse(null);
    }
}

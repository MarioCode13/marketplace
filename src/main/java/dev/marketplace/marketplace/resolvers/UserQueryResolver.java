package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.model.TrustRating;
import dev.marketplace.marketplace.model.VerificationDocument;
import dev.marketplace.marketplace.model.ProfileCompletion;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.dto.ListingDTO;
import dev.marketplace.marketplace.service.UserService;
import dev.marketplace.marketplace.service.TrustRatingService;
import dev.marketplace.marketplace.service.VerificationDocumentService;
import dev.marketplace.marketplace.service.ListingService;
import dev.marketplace.marketplace.repository.ProfileCompletionRepository;
import dev.marketplace.marketplace.repository.StoreBrandingRepository;
import dev.marketplace.marketplace.repository.BusinessRepository;
import dev.marketplace.marketplace.model.Business;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import dev.marketplace.marketplace.model.StoreBranding;

@CrossOrigin(origins = "http://localhost:3000")
@Controller
public class UserQueryResolver {

    private final UserService userService;
    private final TrustRatingService trustRatingService;
    private final VerificationDocumentService verificationDocumentService;
    private final ProfileCompletionRepository profileCompletionRepository;
    private final StoreBrandingRepository storeBrandingRepository;
    private final ListingService listingService;
    private final BusinessRepository businessRepository;

    @SchemaMapping(typeName = "User", field = "profileImageUrl")
    public String resolveProfileImageUrl(User user) {
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            return user.getProfileImageUrl();
        }
        return null;
    }

    @SchemaMapping(typeName = "User", field = "idPhotoUrl")
    public String resolveIdPhotoUrl(User user) {
        if (user.getIdPhotoUrl() != null && !user.getIdPhotoUrl().isEmpty()) {
            return user.getIdPhotoUrl();
        }
        return null;
    }

    @SchemaMapping(typeName = "User", field = "driversLicenseUrl")
    public String resolveDriversLicenseUrl(User user) {
        if (user.getDriversLicenseUrl() != null && !user.getDriversLicenseUrl().isEmpty()) {
            return user.getDriversLicenseUrl();
        }
        return null;
    }

    @SchemaMapping(typeName = "User", field = "proofOfAddressUrl")
    public String resolveProofOfAddressUrl(User user) {
        if (user.getProofOfAddressUrl() != null && !user.getProofOfAddressUrl().isEmpty()) {
            return user.getProofOfAddressUrl();
        }
        return null;
    }

    @SchemaMapping(typeName = "User", field = "trustRating")
    public TrustRating resolveTrustRating(User user) {
        return trustRatingService.getTrustRating(user.getId());
    }

    @SchemaMapping(typeName = "User", field = "verificationDocuments")
    public List<VerificationDocument> resolveVerificationDocuments(User user) {
        return verificationDocumentService.getUserDocuments(user.getId());
    }

    @SchemaMapping(typeName = "User", field = "profileCompletion")
    public ProfileCompletion resolveProfileCompletion(User user) {
        return profileCompletionRepository.findByUserId(user.getId()).orElse(null);
    }

    @SchemaMapping(typeName = "User", field = "planType")
    public String resolvePlanType(User user) {
        return user.getPlanType();
    }

    @SchemaMapping(typeName = "User", field = "storeBranding")
    public dev.marketplace.marketplace.model.StoreBranding resolveStoreBranding(User user) {
        // Try to find business where user is owner
        Optional<Business> ownedBusiness = businessRepository.findOwnedByUser(user);
        Business business = null;
        if (ownedBusiness.isPresent()) {
            business = ownedBusiness.get();
        } else {
            // Optionally, return first business where user is a team member
            List<Business> businesses = businessRepository.findByUser(user);
            if (!businesses.isEmpty()) {
                business = businesses.get(0);
            }
        }
        if (business != null) {
            return storeBrandingRepository.findByBusiness(business).orElse(null);
        }
        return null;
    }

    @SchemaMapping(typeName = "StoreBranding", field = "storeName")
    public String resolveStoreName(StoreBranding branding) {
        return branding.getStoreName();
    }

    @SchemaMapping(typeName = "User", field = "listings")
    public List<ListingDTO> getListings(User user) {
        return listingService.getListingsByUserId(user.getId());
    }

    @SchemaMapping(typeName = "User", field = "business")
    public Business resolveBusiness(User user) {
        // Try to find business where user is owner
        Optional<Business> ownedBusiness = businessRepository.findOwnedByUser(user);
        if (ownedBusiness.isPresent()) {
            return ownedBusiness.get();
        }
        // Optionally, return first business where user is a team member
        List<Business> businesses = businessRepository.findByUser(user);
        return businesses.isEmpty() ? null : businesses.get(0);
    }

    public UserQueryResolver(
        UserService userService,
        TrustRatingService trustRatingService,
        VerificationDocumentService verificationDocumentService,
        ProfileCompletionRepository profileCompletionRepository,
        StoreBrandingRepository storeBrandingRepository,
        ListingService listingService,
        BusinessRepository businessRepository
    ) {
        this.userService = userService;
        this.trustRatingService = trustRatingService;
        this.verificationDocumentService = verificationDocumentService;
        this.profileCompletionRepository = profileCompletionRepository;
        this.storeBrandingRepository = storeBrandingRepository;
        this.listingService = listingService;
        this.businessRepository = businessRepository;
    }

    @QueryMapping
    public User getUserById(@Argument Long id) {
        return userService.getUserById(id);
    }

    @QueryMapping
    public User user(@Argument Long id) {
        return userService.getUserById(id);
    }

    @QueryMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }
    
    @QueryMapping
    public List<User> searchUsers(@Argument String searchTerm) {
        return userService.searchUsers(searchTerm);
    }

    public User getUserByEmail(@Argument String email) {
        Optional<User> userOpt = userService.getUserByEmail(email);
        return userOpt.orElseThrow(() -> new RuntimeException("User not found"));
    }

    @QueryMapping
    public String getProfileImage(@Argument Long userId) {
        return userService.getProfileImageUrl(userId);
    }

    @QueryMapping
    public String getUserProfileImage(@Argument Long userId) {
        return userService.getProfileImageUrl(userId);
    }

    @QueryMapping
    public User storeBySlug(@Argument String slug) {
        return storeBrandingRepository.findBySlug(slug)
                .map(branding -> branding.getBusiness() != null ? branding.getBusiness().getOwner() : null)
                .orElse(null);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public User me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        System.out.println("Authentication: " + authentication);

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();

        System.out.println("Principal: " + principal);

        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userService.getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } else {
            throw new RuntimeException("Invalid authentication details");
        }
    }


}

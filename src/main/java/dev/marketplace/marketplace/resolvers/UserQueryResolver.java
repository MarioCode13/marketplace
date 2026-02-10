package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.model.TrustRating;
import dev.marketplace.marketplace.model.VerificationDocument;
import dev.marketplace.marketplace.model.ProfileCompletion;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.dto.ListingDTO;
import dev.marketplace.marketplace.dto.UserDTO;
import dev.marketplace.marketplace.mapper.UserMapper;
import dev.marketplace.marketplace.service.UserService;
import dev.marketplace.marketplace.service.TrustRatingService;
import dev.marketplace.marketplace.service.VerificationDocumentService;
import dev.marketplace.marketplace.service.ListingService;
import dev.marketplace.marketplace.repository.ProfileCompletionRepository;
import dev.marketplace.marketplace.repository.StoreBrandingRepository;
import dev.marketplace.marketplace.repository.BusinessRepository;
import dev.marketplace.marketplace.repository.CityRepository;
import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.City;
import dev.marketplace.marketplace.model.Subscription;
import dev.marketplace.marketplace.service.SubscriptionService;
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
import java.util.UUID;
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
    private final SubscriptionService subscriptionService;
    private final CityRepository cityRepository;

    @SchemaMapping(typeName = "User", field = "profileImageUrl")
    public String resolveProfileImageUrl(Object userObj) {
        UserDTO user = userObj instanceof UserDTO ? (UserDTO) userObj : UserMapper.toDto((User) userObj);
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            return user.getProfileImageUrl();
        }
        return null;
    }

    /**
     * Field-level authorization: Only allow users to see their own email
     * Returns null for unauthorized access attempts
     */
    @SchemaMapping(typeName = "User", field = "email")
    public String resolveEmail(Object userObj) {
        UserDTO user = userObj instanceof UserDTO ? (UserDTO) userObj : UserMapper.toDto((User) userObj);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // If not authenticated, return null
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        // Check if current user is viewing their own data
        try {
            User currentUser = getCurrentUser();
            if (currentUser != null && currentUser.getId().equals(user.getId())) {
                return user.getEmail();
            }
        } catch (Exception e) {
            // If we can't get current user, return null for security
            return null;
        }
        
        // Not authorized - return null
        return null;
    }

    /**
     * Field-level authorization: Only allow users to see their own ID number
     */
    @SchemaMapping(typeName = "User", field = "idNumber")
    public String resolveIdNumber(Object userObj) {
        UserDTO user = userObj instanceof UserDTO ? (UserDTO) userObj : UserMapper.toDto((User) userObj);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        try {
            User currentUser = getCurrentUser();
            if (currentUser != null && currentUser.getId().equals(user.getId())) {
                return user.getIdNumber();
            }
        } catch (Exception e) {
            return null;
        }
        
        return null;
    }

    /**
     * Field-level authorization: Only allow users to see their own contact number
     */
    @SchemaMapping(typeName = "User", field = "contactNumber")
    public String resolveContactNumber(Object userObj) {
        UserDTO user = userObj instanceof UserDTO ? (UserDTO) userObj : UserMapper.toDto((User) userObj);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        try {
            User currentUser = getCurrentUser();
            if (currentUser != null && currentUser.getId().equals(user.getId())) {
                return user.getContactNumber();
            }
        } catch (Exception e) {
            return null;
        }
        
        return null;
    }

    @SchemaMapping(typeName = "User", field = "trustRating")
    public TrustRating resolveTrustRating(Object userObj) {
        UserDTO user = userObj instanceof UserDTO ? (UserDTO) userObj : UserMapper.toDto((User) userObj);
        return trustRatingService.getTrustRating(user.getId());
    }

    @SchemaMapping(typeName = "User", field = "verificationDocuments")
    public List<VerificationDocument> resolveVerificationDocuments(Object userObj) {
        UserDTO user = userObj instanceof UserDTO ? (UserDTO) userObj : UserMapper.toDto((User) userObj);
        return verificationDocumentService.getUserDocuments(user.getId());
    }

    @SchemaMapping(typeName = "User", field = "profileCompletion")
    public ProfileCompletion resolveProfileCompletion(Object userObj) {
        UserDTO user = userObj instanceof UserDTO ? (UserDTO) userObj : UserMapper.toDto((User) userObj);
        return profileCompletionRepository.findByUserId(user.getId()).orElse(null);
    }

    @SchemaMapping(typeName = "User", field = "planType")
    public String resolvePlanType(Object userObj) {
        UserDTO user = userObj instanceof UserDTO ? (UserDTO) userObj : UserMapper.toDto((User) userObj);
        Optional<Subscription> activeSub = subscriptionService.getActiveSubscription(user.getId());
        return activeSub.map(sub -> sub.getPlanType() != null ? sub.getPlanType().name() : null).orElse(null);
    }

    @SchemaMapping(typeName = "User", field = "storeBranding")
    public dev.marketplace.marketplace.model.StoreBranding resolveStoreBranding(Object userObj) {
        UserDTO user = userObj instanceof UserDTO ? (UserDTO) userObj : UserMapper.toDto((User) userObj);
        // Fetch User entity to use repository methods that expect User
        Optional<User> userEntityOpt = userService.findById(user.getId());
        if (userEntityOpt.isEmpty()) return null;
        User userEntity = userEntityOpt.get();
        // Try to find business where user is owner
        Optional<Business> ownedBusiness = businessRepository.findOwnedByUser(userEntity);
        Business business = null;
        if (ownedBusiness.isPresent()) {
            business = ownedBusiness.get();
        } else {
            // Optionally, return first business where user is a team member
            List<Business> businesses = businessRepository.findByUser(userEntity);
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
    public List<ListingDTO> getListings(Object userObj) {
        UserDTO user = userObj instanceof UserDTO ? (UserDTO) userObj : UserMapper.toDto((User) userObj);
        return listingService.getListingsByUserId(user.getId());
    }

    @SchemaMapping(typeName = "User", field = "business")
    public Business resolveBusiness(Object userObj) {
        UserDTO user = userObj instanceof UserDTO ? (UserDTO) userObj : UserMapper.toDto((User) userObj);
        Optional<User> userEntityOpt = userService.findById(user.getId());
        if (userEntityOpt.isEmpty()) return null;
        User userEntity = userEntityOpt.get();
        // Try to find business where user is owner
        Optional<Business> ownedBusiness = businessRepository.findOwnedByUser(userEntity);
        if (ownedBusiness.isPresent()) {
            return ownedBusiness.get();
        }
        // Optionally, return first business where user is a team member
        List<Business> businesses = businessRepository.findByUser(userEntity);
        return businesses.isEmpty() ? null : businesses.get(0);
    }

    @SchemaMapping(typeName = "User", field = "subscription")
    public Subscription resolveSubscription(Object userObj) {
        UserDTO user = userObj instanceof UserDTO ? (UserDTO) userObj : UserMapper.toDto((User) userObj);
        return subscriptionService.getActiveSubscription(user.getId()).orElse(null);
    }

    /**
     * Field-level authorization: Only allow users to see their own document URLs
     */
    @SchemaMapping(typeName = "User", field = "idPhotoUrl")
    public String resolveIdPhotoUrl(Object userObj) {
        return resolveSensitiveField(userObj, "idPhotoUrl");
    }

    @SchemaMapping(typeName = "User", field = "driversLicenseUrl")
    public String resolveDriversLicenseUrl(Object userObj) {
        return resolveSensitiveField(userObj, "driversLicenseUrl");
    }

    @SchemaMapping(typeName = "User", field = "proofOfAddressUrl")
    public String resolveProofOfAddressUrl(Object userObj) {
        return resolveSensitiveField(userObj, "proofOfAddressUrl");
    }

    /**
     * Helper method to check if current user can access sensitive document fields
     * Returns the field value if authorized, null otherwise
     * Note: Document URLs should ideally be accessed through verificationDocuments field
     */
    private String resolveSensitiveField(Object userObj, String fieldName) {
        UserDTO user = userObj instanceof UserDTO ? (UserDTO) userObj : UserMapper.toDto((User) userObj);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        try {
            User currentUser = getCurrentUser();
            if (currentUser != null && currentUser.getId().equals(user.getId())) {
                // User is viewing their own data - check if document exists in verificationDocuments
                // For now, return null as these should be accessed through verificationDocuments
                // This prevents unauthorized access while maintaining functionality
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        
        return null;
    }

    /**
     * Helper method to get the current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("User not authenticated");
        }

        String email = authentication.getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    public UserQueryResolver(
        UserService userService,
        TrustRatingService trustRatingService,
        VerificationDocumentService verificationDocumentService,
        ProfileCompletionRepository profileCompletionRepository,
        StoreBrandingRepository storeBrandingRepository,
        ListingService listingService,
        BusinessRepository businessRepository,
        SubscriptionService subscriptionService,
        CityRepository cityRepository
    ) {
        this.userService = userService;
        this.trustRatingService = trustRatingService;
        this.verificationDocumentService = verificationDocumentService;
        this.profileCompletionRepository = profileCompletionRepository;
        this.storeBrandingRepository = storeBrandingRepository;
        this.listingService = listingService;
        this.businessRepository = businessRepository;
        this.subscriptionService = subscriptionService;
        this.cityRepository = cityRepository;
    }

    /**
     * Resolves User.city so that clients get the full City (id, name, region) when User is returned as UserDTO.
     */
    @SchemaMapping(typeName = "User", field = "city")
    public City resolveCity(Object userObj) {
        UserDTO user = userObj instanceof UserDTO ? (UserDTO) userObj : UserMapper.toDto((User) userObj);
        if (user.getCityId() == null) return null;
        return cityRepository.findById(user.getCityId()).orElse(null);
    }

    @QueryMapping
    public UserDTO getUserById(@Argument UUID id) {
        return UserMapper.toDto(userService.getUserById(id));
    }

    @QueryMapping
    public UserDTO user(@Argument UUID id) {
        return UserMapper.toDto(userService.getUserById(id));
    }

    @QueryMapping
    public List<UserDTO> getAllUsers() {
        return userService.getAllUsers().stream().map(UserMapper::toDto).toList();
    }
    
    @QueryMapping
    public List<UserDTO> searchUsers(@Argument String searchTerm) {
        return userService.searchUsers(searchTerm).stream().map(UserMapper::toDto).toList();
    }

    public UserDTO getUserByEmail(@Argument String email) {
        Optional<User> userOpt = userService.getUserByEmail(email);
        return userOpt.map(UserMapper::toDto).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @QueryMapping
    public String getProfileImage(@Argument UUID userId) {
        return userService.getUserProfileImageUrl(userId).orElse(null);
    }

    @QueryMapping
    public String getUserProfileImage(@Argument UUID userId) {
        return userService.getUserProfileImageUrl(userId).orElse(null);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public UserDTO me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            // Load user with city so cityId/city is populated in the DTO and resolvers
            return userService.getUserByEmailWithCity(email)
                    .map(UserMapper::toDto)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } else {
            throw new RuntimeException("Invalid authentication details");
        }
    }

    @QueryMapping
    public List<ListingDTO> getUserListings(@Argument UUID id) {
        return listingService.getListingsByUserId(id);
    }


}


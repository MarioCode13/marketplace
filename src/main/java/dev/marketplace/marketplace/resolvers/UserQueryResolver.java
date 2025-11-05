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
import dev.marketplace.marketplace.model.Business;
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

    @SchemaMapping(typeName = "User", field = "profileImageUrl")
    public String resolveProfileImageUrl(Object userObj) {
        UserDTO user = userObj instanceof UserDTO ? (UserDTO) userObj : UserMapper.toDto((User) userObj);
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            return user.getProfileImageUrl();
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

    public UserQueryResolver(
        UserService userService,
        TrustRatingService trustRatingService,
        VerificationDocumentService verificationDocumentService,
        ProfileCompletionRepository profileCompletionRepository,
        StoreBrandingRepository storeBrandingRepository,
        ListingService listingService,
        BusinessRepository businessRepository,
        SubscriptionService subscriptionService
    ) {
        this.userService = userService;
        this.trustRatingService = trustRatingService;
        this.verificationDocumentService = verificationDocumentService;
        this.profileCompletionRepository = profileCompletionRepository;
        this.storeBrandingRepository = storeBrandingRepository;
        this.listingService = listingService;
        this.businessRepository = businessRepository;
        this.subscriptionService = subscriptionService;
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
        return userService.getProfileImageUrl(userId);
    }

    @QueryMapping
    public String getUserProfileImage(@Argument UUID userId) {
        return userService.getProfileImageUrl(userId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public UserDTO me() {
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

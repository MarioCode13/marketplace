package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.model.TrustRating;
import dev.marketplace.marketplace.model.VerificationDocument;
import dev.marketplace.marketplace.model.ProfileCompletion;
import dev.marketplace.marketplace.service.UserService;
import dev.marketplace.marketplace.service.TrustRatingService;
import dev.marketplace.marketplace.service.VerificationDocumentService;
import dev.marketplace.marketplace.repository.ProfileCompletionRepository;
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

@CrossOrigin(origins = "http://localhost:3000")
@Controller
public class UserQueryResolver {

    private final UserService userService;
    private final TrustRatingService trustRatingService;
    private final VerificationDocumentService verificationDocumentService;
    private final ProfileCompletionRepository profileCompletionRepository;

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

    public UserQueryResolver(UserService userService, TrustRatingService trustRatingService, VerificationDocumentService verificationDocumentService, ProfileCompletionRepository profileCompletionRepository) {
        this.userService = userService;
        this.trustRatingService = trustRatingService;
        this.verificationDocumentService = verificationDocumentService;
        this.profileCompletionRepository = profileCompletionRepository;
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

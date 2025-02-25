package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.service.UserService;
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

    @SchemaMapping(typeName = "User", field = "profileImage")
    public String resolveProfileImage(User user) {
        if (user.getProfileImage() != null) {
            return Base64.getEncoder().encodeToString(user.getProfileImage());
        }
        return null; // If no image exists
    }

    public UserQueryResolver(UserService userService) {
        this.userService = userService;
    }

    @QueryMapping
    public User getUserById(@Argument Long id) {
        return userService.getUserById(id);
    }

    @QueryMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    public User getUserByEmail(@Argument String email) {
        Optional<User> userOpt = userService.getUserByEmail(email);
        return userOpt.orElseThrow(() -> new RuntimeException("User not found"));
    }

//    public Optional<byte[]> getProfileImage(Long userId) {
//        return userService.getUserProfileImage(userId);
//    }

//    public String getUserProfileImage(Long userId) {
//        Optional<byte[]> imageData = userService.getUserProfileImage(userId);
//
//        return imageData.map(bytes -> Base64.getEncoder().encodeToString(bytes))
//                .orElse(null);
//    }

    public String getUserProfileImage(Long userId) {
        return userService.getUserProfileImage(userId)
                .map(bytes -> Base64.getEncoder().encodeToString(bytes))
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

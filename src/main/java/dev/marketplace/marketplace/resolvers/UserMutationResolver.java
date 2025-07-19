package dev.marketplace.marketplace.resolvers;

import com.backblaze.b2.client.exceptions.B2Exception;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.UserRepository;
import dev.marketplace.marketplace.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;


@CrossOrigin(origins = "http://localhost:3000")
@Controller
public class UserMutationResolver {

    private final UserService userService;

    public UserMutationResolver(UserService userService) {
        this.userService = userService;
    }

    @MutationMapping
    public User updateUser(
            @Argument Long id,
            @Argument String username,
            @Argument String email,
            @Argument String firstName,
            @Argument String lastName,
            @Argument String bio,
            @Argument Long cityId,
            @Argument String customCity,
            @Argument String contactNumber
    ) {
        return userService.updateUser(id, username, email, firstName, lastName, bio, cityId, customCity, contactNumber);
    }

    @MutationMapping
    public String uploadProfileImage(@Argument String image, @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        String imageUrl = userService.uploadImageAndGetUrl(image);

        userService.updateProfileImage(userId, imageUrl);

        return imageUrl;
    }

    @MutationMapping
    public User updateUserPlanType(@Argument Long id, @Argument String planType) {
        return userService.updateUserPlanType(id, planType);
    }

    @MutationMapping
    public User updateStoreBranding(
            @Argument Long id,
            @Argument String slug,
            @Argument String logoUrl,
            @Argument String bannerUrl,
            @Argument String themeColor,
            @Argument String primaryColor,
            @Argument String secondaryColor,
            @Argument String lightOrDark,
            @Argument String about,
            @Argument String storeName
    ) {
        return userService.updateStoreBranding(id, slug, logoUrl, bannerUrl, themeColor, primaryColor, secondaryColor, lightOrDark, about, storeName);
    }
}
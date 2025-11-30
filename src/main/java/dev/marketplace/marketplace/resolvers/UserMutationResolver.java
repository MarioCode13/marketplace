package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.dto.UserDTO;
import dev.marketplace.marketplace.mapper.UserMapper;
import dev.marketplace.marketplace.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;


@CrossOrigin(origins = "http://localhost:3000")
@Controller
public class UserMutationResolver {

    private final UserService userService;

    public UserMutationResolver(UserService userService) {
        this.userService = userService;
    }

    @MutationMapping
    public UserDTO updateUser(
            @Argument UUID id,
            @Argument String username,
            @Argument String email,
            @Argument String firstName,
            @Argument String lastName,
            @Argument String bio,
            @Argument UUID cityId,
            @Argument String customCity,
            @Argument String contactNumber,
            @Argument String idNumber
    ) {
        User updated = userService.updateUser(id, username, email, firstName, lastName, bio, cityId, customCity, contactNumber, idNumber);
        return UserMapper.toDto(updated);
    }

    @MutationMapping
    public String uploadProfileImage(@Argument String image, @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userService.getUserIdByUsername(userDetails.getUsername());
        String imageUrl = userService.uploadImageAndGetUrl(image);

        userService.updateProfileImage(userId, imageUrl);

        return imageUrl;
    }
}
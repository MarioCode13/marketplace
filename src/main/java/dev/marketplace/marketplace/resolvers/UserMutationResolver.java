package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.UserRepository;
import dev.marketplace.marketplace.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


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
            @Argument String email
    ) {
        return userService.updateUser(id, username, email);
    }
    public String uploadProfileImage(Long userId, MultipartFile image) {
        try {
            byte[] imageData = image.getBytes();
            userService.saveUserProfileImage(userId, imageData);
            return "Profile image uploaded successfully";
        } catch (IOException e) {
            throw new RuntimeException("Error uploading profile image", e);
        }
    }
}
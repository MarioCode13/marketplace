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

    @MutationMapping
    public String uploadProfileImage(
            @Argument Long userId,
            @Argument MultipartFile image
    ) {
        try {
            // Upload the image, get the file path or URL (example using Backblaze or similar)
            String imageUrl = userService.uploadImageAndGetUrl(image);  // Assuming this method handles the upload and gets the URL

            // Save the image URL in the user profile
            userService.saveUserProfileImage(userId, imageUrl);

            // Return the URL or a success message
            return "Profile image uploaded successfully. Image URL: " + imageUrl;
        } catch (IOException e) {
            throw new RuntimeException("Error uploading profile image", e);
        } catch (B2Exception e) {
            throw new RuntimeException(e);
        }
    }
}
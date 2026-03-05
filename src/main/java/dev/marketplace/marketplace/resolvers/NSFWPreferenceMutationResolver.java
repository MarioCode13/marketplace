package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.dto.UserDTO;
import dev.marketplace.marketplace.mapper.UserMapper;
import dev.marketplace.marketplace.repository.UserRepository;
import dev.marketplace.marketplace.service.NSFWContentService;
import dev.marketplace.marketplace.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * GraphQL mutations for user NSFW content preferences and age verification
 */
@CrossOrigin(origins = "http://localhost:3000")
@Controller
public class NSFWPreferenceMutationResolver {

    private final NSFWContentService nsfwContentService;
    private final UserService userService;
    private final UserRepository userRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public NSFWPreferenceMutationResolver(NSFWContentService nsfwContentService,
                                          UserService userService,
                                          UserRepository userRepository) {
        this.nsfwContentService = nsfwContentService;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    /**
     * Verify user's age based on date of birth
     * Must be 18+ to view NSFW content
     */
    @MutationMapping
    @Transactional
    public UserDTO verifyUserAge(
            @Argument String dateOfBirth,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userService.getUserIdByUsername(userDetails.getUsername());
        User user = userService.getUserById(userId);

        // Parse date of birth
        LocalDate dob = LocalDate.parse(dateOfBirth, DATE_FORMATTER);
        user.setDateOfBirth(dob);

        // Verify age
        boolean is18Plus = nsfwContentService.verifyUserAge(user);
        if (!is18Plus) {
            throw new IllegalArgumentException("Must be 18 years old or older to view explicit content");
        }

        // Save updated user
        User updated = userRepository.save(user);
        return UserMapper.toDto(updated);
    }

    /**
     * Update user's preference for viewing explicit content
     * Only available after age verification
     */
    @MutationMapping
    @Transactional
    public UserDTO updateExplicitContentPreference(
            @Argument Boolean allowExplicit,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userService.getUserIdByUsername(userDetails.getUsername());
        User user = userService.getUserById(userId);

        // Check if user is age verified
        if (!user.getAgeVerified()) {
            throw new IllegalArgumentException("Must verify age before setting explicit content preference");
        }

        // Update preference
        user.setAllowsExplicitContent(allowExplicit);
        User updated = userRepository.save(user);

        return UserMapper.toDto(updated);
    }
}



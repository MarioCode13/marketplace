package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.dto.AuthResponseDto;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.security.JwtUtil;
import dev.marketplace.marketplace.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import dev.marketplace.marketplace.exceptions.AuthException;
import dev.marketplace.marketplace.exceptions.UserAlreadyExistsException;
import dev.marketplace.marketplace.exceptions.InvalidCredentialsException;
import dev.marketplace.marketplace.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Controller
public class AuthResolver {
    private static final Logger logger = LoggerFactory.getLogger(AuthResolver.class);
    
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthResolver(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @MutationMapping
    public AuthResponseDto register(@Argument String username, @Argument String email, @Argument String password) {
        logger.info("Registration attempt - Username: {}, Email: {}", username, email);
        try {
            User user = userService.registerUser(username, email, password);
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
            logger.info("Registration successful for user: {}", user.getEmail());
            return new AuthResponseDto(token, user.getEmail(), user.getRole().name(), user.getId());
        } catch (UserAlreadyExistsException | ValidationException e) {
            logger.warn("Registration failed with known exception: {}", e.getMessage());
            throw e; // Re-throw custom exceptions as they have proper error codes
        } catch (Exception e) {
            logger.error("Registration failed with unexpected error: {}", e.getMessage(), e);
            throw new AuthException("Registration failed. Please try again.");
        }
    }

    @MutationMapping
    public AuthResponseDto login(@Argument String emailOrUsername, @Argument String password) {
        logger.info("Login attempt - EmailOrUsername: {}", emailOrUsername);
        try {
            // Validate input
            if (emailOrUsername == null || emailOrUsername.trim().isEmpty()) {
                logger.warn("Login failed: Email or username is empty");
                throw new ValidationException("Email or username is required");
            }
            if (password == null || password.trim().isEmpty()) {
                logger.warn("Login failed: Password is empty");
                throw new ValidationException("Password is required");
            }

            logger.debug("Attempting to authenticate user: {}", emailOrUsername);
            Optional<User> userOpt = userService.authenticateUser(emailOrUsername, password);

            if (userOpt.isEmpty()) {
                logger.warn("Login failed: No user found or invalid password for: {}", emailOrUsername);
                throw new InvalidCredentialsException("Invalid email/username or password");
            }

            User user = userOpt.get();
            logger.info("Login successful for user: {} (ID: {})", user.getEmail(), user.getId());
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());

            return new AuthResponseDto(token, user.getEmail(), user.getRole().name(), user.getId());
        } catch (ValidationException | InvalidCredentialsException e) {
            logger.warn("Login failed with known exception: {}", e.getMessage());
            throw e; // Re-throw custom exceptions as they have proper error codes
        } catch (Exception e) {
            logger.error("Login failed with unexpected error: {}", e.getMessage(), e);
            throw new AuthException("Login failed. Please try again.");
        }
    }


    public record AuthResponse(String token) {}
}

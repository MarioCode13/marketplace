package dev.marketplace.marketplace.service;

import com.backblaze.b2.client.exceptions.B2Exception;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.marketplace.marketplace.enums.Role;
import dev.marketplace.marketplace.enums.PlanType;
import dev.marketplace.marketplace.exceptions.UserAlreadyExistsException;
import dev.marketplace.marketplace.exceptions.ValidationException;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.marketplace.marketplace.model.City;
import dev.marketplace.marketplace.repository.CityRepository;
import dev.marketplace.marketplace.repository.SubscriptionRepository;
import dev.marketplace.marketplace.model.Subscription.SubscriptionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class UserService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final B2StorageService b2StorageService;
    private final CityRepository cityRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordValidationService passwordValidationService;

    @Autowired
    private TrustRatingService trustRatingService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private dev.marketplace.marketplace.config.AppConfig appConfig;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, B2StorageService b2StorageService, CityRepository cityRepository, SubscriptionRepository subscriptionRepository, PasswordValidationService passwordValidationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.b2StorageService = b2StorageService;
        this.cityRepository = cityRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.passwordValidationService = passwordValidationService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<User> userOpt = userRepository.findByEmail(username);

        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(username);
        }
        
        User user = userOpt
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username/email: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRole().name())
                .build();
    }

    public User registerUser(String username, String email, String password) {
        logger.info("Registering new user - Username: {}, Email: {}", username, email);

        // Validate username
        validateUsername(username);

        // Validate email
        validateEmail(email);

        // Validate password strength
        passwordValidationService.validatePasswordStrength(password);

        // Check for existing email
        if (userRepository.findByEmail(email).isPresent()) {
            logger.warn("Registration failed: Email already exists: {}", email);
            throw new UserAlreadyExistsException("An account with this email already exists");
        }

        // Check for existing username
        if (userRepository.findByUsername(username).isPresent()) {
            logger.warn("Registration failed: Username already exists: {}", username);
            throw new UserAlreadyExistsException("This username is already taken");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.HAS_ACCOUNT);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);
        logger.info("User registered successfully - ID: {}, Email: {}", savedUser.getId(), savedUser.getEmail());
        trustRatingService.calculateAndUpdateTrustRating(savedUser.getId());

        // Generate email verification token
        String verificationToken = tokenService.generateEmailVerificationToken();
        savedUser.setEmailVerificationToken(verificationToken);
        savedUser.setEmailVerificationTokenExpiry(java.time.LocalDateTime.now().plusHours(24)); // Expires in 24 hours
        userRepository.save(savedUser);

        // Send verification email (wrapped in try-catch to prevent registration failure)
        String baseUrl = appConfig.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://localhost:8080";
        }
        String verificationUrl = baseUrl + "/api/auth/verify-email?token=" + verificationToken;
        try {
            notificationService.sendEmailVerificationNotification(savedUser, verificationUrl);
            logger.info("Verification email sent successfully to: {}", savedUser.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}, but user was registered successfully. They can request resend.", savedUser.getEmail(), e);
        }

        return savedUser;
    }

    /**
     * Validates username format and length
     * @param username the username to validate
     * @throws ValidationException if username is invalid
     */
    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Username validation failed: Username is empty");
            throw new ValidationException("Username is required");
        }

        String trimmedUsername = username.trim();

        if (trimmedUsername.length() < 3) {
            logger.warn("Username validation failed: Username too short: {}", username);
            throw new ValidationException("Username must be at least 3 characters long");
        }

        if (trimmedUsername.length() > 50) {
            logger.warn("Username validation failed: Username too long: {}", username);
            throw new ValidationException("Username must not exceed 50 characters");
        }

        // Username can only contain alphanumeric characters, underscores, and hyphens
        if (!trimmedUsername.matches("^[a-zA-Z0-9_-]+$")) {
            logger.warn("Username validation failed: Invalid characters in username: {}", username);
            throw new ValidationException("Username can only contain letters, numbers, underscores, and hyphens");
        }
    }

    /**
     * Validates email format
     * @param email the email to validate
     * @throws ValidationException if email is invalid
     */
    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            logger.warn("Email validation failed: Email is empty");
            throw new ValidationException("Email is required");
        }

        String trimmedEmail = email.trim();

        // Basic email validation regex
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (!trimmedEmail.matches(emailRegex)) {
            logger.warn("Email validation failed: Invalid email format: {}", email);
            throw new ValidationException("Please enter a valid email address");
        }

        if (trimmedEmail.length() > 100) {
            logger.warn("Email validation failed: Email too long: {}", email);
            throw new ValidationException("Email address is too long");
        }
    }

    public User updateUser(UUID userId, String username, String email, String firstName, String lastName, String bio, UUID cityId, String customCity, String contactNumber, String idNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (username != null && !username.isBlank()) {
            if (userRepository.existsByUsernameAndIdNot(username, userId)) {
                throw new IllegalArgumentException("Username already taken");
            }
            user.setUsername(username);
        }

        if (email != null && !email.isBlank()) {
            if (userRepository.existsByEmailAndIdNot(email, userId)) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(email);
        }

        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }
        if (bio != null) {
            user.setBio(bio);
        }
        if (cityId != null) {
            City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new IllegalArgumentException("City not found with ID: " + cityId));
            user.setCity(city);
        }
        if (customCity != null) {
            user.setCustomCity(customCity);
        }
        if (contactNumber != null) {
            user.setContactNumber(contactNumber);
        }
        if (idNumber != null) {
            user.setIdNumber(idNumber);
        }

        User saved = userRepository.save(user);
        // Recalculate profile completion and trust rating after profile fields are changed
        try {
            trustRatingService.updateProfileCompletion(saved.getId());
        } catch (Exception e) {
            logger.warn("Failed to update profile completion/trust rating for user {}: {}", saved.getId(), e.getMessage());
        }
        return saved;
    }

    public UUID getUserIdByUsername(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(username);
        }
        return userOpt
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username/email: " + username));
    }


    @Transactional
    public User upgradeUserRole(UUID userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (newRole == Role.SUBSCRIBED && user.getRole() == Role.HAS_ACCOUNT) {
            user.setRole(Role.SUBSCRIBED);
        } else {
            throw new IllegalArgumentException("Invalid role transition");
        }

        return userRepository.save(user);
    }


    public Optional<User> authenticateUser(String emailOrUsername, String password) {
        logger.debug("Authenticating user: {}", emailOrUsername);
        
        Optional<User> userOpt = userRepository.findByEmail(emailOrUsername);
        logger.debug("User found by email: {}", userOpt.isPresent());

        if (userOpt.isEmpty()) {
            logger.debug("User not found by email, trying username: {}", emailOrUsername);
            userOpt = userRepository.findByUsername(emailOrUsername);
            logger.debug("User found by username: {}", userOpt.isPresent());
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            logger.debug("Found user: {} (ID: {}) with stored password hash: {}", user.getEmail(), user.getId(), user.getPassword());

            // Check if email is verified
            if (user.getEmailVerified() == null || !user.getEmailVerified()) {
                logger.warn("Authentication failed: Email not verified for user: {}", user.getEmail());
                throw new ValidationException("Please verify your email address before logging in. Check your inbox for the verification link.");
            }

            logger.debug("Attempting to match password: '{}' with stored hash", password);
            
            boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
            logger.debug("Password match result for user {}: {}", user.getEmail(), passwordMatches);
            
            if (passwordMatches) {
                logger.info("Authentication successful for user: {} (ID: {})", user.getEmail(), user.getId());
                return userOpt;
            } else {
                logger.warn("Authentication failed: Invalid password for user: {}", user.getEmail());
                logger.debug("Password '{}' does not match hash: {}", password, user.getPassword());
            }
        } else {
            logger.warn("Authentication failed: No user found with email/username: {}", emailOrUsername);
        }

        return Optional.empty();
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Returns user with city relation loaded (for me query and profile).
     */
    public Optional<User> getUserByEmailWithCity(String email) {
        return userRepository.findByEmailWithCity(email);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public List<User> searchUsers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String term = searchTerm.trim().toLowerCase();
        return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term);
    }

    public User getUserById(UUID id) {
        return userRepository.findByIdWithCity(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public Optional<String> getUserProfileImageUrl(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getProfileImageUrl);
    }




    @Transactional
    public User saveUserProfileImage(UUID userId, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setProfileImageUrl(imageUrl);

        User saved = userRepository.save(user);
        // Recalculate profile completion/trust rating after profile image change
        try {
            trustRatingService.updateProfileCompletion(saved.getId());
        } catch (Exception e) {
            logger.warn("Failed to update profile completion/trust rating for user {}: {}", saved.getId(), e.getMessage());
        }
        return saved;
    }

    @Transactional
    public User updateProfileImage(UUID userId, String imageUrl) {
        return saveUserProfileImage(userId, imageUrl);
    }

    @Transactional
    public User uploadProfileImage(UUID userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            String fileName = "profiles/" + userId + "/profile.jpg";
            String uploadedFileName = b2StorageService.uploadImage(fileName, file.getBytes());
            String url = b2StorageService.generatePreSignedUrl(uploadedFileName);
            user.setProfileImageUrl(url);
            User saved = userRepository.save(user);
            // Recalculate profile completion/trust rating after profile image change
            try {
                trustRatingService.updateProfileCompletion(saved.getId());
            } catch (Exception e) {
                logger.warn("Failed to update profile completion/trust rating for user {}: {}", saved.getId(), e.getMessage());
            }
            return saved;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload profile image", e);
        }
    }

    // New helper: return nullable profile image URL (used by GraphQL resolvers)
    public String getProfileImageUrl(UUID userId) {
        return getUserProfileImageUrl(userId).orElse(null);
    }

    // New helper: accept base64 image data, upload to B2 and return a pre-signed URL
    public String uploadImageAndGetUrl(String base64Image) {
        if (base64Image == null || base64Image.isBlank()) {
            throw new IllegalArgumentException("Image data is required");
        }

        // Remove data URL prefix if present
        String payload = base64Image;
        int comma = payload.indexOf(',');
        if (comma >= 0) {
            payload = payload.substring(comma + 1);
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid base64 image data", e);
        }

        try {
            String fileName = "profiles/" + UUID.randomUUID() + ".jpg";
            String uploadedFileName = b2StorageService.uploadImage(fileName, bytes);
            String url = b2StorageService.generatePreSignedUrl(uploadedFileName);
            return url;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload image to storage", e);
        }
    }

    public boolean hasActiveSubscription(UUID userId) {
        return subscriptionRepository.existsByUserIdAndStatusIn(userId, java.util.Arrays.asList(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIAL));
    }


    public java.util.Optional<dev.marketplace.marketplace.model.User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public java.util.Optional<dev.marketplace.marketplace.model.User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public java.util.Optional<dev.marketplace.marketplace.model.User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User updateUserPlanType(UUID userId, PlanType planType) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        // No planType field anymore, so just return user or handle as needed
        return userRepository.save(user);
    }

    /**
     * Verify email using the verification token
     * @param token The email verification token
     * @return True if email was verified successfully, false if token is invalid or expired
     */
    @Transactional
    public boolean verifyEmailToken(String token) {
        if (token == null || token.isBlank()) {
            logger.warn("Email verification failed: Token is empty");
            return false;
        }

        Optional<User> userOpt = userRepository.findAll().stream()
            .filter(u -> token.equals(u.getEmailVerificationToken()))
            .findFirst();

        if (userOpt.isEmpty()) {
            logger.warn("Email verification failed: No user found with this token");
            return false;
        }

        User user = userOpt.get();

        // Check if token has expired
        if (user.getEmailVerificationTokenExpiry() == null || java.time.LocalDateTime.now().isAfter(user.getEmailVerificationTokenExpiry())) {
            logger.warn("Email verification failed: Token expired for user {}", user.getEmail());
            return false;
        }

        // Mark email as verified and clear the token
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);
        userRepository.save(user);

        logger.info("Email verified successfully for user: {}", user.getEmail());
        return true;
    }

    /**
     * Resend verification email to user
     * @param email The user's email address
     * @return True if email was sent successfully, false if user not found or already verified
     */
    @Transactional
    public boolean resendVerificationEmail(String email) {
        if (email == null || email.isBlank()) {
            logger.warn("Resend verification email failed: Email is empty");
            return false;
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            logger.warn("Resend verification email failed: No user found with email: {}", email);
            return false;
        }

        User user = userOpt.get();

        // Don't resend if already verified
        if (user.getEmailVerified() != null && user.getEmailVerified()) {
            logger.warn("Resend verification email failed: Email already verified for user: {}", email);
            return false;
        }

        // Generate new verification token
        String verificationToken = tokenService.generateEmailVerificationToken();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationTokenExpiry(java.time.LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        // Send verification email
        String baseUrl = appConfig.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://localhost:8080";
        }
        String verificationUrl = baseUrl + "/api/auth/verify-email?token=" + verificationToken;
        try {
            notificationService.sendEmailVerificationNotification(user, verificationUrl);
            logger.info("Resent verification email for user: {}", email);
            return true;
        } catch (Exception e) {
            logger.error("Failed to resend verification email for user: {}", email, e);
            return false;
        }
    }
}

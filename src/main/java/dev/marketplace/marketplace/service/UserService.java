package dev.marketplace.marketplace.service;

import com.backblaze.b2.client.exceptions.B2Exception;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.UserRepository;
import dev.marketplace.marketplace.security.JwtUtil;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.marketplace.marketplace.enums.Role;
import dev.marketplace.marketplace.exceptions.AuthException;
import dev.marketplace.marketplace.exceptions.UserAlreadyExistsException;
import dev.marketplace.marketplace.exceptions.InvalidCredentialsException;
import dev.marketplace.marketplace.exceptions.ValidationException;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Base64;

@Service
public class UserService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final B2StorageService b2StorageService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, B2StorageService b2StorageService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.b2StorageService = b2StorageService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // First try to find by email
        Optional<User> userOpt = userRepository.findByEmail(username);
        
        // If not found, try to find by username
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
        
        // Validate input
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Registration failed: Username is empty");
            throw new ValidationException("Username is required");
        }
        if (email == null || email.trim().isEmpty()) {
            logger.warn("Registration failed: Email is empty");
            throw new ValidationException("Email is required");
        }
        if (password == null || password.trim().isEmpty()) {
            logger.warn("Registration failed: Password is empty");
            throw new ValidationException("Password is required");
        }
        if (password.length() < 3) {
            logger.warn("Registration failed: Password too short for user: {}", username);
            throw new ValidationException("Password must be at least 3 characters long");
        }
        if (username.length() < 3) {
            logger.warn("Registration failed: Username too short: {}", username);
            throw new ValidationException("Username must be at least 3 characters long");
        }
        if (!email.contains("@")) {
            logger.warn("Registration failed: Invalid email format: {}", email);
            throw new ValidationException("Please enter a valid email address");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            logger.warn("Registration failed: Email already exists: {}", email);
            throw new UserAlreadyExistsException("An account with this email already exists");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            logger.warn("Registration failed: Username already exists: {}", username);
            throw new UserAlreadyExistsException("This username is already taken");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.HAS_ACCOUNT);

        User savedUser = userRepository.save(user);
        logger.info("User registered successfully - ID: {}, Email: {}", savedUser.getId(), savedUser.getEmail());
        return savedUser;
    }

    public User updateUser(Long userId, String username, String email, String firstName, String lastName, String bio, String location) {
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
        if (location != null) {
            user.setLocation(location);
        }

        return userRepository.save(user);
    }

    public Long getUserIdByUsername(String username) {
        // First try to find by username
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        // If not found by username, try by email
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(username);
        }
        
        return userOpt
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username/email: " + username));
    }


    @Transactional
    public User upgradeUserRole(Long userId, Role newRole) {
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

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public Optional<String> getUserProfileImageUrl(Long userId) {
        return userRepository.findById(userId)
                .map(User::getProfileImageUrl);
    }




    @Transactional
    public User saveUserProfileImage(Long userId, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setProfileImageUrl(imageUrl);

        return userRepository.save(user);
    }

    @Transactional
    public User updateProfileImage(Long userId, String imageUrl) {
        return saveUserProfileImage(userId, imageUrl);
    }

    public String getProfileImageUrl(Long userId) {
        return userRepository.findById(userId)
                .map(User::getProfileImageUrl)
                .orElse(null);
    }

    public String uploadImageAndGetUrl(String base64Image) {
        String fileName = "profiles/" + UUID.randomUUID() + ".jpg";
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        try {
            String uploadedFileName = b2StorageService.uploadImage(fileName, imageBytes);
            return b2StorageService.generatePreSignedUrl(uploadedFileName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    @Transactional
    public User uploadIdPhoto(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            String fileName = "documents/id_photos/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            String uploadedFileName = b2StorageService.uploadImage(fileName, file.getBytes());
            String url = b2StorageService.generatePreSignedUrl(uploadedFileName);
            user.setIdPhotoUrl(url);
            return userRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload ID photo", e);
        }
    }

    @Transactional
    public User uploadDriversLicense(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            String fileName = "documents/drivers_licenses/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            String uploadedFileName = b2StorageService.uploadImage(fileName, file.getBytes());
            String url = b2StorageService.generatePreSignedUrl(uploadedFileName);
            user.setDriversLicenseUrl(url);
            return userRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload driver's license", e);
        }
    }

    @Transactional
    public User uploadProofOfAddress(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            String fileName = "documents/proof_of_address/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            String uploadedFileName = b2StorageService.uploadImage(fileName, file.getBytes());
            String url = b2StorageService.generatePreSignedUrl(uploadedFileName);
            user.setProofOfAddressUrl(url);
            return userRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload proof of address", e);
        }
    }

    @Transactional
    public User uploadProfileImage(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            String fileName = "profiles/" + userId + "/profile.jpg";
            String uploadedFileName = b2StorageService.uploadImage(fileName, file.getBytes());
            String url = b2StorageService.generatePreSignedUrl(uploadedFileName);
            user.setProfileImageUrl(url);
            return userRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload profile image", e);
        }
    }
}

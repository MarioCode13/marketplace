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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Base64;

@Service
public class UserService implements UserDetailsService {
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
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRole().name())
                .build();
    }

    public User registerUser(String username, String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.HAS_ACCOUNT);

        return userRepository.save(user);
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
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
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
        Optional<User> userOpt = userRepository.findByEmail(emailOrUsername);

        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(emailOrUsername);
        }

        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return userOpt;
        }

        return Optional.empty();
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
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

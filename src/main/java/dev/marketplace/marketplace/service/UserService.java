package dev.marketplace.marketplace.service;

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

import javax.sql.rowset.serial.SerialBlob;
import java.sql.Blob;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // Method required by Spring Security for authentication
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), // Username field (email in this case)
                user.getPassword(), // Hashed password
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")) // Default role
        );
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
        user.setRole(User.Role.HAS_ACCOUNT);

        return userRepository.save(user);
    }

    public User updateUser(Long userId, String username, String email) {
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

        return userRepository.save(user);
    }

    @Transactional
    public User upgradeUserRole(Long userId, User.Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (newRole == User.Role.SUBSCRIBED && user.getRole() == User.Role.HAS_ACCOUNT) {
            user.setRole(User.Role.SUBSCRIBED);
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

//    public String getUserProfileImage(Long userId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        return user.getProfileImage() != null
//                ? Base64.getEncoder().encodeToString(user.getProfileImage())  // Convert to Base64
//                : null;
//    }
//public String getUserProfileImage(Long userId) {
//    User user = userRepository.findById(userId)
//            .orElseThrow(() -> new RuntimeException("User not found"));
//
//    return user.getProfileImage() != null
//            ? Base64.getEncoder().encodeToString(user.getProfileImage())
//            : null;
//}
public Optional<byte[]> getUserProfileImage(Long userId) {
    return userRepository.findById(userId)
            .map(User::getProfileImage);
}



    @Transactional
    public void saveUserProfileImage(Long userId, byte[] imageData) {
        System.out.println("Saving image of size: " + imageData.length);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        System.out.println("Before saving, image data type: " + imageData.getClass().getName()); // Debugging line
        try {
            Blob imageBlob = new SerialBlob(imageData); // Convert to Blob
            user.setProfileImage(imageBlob.getBytes(1, (int) imageBlob.length())); // Ensure byte[] format
            userRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save profile image", e);
        }
    }
}

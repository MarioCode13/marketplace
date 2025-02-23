package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.UserRepository;
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

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        return userRepository.save(user);
    }

    public User updateUser(Long userId, String username, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (username != null && !username.isBlank()) {
            if (userRepository.findByUsername(username).isPresent()) {
                throw new IllegalArgumentException("Username already taken");
            }
            user.setUsername(username);
        }

        if (email != null && !email.isBlank()) {
            if (userRepository.findByEmail(email).isPresent()) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(email);
        }

        return userRepository.save(user);
    }

    public Optional<User> authenticateUser(String emailOrUsername, String password) {
        Optional<User> userOpt = userRepository.findByEmail(emailOrUsername);

        if (userOpt.isEmpty()) {  // Java 11+ method, might not work for Java 8
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

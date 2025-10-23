package dev.marketplace.marketplace.unit;

import dev.marketplace.marketplace.exceptions.ValidationException;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.UserRepository;
import dev.marketplace.marketplace.repository.CityRepository;
import dev.marketplace.marketplace.repository.SubscriptionRepository;
import dev.marketplace.marketplace.service.B2StorageService;
import dev.marketplace.marketplace.service.TrustRatingService;
import dev.marketplace.marketplace.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private B2StorageService b2StorageService;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private TrustRatingService trustRatingService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void init() {
        // ensure the autowired field is populated (some Mockito setups may not inject into private fields reliably)
        ReflectionTestUtils.setField(userService, "trustRatingService", trustRatingService);
    }

    @Test
    void registerUser_success() {
        String username = "testuser";
        String email = "test@example.com";
        String rawPassword = "password";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(rawPassword)).thenReturn("encoded-password");

        User saved = new User();
        UUID id = UUID.randomUUID();
        saved.setId(id);
        saved.setUsername(username);
        saved.setEmail(email);
        saved.setPassword("encoded-password");

        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.registerUser(username, email, rawPassword);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals(username, result.getUsername());
        verify(trustRatingService, times(1)).calculateAndUpdateTrustRating(id);
    }

    @Test
    void registerUser_invalidEmail_throwsValidation() {
        assertThrows(ValidationException.class, () -> userService.registerUser("u", "invalid-email", "pwd"));
    }
}

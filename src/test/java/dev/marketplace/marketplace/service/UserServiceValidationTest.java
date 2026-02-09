package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.exceptions.ValidationException;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.UserRepository;
import dev.marketplace.marketplace.repository.CityRepository;
import dev.marketplace.marketplace.repository.SubscriptionRepository;
import dev.marketplace.marketplace.enums.Role;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
public class UserServiceValidationTest {

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
    private PasswordValidationService passwordValidationService;

    @Mock
    private TrustRatingService trustRatingService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(userService, "trustRatingService", trustRatingService);
        doNothing().when(trustRatingService).calculateAndUpdateTrustRating(any(UUID.class));
    }

    private User createMockUser(String username, String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encoded_password");
        user.setRole(Role.HAS_ACCOUNT);
        return user;
    }

    // ===== USERNAME VALIDATION TESTS =====

    @Test
    public void testRegisterWithValidUsername() {
        String username = "validUser123";
        String email = "test@example.com";
        String password = "ValidPass123!";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(createMockUser(username, email));

        // Should not throw any validation exception for username
        User result = userService.registerUser(username, email, password);
        assertNotNull(result);
        assertEquals(username, result.getUsername());
    }

    @Test
    public void testRegisterWithNullUsername() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.registerUser(null, "test@example.com", "ValidPass123!")
        );
        assertTrue(exception.getMessage().contains("Username is required"));
    }

    @Test
    public void testRegisterWithEmptyUsername() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.registerUser("", "test@example.com", "ValidPass123!")
        );
        assertTrue(exception.getMessage().contains("Username is required"));
    }

    @Test
    public void testRegisterWithWhitespaceOnlyUsername() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.registerUser("   ", "test@example.com", "ValidPass123!")
        );
        assertTrue(exception.getMessage().contains("Username is required"));
    }

    @Test
    public void testRegisterWithTooShortUsername() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.registerUser("ab", "test@example.com", "ValidPass123!")
        );
        assertTrue(exception.getMessage().contains("at least 3 characters"));
    }

    @Test
    public void testRegisterWithTooLongUsername() {
        String longUsername = "a".repeat(51);
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.registerUser(longUsername, "test@example.com", "ValidPass123!")
        );
        assertTrue(exception.getMessage().contains("not exceed 50 characters"));
    }

    @Test
    public void testRegisterWithInvalidCharactersInUsername() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.registerUser("user@name", "test@example.com", "ValidPass123!")
        );
        assertTrue(exception.getMessage().contains("letters, numbers, underscores, and hyphens"));
    }

    @Test
    public void testRegisterWithSpecialCharactersInUsername() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.registerUser("user!name", "test@example.com", "ValidPass123!")
        );
        assertTrue(exception.getMessage().contains("letters, numbers, underscores, and hyphens"));
    }

    @Test
    public void testRegisterWithUsernameContainingUnderscores() {
        String username = "valid_user_123";
        String email = "test@example.com";
        String password = "ValidPass123!";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(createMockUser(username, email));

        // Should not throw exception for valid username with underscores
        User result = userService.registerUser(username, email, password);
        assertNotNull(result);
    }

    @Test
    public void testRegisterWithUsernameContainingHyphens() {
        String username = "valid-user-123";
        String email = "test@example.com";
        String password = "ValidPass123!";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(createMockUser(username, email));

        // Should not throw exception for valid username with hyphens
        User result = userService.registerUser(username, email, password);
        assertNotNull(result);
    }

    // ===== EMAIL VALIDATION TESTS =====

    @Test
    public void testRegisterWithValidEmail() {
        String username = "validUser";
        String email = "valid.email@example.com";
        String password = "ValidPass123!";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(createMockUser(username, email));

        // Should not throw any validation exception for email
        User result = userService.registerUser(username, email, password);
        assertNotNull(result);
    }

    @Test
    public void testRegisterWithNullEmail() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.registerUser("validUser", null, "ValidPass123!")
        );
        assertTrue(exception.getMessage().contains("Email is required"));
    }

    @Test
    public void testRegisterWithEmptyEmail() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.registerUser("validUser", "", "ValidPass123!")
        );
        assertTrue(exception.getMessage().contains("Email is required"));
    }

    @Test
    public void testRegisterWithWhitespaceOnlyEmail() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.registerUser("validUser", "   ", "ValidPass123!")
        );
        assertTrue(exception.getMessage().contains("Email is required"));
    }

    @Test
    public void testRegisterWithInvalidEmailFormat() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.registerUser("validUser", "invalid-email", "ValidPass123!")
        );
        assertTrue(exception.getMessage().contains("valid email"));
    }

    @Test
    public void testRegisterWithEmailMissingDomain() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.registerUser("validUser", "user@", "ValidPass123!")
        );
        assertTrue(exception.getMessage().contains("valid email"));
    }

    @Test
    public void testRegisterWithEmailMissingLocalPart() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.registerUser("validUser", "@example.com", "ValidPass123!")
        );
        assertTrue(exception.getMessage().contains("valid email"));
    }

    @Test
    public void testRegisterWithTooLongEmail() {
        String longEmail = "a".repeat(90) + "@example.com";
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> userService.registerUser("validUser", longEmail, "ValidPass123!")
        );
        assertTrue(exception.getMessage().contains("too long"));
    }

    @Test
    public void testRegisterWithEmailContainingPlus() {
        String username = "validUser";
        String email = "user+tag@example.com";
        String password = "ValidPass123!";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(createMockUser(username, email));

        // Should accept emails with plus signs (commonly used)
        User result = userService.registerUser(username, email, password);
        assertNotNull(result);
    }

    @Test
    public void testRegisterWithEmailContainingDot() {
        String username = "validUser";
        String email = "user.name@example.com";
        String password = "ValidPass123!";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(createMockUser(username, email));

        // Should accept emails with dots
        User result = userService.registerUser(username, email, password);
        assertNotNull(result);
    }
}


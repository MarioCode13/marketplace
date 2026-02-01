package dev.marketplace.marketplace.unit;

import dev.marketplace.marketplace.controllers.PayFastController;
import dev.marketplace.marketplace.model.Subscription;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.service.SubscriptionService;
import dev.marketplace.marketplace.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PayFastControllerUnitTest {

    private PayFastController controller;
    private UserService mockUserService;
    private SubscriptionService mockSubscriptionService;

    @BeforeEach
    void setup() {
        controller = new PayFastController();
        mockUserService = mock(UserService.class);
        mockSubscriptionService = mock(SubscriptionService.class);

        // Inject mocks into controller
        ReflectionTestUtils.setField(controller, "userService", mockUserService);
        ReflectionTestUtils.setField(controller, "subscriptionService", mockSubscriptionService);

        // Set payfast config values used in url/signature generation
        ReflectionTestUtils.setField(controller, "merchantId", "10000100");
        ReflectionTestUtils.setField(controller, "merchantKey", "46f0cd694581a");
        ReflectionTestUtils.setField(controller, "payfastUrl", "http://localhost:9090/payfast");
    }

    @Test
    void generateSignature_producesExpectedMd5() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("merchant_id", "10000100");
        params.put("merchant_key", "46f0cd694581a");
        params.put("amount", "100.00");
        params.put("item_name", "Pro Store Subscription");

        // Use reflection to call private method
        Method m = PayFastController.class.getDeclaredMethod("generateSignature", Map.class);
        m.setAccessible(true);
        String sig = (String) m.invoke(controller, params);

        assertNotNull(sig);
        assertFalse(sig.isEmpty());
        // MD5 hex length = 32
        assertEquals(32, sig.length());
    }

    @Test
    void handlePayFastITN_whenComplete_callsSubscriptionService() throws Exception {
        // Create a test user
        User u = new User();
        UUID userId = UUID.randomUUID();
        u.setId(userId);
        u.setEmail("test@example.com");

        when(mockUserService.getUserByEmail("test@example.com")).thenReturn(Optional.of(u));

        Map<String, String> payload = new HashMap<>();
        payload.put("custom_str2", "test@example.com");
        payload.put("payment_status", "COMPLETE");
        payload.put("custom_str1", "pro_store");

        // Call the controller method
        controller.handlePayFastITN(payload);

        // Verify subscriptionService called with correct userId and planType
        ArgumentCaptor<Subscription.PlanType> planCaptor = ArgumentCaptor.forClass(Subscription.PlanType.class);
        verify(mockSubscriptionService, times(1)).createOrActivatePayFastSubscription(eq(userId), planCaptor.capture());
        assertEquals(Subscription.PlanType.PRO_STORE, planCaptor.getValue());
    }
}


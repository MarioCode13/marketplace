package dev.marketplace.marketplace.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.marketplace.marketplace.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.service.SubscriptionService;
import dev.marketplace.marketplace.model.Subscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/payments/payfast")
public class PayFastController {
    private static final Logger log = LoggerFactory.getLogger(PayFastController.class);
    private static final String MERCHANT_ID = "10040629";
    private static final String MERCHANT_KEY = "s0cc5tl93gmzu";
    private static final String PAYFAST_URL = "https://sandbox.payfast.co.za/eng/process";
    private static final String RETURN_URL = "http://localhost:3000/subscriptions/success";
    private static final String CANCEL_URL = "http://localhost:3000/subscriptions/cancel";
//    private static final String NOTIFY_URL = "http://localhost:8080/api/payments/payfast/itn";
    private static final String NOTIFY_URL = "https://03f0de192067.ngrok-free.app/api/payments/payfast/itn";
    private static final String PASSPHRASE = ""; // Set if you have one in PayFast dashboard

    @Autowired
    private UserService userService;

    @Autowired
    private SubscriptionService subscriptionService;

    @GetMapping("/subscription-url")
    public ResponseEntity<String> getPayFastSubscriptionUrl(
            @RequestParam(defaultValue = "Pro Store Subscription") String itemName,
            @RequestParam(defaultValue = "100.00") String amount,
            @RequestParam(defaultValue = "100.00") String recurringAmount,
            @RequestParam(defaultValue = "3") String frequency, // 3 = monthly
            @RequestParam(defaultValue = "0") String cycles, // 0 = indefinite
            @RequestParam(defaultValue = "pro_store") String planType, // plan type
            @RequestParam String userEmail // new required param
    ) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("merchant_id", MERCHANT_ID);
        params.put("merchant_key", MERCHANT_KEY);
        params.put("return_url", RETURN_URL);
        params.put("cancel_url", CANCEL_URL);
        params.put("notify_url", NOTIFY_URL);
        params.put("amount", amount);
        params.put("item_name", itemName);
        params.put("subscription_type", "1");
        params.put("recurring_amount", recurringAmount);
        params.put("frequency", frequency);
        params.put("cycles", cycles);
        params.put("custom_str1", planType); // pass plan type
        params.put("custom_str2", userEmail); // pass user email

        String signature = generateSignature(params);

        StringBuilder url = new StringBuilder(PAYFAST_URL + "?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
               .append("=")
               .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
               .append("&");
        }
        url.append("signature=").append(signature);
        return ResponseEntity.ok(url.toString());
    }

    @GetMapping("/itn")
    public ResponseEntity<String> debugPayFastITN() {
        log.info("[PayFast ITN] GET /itn endpoint hit (debug)");
        return ResponseEntity.ok("PayFast ITN endpoint is reachable (GET)");
    }

    @PostMapping("/itn")
    public ResponseEntity<String> handlePayFastITN(@RequestParam Map<String, String> payload) {
        log.info("[PayFast ITN] Received ITN: {}", payload);
        String email = payload.get("custom_str2");
        String paymentStatus = payload.get("payment_status");
        String planTypeStr = payload.get("custom_str1");
        log.info("[PayFast ITN] Parsed values: email={}, paymentStatus={}, planTypeStr={}", email, paymentStatus, planTypeStr);
        if (email != null && !email.isEmpty() && "COMPLETE".equalsIgnoreCase(paymentStatus) && planTypeStr != null) {
            Optional<User> userOpt = userService.getUserByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                log.info("[PayFast ITN] Found user: id={}, email={}", user.getId(), user.getEmail());
                try {
                    Subscription.PlanType planType = Subscription.PlanType.valueOf(planTypeStr.toUpperCase());
                    log.info("[PayFast ITN] Parsed planType enum: {}", planType);
                    subscriptionService.createOrActivatePayFastSubscription(user.getId(), planType);
                    log.info("[PayFast ITN] Subscription activated for user {} with plan {}", email, planType);
                } catch (IllegalArgumentException e) {
                    log.error("[PayFast ITN] Invalid plan type: {}", planTypeStr, e);
                } catch (Exception e) {
                    log.error("[PayFast ITN] Exception during subscription activation for user {}: {}", email, e.getMessage(), e);
                }
            } else {
                log.error("[PayFast ITN] No user found for email: {}", email);
            }
        } else {
            log.error("[PayFast ITN] Missing email, payment not complete, or plan type. email={}, status={}, planTypeStr={}", email, paymentStatus, planTypeStr);
        }
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/user/subscription-status")
    public ResponseEntity<Map<String, Boolean>> getSubscriptionStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String usernameOrEmail = authentication.getName();
        UUID userId = userService.getUserIdByUsername(usernameOrEmail);
        boolean active = userService.hasActiveSubscription(userId);
        Map<String, Boolean> response = new java.util.HashMap<>();
        response.put("active", active);
        return ResponseEntity.ok(response);
    }

    private String generateSignature(Map<String, String> params) {
        // 1. Exclude empty values and the 'signature' field
        Map<String, String> filtered = params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty() && !"signature".equals(e.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));

        // 2. Build the string
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            sb.append(entry.getKey()).append("=")
              .append(java.net.URLEncoder.encode(entry.getValue(), java.nio.charset.StandardCharsets.UTF_8)).append("&");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // Remove trailing &
        }
        // 3. MD5 hash
        return org.apache.commons.codec.digest.DigestUtils.md5Hex(sb.toString());
    }
}

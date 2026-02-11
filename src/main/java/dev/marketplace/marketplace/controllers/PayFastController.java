package dev.marketplace.marketplace.controllers;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
import dev.marketplace.marketplace.config.PayFastProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;

/**
 * PayFast payment gateway integration controller.
 *
 * Signature Generation Rules (per PayFast official documentation):
 * 1. INITIAL REQUEST: Include merchant_id and merchant_key, raw values (NO URL encoding), alphabetical order, append passphrase
 * 2. ITN CALLBACK: Exclude merchant_key, raw values (NO URL encoding), alphabetical order, append passphrase
 */
@ConditionalOnProperty(prefix = "payfast", name = "enabled", havingValue = "true")
@RestController
@RequestMapping("/api/payments/payfast")
public class PayFastController {
    private static final Logger log = LoggerFactory.getLogger(PayFastController.class);

    private final PayFastProperties payFastProperties;
    private boolean payfastConfigured = true;

    @Autowired
    private UserService userService;

    @Autowired
    private SubscriptionService subscriptionService;

    public PayFastController(PayFastProperties payFastProperties) {
        this.payFastProperties = payFastProperties;
    }

    @PostConstruct
    public void validatePayFastConfig() {
        StringBuilder missing = new StringBuilder();
        if (payFastProperties.getMerchantId() == null || payFastProperties.getMerchantId().isBlank()) missing.append("merchantId ");
        if (payFastProperties.getMerchantKey() == null || payFastProperties.getMerchantKey().isBlank()) missing.append("merchantKey ");
        if (payFastProperties.getUrl() == null || payFastProperties.getUrl().isBlank()) missing.append("payfastUrl ");

        if (!missing.isEmpty()) {
            log.error("[PayFast] Missing required configuration: {}. Disabling PayFast endpoints.", missing.toString().trim());
            payfastConfigured = false;
        } else {
            log.info("[PayFast] Configuration present; PayFast endpoints enabled (merchantId={})", payFastProperties.getMerchantId());
        }
    }

    @GetMapping("/subscription-url")
    public ResponseEntity<String> getPayFastSubscriptionUrl(
            @RequestParam(required = false) String nameFirst,
            @RequestParam(required = false) String nameLast,
            @RequestParam(required = false) String emailAddress,
            @RequestParam(defaultValue = "Pro Store Subscription") String itemName,
            @RequestParam(defaultValue = "100.00") String amount,
            @RequestParam(defaultValue = "100.00") String recurringAmount,
            @RequestParam(defaultValue = "3") String frequency,
            @RequestParam(defaultValue = "0") String cycles,
            @RequestParam(defaultValue = "pro_store") String planType,
            @RequestParam(required = false) String itemDescription
    ) {
        if (!payfastConfigured) return ResponseEntity.status(503).body("PayFast not configured");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return ResponseEntity.status(401).body("Not authenticated");

        Optional<User> userOpt = userService.getUserByEmail(auth.getName());
        if (userOpt.isEmpty()) return ResponseEntity.status(404).body("User not found");

        User user = userOpt.get();
        if (nameFirst == null || nameFirst.isBlank()) nameFirst = user.getFirstName() != null ? user.getFirstName() : "User";
        if (nameLast == null || nameLast.isBlank()) nameLast = user.getLastName() != null ? user.getLastName() : "Account";
        if (emailAddress == null || emailAddress.isBlank()) emailAddress = user.getEmail();

        nameFirst = nameFirst.trim();
        nameLast = nameLast.trim();
        emailAddress = emailAddress.trim();
        itemName = itemName.trim();
        amount = amount.trim();
        recurringAmount = recurringAmount.trim();

        log.info("[PayFast] Building subscription URL for user: {} {} ({})", nameFirst, nameLast, emailAddress);

        // Build signature using raw values in alphabetical order, append passphrase
        String passphrase = payFastProperties.getPassphrase();
        String sigBase = "amount=" + amount + "&" +
                "cycles=" + cycles + "&" +
                "email_address=" + emailAddress + "&" +
                "frequency=" + frequency + "&" +
                "item_name=" + itemName + "&" +
                "merchant_id=" + payFastProperties.getMerchantId() + "&" +
                "name_first=" + nameFirst + "&" +
                "name_last=" + nameLast + "&" +
                "recurring_amount=" + recurringAmount + "&" +
                "subscription_type=1&" +
                "passphrase=" + passphrase;

        String signature = org.apache.commons.codec.digest.DigestUtils.md5Hex(sigBase);
        log.info("[PayFast] Generated subscription signature");

        // Build URL with URL-encoded parameters (for transmission)
        String url = payFastProperties.getUrl() + "?" +
                "amount=" + enc(amount) +
                "&cancel_url=" + enc(payFastProperties.getCancelUrl()) +
                "&custom_str1=" + enc(planType) +
                "&custom_str2=" + enc(emailAddress) +
                "&cycles=" + cycles +
                "&email_address=" + enc(emailAddress) +
                "&frequency=" + frequency +
                "&item_name=" + enc(itemName) +
                "&merchant_id=" + payFastProperties.getMerchantId() +
                "&merchant_key=" + payFastProperties.getMerchantKey() +
                "&name_first=" + enc(nameFirst) +
                "&name_last=" + enc(nameLast) +
                "&notify_url=" + enc(payFastProperties.getNotifyUrl()) +
                "&recurring_amount=" + enc(recurringAmount) +
                "&return_url=" + enc(payFastProperties.getReturnUrl()) +
                "&subscription_type=1" +
                "&signature=" + signature;

        log.info("[PayFast] Subscription URL generated successfully");
        return ResponseEntity.ok(url);
    }

    private String enc(String s) {
        if (s == null) return "";
        try {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }

    @GetMapping("/debug/signature")
    public ResponseEntity<Map<String, String>> debugSignature(
            @RequestParam String merchant_id,
            @RequestParam String merchant_key,
            @RequestParam String amount,
            @RequestParam String passphrase
    ) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("amount", amount);
        params.put("merchant_id", merchant_id);
        params.put("merchant_key", merchant_key);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        sb.append("passphrase=").append(passphrase);

        String baseString = sb.toString();
        String signature = org.apache.commons.codec.digest.DigestUtils.md5Hex(baseString);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("baseString", baseString);
        response.put("signature", signature);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/itn")
    public ResponseEntity<String> debugPayFastITN() {
        log.info("[PayFast ITN] GET /itn endpoint hit (debug)");
        return ResponseEntity.ok("PayFast ITN endpoint is reachable (GET)");
    }

    @PostMapping("/itn")
    public ResponseEntity<String> handlePayFastITN(@RequestParam Map<String, String> payload) {
        if (!payfastConfigured) {
            log.warn("[PayFast ITN] Received ITN but PayFast not configured; ignoring.");
            return ResponseEntity.ok("OK");
        }

        log.info("[PayFast ITN] ========== RECEIVED ITN CALLBACK ==========");
        log.info("[PayFast ITN] Full payload received: {}", payload);

        String receivedSignature = payload.get("signature");
        log.info("[PayFast ITN] Received signature: {}", receivedSignature);

        // Validate signature by regenerating it
        // PayFast ITN Spec: exclude merchant_key, use raw values (NO URL encoding), append passphrase
        Map<String, String> paramsForValidation = new LinkedHashMap<>(payload);
        paramsForValidation.remove("signature");

        log.info("[PayFast ITN] Params for signature validation (signature removed)");

        // Generate expected signature using PayFast's official method
        String expectedSignature = generateSignatureForITN(paramsForValidation);

        log.info("[PayFast ITN] Expected signature: {}", expectedSignature);
        log.info("[PayFast ITN] Received signature: {}", receivedSignature);

        boolean match = receivedSignature != null && receivedSignature.equals(expectedSignature);

        if (!match) {
            log.error("[PayFast ITN] SIGNATURE MISMATCH! Received: {}, Expected: {}", receivedSignature, expectedSignature);
            log.error("[PayFast ITN] Aborting transaction processing due to signature mismatch");
            return ResponseEntity.status(400).body("Signature mismatch");
        }

        log.info("[PayFast ITN] Signature validation passed!");

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
        log.info("[PayFast ITN] ========== END ITN CALLBACK ==========");
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

    /**
     * Generate signature for ITN (Instant Transaction Notification) callbacks.
     * PayFast Spec: Exclude merchant_key, use raw values (NO URL encoding), alphabetical order, append passphrase.
     */
    private String generateSignatureForITN(Map<String, String> params) {
        log.info("[PayFast Signature] ========== SIGNATURE GENERATION FOR ITN CALLBACK ==========");

        // 1. Filter: exclude empty values and signature field
        Map<String, String> filtered = params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty() && !"signature".equals(e.getKey()))
            .sorted(Map.Entry.comparingByKey())  // Alphabetical order
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));

        // 2. CRITICAL: Exclude merchant_key from ITN signature (PayFast spec)
        filtered.remove("merchant_key");

        log.debug("[PayFast Signature] Filtered params (no merchant_key): {}", filtered);

        // 3. Build base string: key=value&key=value&... (raw values, NO URL encoding)
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }

        // Trim trailing '&'
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '&') {
            sb.setLength(sb.length() - 1);
        }

        // 4. Append passphrase at the end
        String passphrase = payFastProperties.getPassphrase();
        if (passphrase != null && !passphrase.isBlank()) {
            sb.append("&passphrase=").append(passphrase);
            String masked = maskPassphrase(passphrase);
            log.debug("[PayFast Signature] Passphrase appended (masked={}, length={})", masked, passphrase.length());
        } else {
            log.warn("[PayFast Signature] No passphrase configured!");
        }

        String baseString = sb.toString();
        log.debug("[PayFast Signature] Base string to hash: {}", baseString);

        // 5. MD5 hash
        String signature = org.apache.commons.codec.digest.DigestUtils.md5Hex(baseString);
        log.info("[PayFast Signature] Generated MD5 signature: {}", signature);
        log.info("[PayFast Signature] ========== SIGNATURE GENERATION END ==========");

        return signature;
    }

    /**
     * Generate signature for initial payment request.
     * PayFast Spec: Include merchant_key, use raw values (NO URL encoding), alphabetical order, append passphrase.
     */
    private String generateSignatureForInitialRequest(Map<String, String> params) {
        log.info("[PayFast Signature] ========== SIGNATURE GENERATION FOR INITIAL REQUEST ==========");

        // 1. Filter: exclude empty values and signature field
        Map<String, String> filtered = params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty() && !"signature".equals(e.getKey()))
            .sorted(Map.Entry.comparingByKey())  // Alphabetical order
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));

        log.debug("[PayFast Signature] Filtered params (sorted): {}", filtered);

        // 2. Build base string: key=value&key=value&... (raw values, NO URL encoding)
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }

        // Trim trailing '&'
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '&') {
            sb.setLength(sb.length() - 1);
        }

        // 3. Append passphrase at the end
        String passphrase = payFastProperties.getPassphrase();
        if (passphrase != null && !passphrase.isBlank()) {
            sb.append("&passphrase=").append(passphrase);
            String masked = maskPassphrase(passphrase);
            log.debug("[PayFast Signature] Passphrase appended (masked={}, length={})", masked, passphrase.length());
        } else {
            log.warn("[PayFast Signature] No passphrase configured!");
        }

        String baseString = sb.toString();
        log.debug("[PayFast Signature] Base string to hash: {}", baseString);

        // 4. MD5 hash
        String signature = org.apache.commons.codec.digest.DigestUtils.md5Hex(baseString);
        log.info("[PayFast Signature] Generated MD5 signature: {}", signature);
        log.info("[PayFast Signature] ========== SIGNATURE GENERATION END ==========");

        return signature;
    }

    private String maskPassphrase(String passphrase) {
        if (passphrase == null || passphrase.isEmpty()) return "";
        if (passphrase.length() <= 4) return "****";
        return passphrase.substring(0, 2) + "****" + passphrase.substring(passphrase.length() - 2);
    }
}


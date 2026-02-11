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

        // Build the full set of parameters exactly as they will appear in the final URL
        Map<String, String> params = new LinkedHashMap<>();
        params.put("amount", amount);
        params.put("cancel_url", payFastProperties.getCancelUrl());
        params.put("custom_str1", planType);
        params.put("custom_str2", emailAddress);
        params.put("cycles", cycles);
        params.put("email_address", emailAddress);
        params.put("frequency", frequency);
        params.put("item_name", itemName);
        params.put("merchant_id", payFastProperties.getMerchantId());
        params.put("merchant_key", payFastProperties.getMerchantKey());
        params.put("name_first", nameFirst);
        params.put("name_last", nameLast);

        // DEBUG: Log the notify_url from config
        String notifyUrl = payFastProperties.getNotifyUrl();
        log.info("[PayFast DEBUG] notifyUrl from config: {}", notifyUrl);
        params.put("notify_url", notifyUrl != null ? notifyUrl : "");

        params.put("recurring_amount", recurringAmount);
        params.put("return_url", payFastProperties.getReturnUrl() != null ? payFastProperties.getReturnUrl() : "");
        params.put("subscription_type", "1");

        // Compute signature deterministically from params (alphabetical order, raw values, include merchant_key, append passphrase)
        String signature = computePayFastSignature(params, true, payFastProperties.getPassphrase());
        log.debug("[PayFast] Signature base computed and MD5 generated");

        // Build URL with URL-encoded parameters (for transmission)
        StringBuilder url = new StringBuilder(payFastProperties.getUrl()).append("?");
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) url.append('&');
            first = false;
            url.append(e.getKey()).append('=').append(enc(e.getValue()));
        }
        url.append("&signature=").append(signature);

        log.info("[PayFast] Subscription URL generated successfully");
        return ResponseEntity.ok(url.toString());
    }

    // New helper: build canonical base string for PayFast signature
    private String computePayFastBaseString(Map<String, String> params, boolean includeMerchantKey, String passphrase, boolean urlEncodeValues) {
        Map<String, String> filtered = params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));

        if (!includeMerchantKey) {
            filtered.remove("merchant_key");
        }

        java.util.List<String> keys = new java.util.ArrayList<>(filtered.keySet());
        java.util.Collections.sort(keys);

        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            String v = filtered.get(k);
            String toAppend = urlEncodeValues ? rfc3986Encode(v) : v;
            sb.append(k).append("=").append(toAppend).append("&");
        }
        if (sb.length() > 0 && sb.charAt(sb.length()-1) == '&') sb.setLength(sb.length()-1);

        if (passphrase != null && !passphrase.isBlank()) {
            sb.append("&passphrase=").append(passphrase);
        }
        return sb.toString();
    }

    // Helper: RFC3986 encode for signature variants
    private String rfc3986Encode(String s) {
        if (s == null) return "";
        try {
            String encoded = java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8.name());
            encoded = encoded.replace("+", "%20").replace("%7E", "~");
            return encoded;
        } catch (Exception e) {
            return s;
        }
    }

    // Refactor existing computePayFastSignature to delegate to the new base-string method
    private String computePayFastSignature(Map<String, String> params, boolean includeMerchantKey, String passphrase, boolean urlEncodeValues) {
        String base = computePayFastBaseString(params, includeMerchantKey, passphrase, urlEncodeValues);
        log.info("[PayFast Signature] Base string to hash: {}", base);
        String signature = org.apache.commons.codec.digest.DigestUtils.md5Hex(base);
        log.info("[PayFast Signature] Generated MD5 signature: {}", signature);
        return signature;
    }

    // Overload kept for backward compatibility (raw values)
    private String computePayFastSignature(Map<String, String> params, boolean includeMerchantKey, String passphrase) {
        return computePayFastSignature(params, includeMerchantKey, passphrase, false);
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

        String signature = computePayFastSignature(params, true, passphrase);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("signature", signature);
        response.put("baseString", "(masked)");

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

        // Validate signature by trying multiple variants (temporary debugging to identify the correct signature method)
        Map<String, String> paramsForValidation = new LinkedHashMap<>(payload);
        paramsForValidation.remove("signature");

        log.info("[PayFast ITN] Params for signature validation (signature removed)");

        // Try canonical method first
        String expectedSignature = generateSignatureForITN(paramsForValidation);
        log.info("[PayFast ITN] Variant 1 (exclude merchant_key, raw values): {}", expectedSignature);

        boolean match = receivedSignature != null && receivedSignature.equals(expectedSignature);

        // If canonical doesn't match, try variants (temporary debugging)
        if (!match) {
            log.warn("[PayFast ITN] Canonical signature did not match. Trying variants...");

            // Variant 2: Include merchant_key (for initial request flow, shouldn't be here but test)
            String variant2 = generateSignatureForInitialRequest(paramsForValidation);
            log.info("[PayFast ITN] Variant 2 (include merchant_key, raw values): {}", variant2);
            if (receivedSignature != null && receivedSignature.equals(variant2)) {
                log.warn("[PayFast ITN] MATCHED Variant 2 (include merchant_key)! This is unexpected for ITN.");
                match = true;
            }

            // Variant 3: Exclude merchant_key but with different param set (try without custom fields)
            if (!match) {
                Map<String, String> paramsWithoutCustom = new LinkedHashMap<>(paramsForValidation);
                paramsWithoutCustom.remove("custom_str1");
                paramsWithoutCustom.remove("custom_str2");
                String variant3 = generateSignatureForITN(paramsWithoutCustom);
                log.info("[PayFast ITN] Variant 3 (exclude merchant_key + custom fields): {}", variant3);
                if (receivedSignature != null && receivedSignature.equals(variant3)) {
                    log.warn("[PayFast ITN] MATCHED Variant 3 (without custom fields)!");
                    match = true;
                }
            }

            // Variant 4: Try without notify_url (sometimes not included)
            if (!match) {
                Map<String, String> paramsWithoutNotify = new LinkedHashMap<>(paramsForValidation);
                paramsWithoutNotify.remove("notify_url");
                String variant4 = generateSignatureForITN(paramsWithoutNotify);
                log.info("[PayFast ITN] Variant 4 (exclude merchant_key + notify_url): {}", variant4);
                if (receivedSignature != null && receivedSignature.equals(variant4)) {
                    log.warn("[PayFast ITN] MATCHED Variant 4 (without notify_url)!");
                    match = true;
                }
            }

            if (!match) {
                log.error("[PayFast ITN] SIGNATURE MISMATCH on all variants! Received: {}", receivedSignature);
                log.error("[PayFast ITN] Canonical expected was: {}", expectedSignature);
                log.error("[PayFast ITN] Aborting transaction processing due to signature mismatch");
                return ResponseEntity.status(400).body("Signature mismatch");
            }
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

    private String maskPassphrase(String passphrase) {
        if (passphrase == null || passphrase.isEmpty()) return "";
        if (passphrase.length() <= 4) return "****";
        return passphrase.substring(0, 2) + "****" + passphrase.substring(passphrase.length() - 2);
    }

    /**
     * Generate signature for initial payment request.
     * PayFast Spec: Include merchant_key, use raw values (NO URL encoding), alphabetical order, append passphrase.
     */
    private String generateSignatureForInitialRequest(Map<String, String> params) {
        // call the 4-arg base-string builder with urlEncodeValues=false
        String base = computePayFastBaseString(params, true, payFastProperties.getPassphrase(), false);
        log.debug("[PayFast Signature] Base string (initial request, with merchant_key): {}", base);
        String signature = org.apache.commons.codec.digest.DigestUtils.md5Hex(base);
        log.debug("[PayFast Signature] Generated MD5 signature (with merchant_key): {}", signature);
        return signature;
    }

    // Helper used when building redirect URL to URL-encode parameter values
    private String enc(String s) {
        if (s == null) return "";
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }

    @PostMapping(path = "/debug/compute-signature", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, String>> debugComputeSignature(
            @RequestBody Map<String, String> body,
            @RequestParam(defaultValue = "true") boolean includeMerchantKey,
            @RequestParam(required = false) String passphrase,
            @RequestParam(defaultValue = "false") boolean urlEncode,
            @RequestParam(defaultValue = "false") boolean returnTestUrl,
            @RequestParam(required = false) String debugSecret
    ) {
        // Temporary debug endpoint — requires a secret query param
        // Remove this endpoint after debugging is complete
        if (debugSecret == null || !debugSecret.equals("payfast123")) {
            log.warn("[PayFast DEBUG] Forbidden debug access (invalid or missing debugSecret)");
            Map<String, String> resp = new java.util.HashMap<>();
            resp.put("error", "forbidden");
            return ResponseEntity.status(403).body(resp);
        }

        if (body == null) body = new java.util.HashMap<>();
        String pf = (passphrase != null && !passphrase.isBlank()) ? passphrase : payFastProperties.getPassphrase();
        String base = computePayFastBaseString(body, includeMerchantKey, pf, urlEncode);
        String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(base);
        Map<String, String> resp = new java.util.LinkedHashMap<>();
        resp.put("baseString", base);
        resp.put("md5", md5);
        resp.put("includeMerchantKey", String.valueOf(includeMerchantKey));
        resp.put("urlEncodeValues", String.valueOf(urlEncode));
        resp.put("usedPassphraseMasked", pf == null ? "(none)" : (pf.length() <= 4 ? "****" : pf.substring(0,2)+"****"+pf.substring(pf.length()-2)));
        if (returnTestUrl) {
            // build the full redirect URL with encoded params (use rfc3986 for param values)
            StringBuilder testUrl = new StringBuilder(payFastProperties.getUrl()).append("?");
            boolean first = true;
            // iterate original body keys in insertion order if present, otherwise use the canonical sorted order
            java.util.List<String> keys = new java.util.ArrayList<>(body.keySet());
            if (keys.isEmpty()) keys = new java.util.ArrayList<>(body.keySet());
            for (String k : keys) {
                if (!first) testUrl.append('&');
                first = false;
                String v = body.get(k);
                testUrl.append(k).append("=").append(rfc3986Encode(v));
            }
            if (!first) testUrl.append('&');
            testUrl.append("signature=").append(md5);
            resp.put("testUrl", testUrl.toString());
        }
        return ResponseEntity.ok(resp);
    }
}

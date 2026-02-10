package dev.marketplace.marketplace.controllers;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
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
import dev.marketplace.marketplace.config.PayFastProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;

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
            @RequestParam(defaultValue = "Pro Store Subscription") String itemName,
            @RequestParam(defaultValue = "100.00") String amount,
            @RequestParam(defaultValue = "100.00") String recurringAmount,
            @RequestParam(defaultValue = "3") String frequency, // 3 = monthly
            @RequestParam(defaultValue = "0") String cycles, // 0 = indefinite
            @RequestParam(defaultValue = "pro_store") String planType, // plan type
            @RequestParam String userEmail // new required param
    ) {
        if (!payfastConfigured) {
            return ResponseEntity.status(503).body("PayFast not configured on this instance");
        }

        log.info("[PayFast] ========== GENERATING SUBSCRIPTION URL ==========");
        log.info("[PayFast] Input parameters: itemName={}, amount={}, recurringAmount={}, frequency={}, cycles={}, planType={}, userEmail={}",
            itemName, amount, recurringAmount, frequency, cycles, planType, userEmail);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("merchant_id", payFastProperties.getMerchantId());
        params.put("merchant_key", payFastProperties.getMerchantKey());
        params.put("amount", amount);
        params.put("item_name", itemName);
        params.put("subscription_type", "1");
        params.put("recurring_amount", recurringAmount);
        params.put("frequency", frequency);
        params.put("cycles", cycles);
        params.put("custom_str1", planType); // pass plan type
        params.put("custom_str2", userEmail); // pass user email

        log.info("[PayFast] Parameters map before signature: {}", params);

        // Generate signature excluding merchant_key (some PayFast setups expect merchant_key excluded)
        String signature = generateSignature(params, false);

        StringBuilder url = new StringBuilder(payFastProperties.getUrl() + "?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
               .append("=")
               .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
               .append("&");
        }
        url.append("signature=").append(signature);
        log.info("[PayFast] Final URL generated: {}", url);
        log.info("[PayFast] ========== END SUBSCRIPTION URL GENERATION ==========");
        return ResponseEntity.ok(url.toString());
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

        // Log all parameters
        payload.forEach((key, value) -> log.info("[PayFast ITN] PARAM: {} = {}", key, value));

        String receivedSignature = payload.get("signature");
        log.info("[PayFast ITN] Received signature: {}", receivedSignature);

        // Validate signature by regenerating it
        // NOTE: PayFast ITN includes transaction details but excludes merchant_key and other setup params
        Map<String, String> paramsForValidation = new LinkedHashMap<>(payload);
        paramsForValidation.remove("signature");

        log.info("[PayFast ITN] Params before signature generation: {}", paramsForValidation);

        String generatedSignature = generateSignature(paramsForValidation);
        String generatedSignatureAlt = generateSignature(paramsForValidation, false);
        log.info("[PayFast ITN] Generated signature for validation (default): {}", generatedSignature);
        log.info("[PayFast ITN] Generated signature for validation (no merchant_key): {}", generatedSignatureAlt);

        log.info("[PayFast ITN] Comparing signatures:");
        log.info("[PayFast ITN]   Received:  {}", receivedSignature);
        log.info("[PayFast ITN]   Generated (default): {}", generatedSignature);
        log.info("[PayFast ITN]   Generated (no merchant_key): {}", generatedSignatureAlt);
        log.info("[PayFast ITN]   Match default: {}", generatedSignature.equals(receivedSignature));
        log.info("[PayFast ITN]   Match no-merchant-key: {}", generatedSignatureAlt.equals(receivedSignature));

        if (!generatedSignature.equals(receivedSignature) && !generatedSignatureAlt.equals(receivedSignature)) {
            log.error("[PayFast ITN] SIGNATURE MISMATCH! Received: {}, Generated(default): {}, Generated(noMerchantKey): {}", receivedSignature, generatedSignature, generatedSignatureAlt);
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

    private String generateSignature(Map<String, String> params) {
        return generateSignature(params, true);
    }

    private String generateSignature(Map<String, String> params, boolean includeMerchantKey) {
        log.info("[PayFast Signature] ========== SIGNATURE GENERATION START (includeMerchantKey={}) ==========", includeMerchantKey);

        // 1. Exclude empty values and the 'signature' field
        log.info("[PayFast Signature] Input params count: {}", params.size());
        log.info("[PayFast Signature] Input params: {}", params);

        Map<String, String> filtered = params.entrySet().stream()
            .filter(e -> {
                boolean hasValue = e.getValue() != null && !e.getValue().isEmpty();
                boolean isNotSignature = !"signature".equals(e.getKey());
                boolean keep = hasValue && isNotSignature;

                if (!keep) {
                    if (!hasValue) {
                        log.debug("[PayFast Signature] Filtering out {} (empty/null value)", e.getKey());
                    } else {
                        log.debug("[PayFast Signature] Filtering out {} (signature field)", e.getKey());
                    }
                }
                return keep;
            })
            .sorted(Map.Entry.comparingByKey())
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));

        // If configured to exclude merchant_key, remove it now (if present)
        if (!includeMerchantKey) {
            if (filtered.containsKey("merchant_key")) {
                filtered.remove("merchant_key");
                log.debug("[PayFast Signature] merchant_key excluded from signature generation");
            }
        }

        log.info("[PayFast Signature] Filtered params count: {}", filtered.size());
        log.info("[PayFast Signature] Filtered params (sorted alphabetically): {}", filtered);

        // 2. Build the base string (DO NOT URL encode for signature generation)
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            log.debug("[PayFast Signature] Adding param: {}={}", entry.getKey(), entry.getValue());
        }

        // Trim trailing '&' if present
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '&') {
            sb.setLength(sb.length() - 1);
        }

        // 3. Append passphrase at the end if configured (do not URL-encode passphrase)
        String passphrase = payFastProperties.getPassphrase();
        if (passphrase != null && !passphrase.isBlank()) {
            sb.append("&passphrase=").append(passphrase);
            // Mask passphrase for logs and provide a hash so we can verify correctness without exposing the secret
            String masked;
            if (passphrase.length() <= 4) {
                masked = "****";
            } else if (passphrase.length() <= 8) {
                masked = passphrase.substring(0, 1) + "****" + passphrase.substring(passphrase.length() - 1);
            } else {
                masked = passphrase.substring(0, 2) + "****" + passphrase.substring(passphrase.length() - 2);
            }
            String passphraseHash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(passphrase);
            log.info("[PayFast Signature] Passphrase appended (masked={}, sha256={}, length={})", masked, passphraseHash, passphrase.length());
        } else {
            log.warn("[PayFast Signature] No passphrase configured!");
        }

        String signatureString = sb.toString();
        log.info("[PayFast Signature] Base string to hash (length={}): {}", signatureString.length(), signatureString);

        // 4. MD5 hash
        String signature = org.apache.commons.codec.digest.DigestUtils.md5Hex(signatureString);
        log.info("[PayFast Signature] Generated MD5 signature: {}", signature);
        log.info("[PayFast Signature] ========== SIGNATURE GENERATION END ==========", includeMerchantKey);

        return signature;
    }
}

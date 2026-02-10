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

        // Sanitize inputs to avoid stray quote characters or whitespace affecting signature
        String safeItemName = sanitizeParam(itemName);
        String safeAmount = sanitizeParam(amount);
        String safeRecurringAmount = sanitizeParam(recurringAmount);
        String safeFrequency = sanitizeParam(frequency);
        String safeCycles = sanitizeParam(cycles);
        String safePlanType = sanitizeParam(planType);
        String safeUserEmail = sanitizeParam(userEmail);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("merchant_id", payFastProperties.getMerchantId());
        params.put("merchant_key", payFastProperties.getMerchantKey());
        params.put("amount", safeAmount);
        params.put("item_name", safeItemName);
        params.put("subscription_type", "1");
        params.put("recurring_amount", safeRecurringAmount);
        params.put("frequency", safeFrequency);
        params.put("cycles", safeCycles);
        params.put("custom_str1", safePlanType); // pass plan type
        params.put("custom_str2", safeUserEmail); // pass user email

        log.info("[PayFast] Parameters map before signature: {}", params);

        // Generate multiple signature variants for debugging and compatibility
        String sig_include_encoded = generateSignature(params, true, true);
        String sig_include_plain = generateSignature(params, true, false);
        String sig_exclude_encoded = generateSignature(params, false, true);
        String sig_exclude_plain = generateSignature(params, false, false);

        log.info("[PayFast] Signature variants (include+encoded={}, include+plain={}, exclude+encoded={}, exclude+plain={})",
                sig_include_encoded, sig_include_plain, sig_exclude_encoded, sig_exclude_plain);

        // Primary choice: EXCLUDE merchant_key and use PLAIN (non-encoded) values for signature
        // PayFast signature validation does NOT include merchant_key and expects plain values
        // So we must also REMOVE merchant_key from the URL parameters
        Map<String, String> urlParams = new LinkedHashMap<>(params);
        urlParams.remove("merchant_key");

        StringBuilder url = new StringBuilder(payFastProperties.getUrl() + "?");
        urlParams.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> url.append(rfc3986Encode(entry.getKey()))
                   .append("=")
                   .append(rfc3986Encode(entry.getValue()))
                   .append("&"));
        // use the exclude+plain signature (merchant_key excluded from signature, plain values used for signature computation)
        url.append("signature=").append(sig_exclude_plain);
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

        // Generate all variants to compare with received signature
        String gen_inc_enc = generateSignature(paramsForValidation, true, true);
        String gen_inc_plain = generateSignature(paramsForValidation, true, false);
        String gen_exc_enc = generateSignature(paramsForValidation, false, true);
        String gen_exc_plain = generateSignature(paramsForValidation, false, false);

        log.info("[PayFast ITN] Generated signatures: include+encoded={}, include+plain={}, exclude+encoded={}, exclude+plain={}", gen_inc_enc, gen_inc_plain, gen_exc_enc, gen_exc_plain);

        boolean match = receivedSignature != null && (
            receivedSignature.equals(gen_inc_enc) ||
            receivedSignature.equals(gen_inc_plain) ||
            receivedSignature.equals(gen_exc_enc) ||
            receivedSignature.equals(gen_exc_plain)
        );

        log.info("[PayFast ITN] Comparing signatures: Received={}, Match={}", receivedSignature, match);

        if (!match) {
            log.error("[PayFast ITN] SIGNATURE MISMATCH! Received: {}, Generated variants: {} | {} | {} | {}", receivedSignature, gen_inc_enc, gen_inc_plain, gen_exc_enc, gen_exc_plain);
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

    private String generateSignature(Map<String, String> params, boolean includeMerchantKey, boolean urlEncodeValues) {
        log.info("[PayFast Signature] ========== SIGNATURE GENERATION START (includeMerchantKey={}, urlEncodeValues={}) ==========", includeMerchantKey, urlEncodeValues);

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

        // 2. Build the base string
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            String value = entry.getValue();
            String toAppend = urlEncodeValues ? rfc3986Encode(value) : value;
            sb.append(entry.getKey()).append("=").append(toAppend).append("&");
            log.debug("[PayFast Signature] Adding param: {}={}", entry.getKey(), toAppend);
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
                masked = passphrase.charAt(0) + "****" + passphrase.charAt(passphrase.length() - 1);
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
        log.info("[PayFast Signature] ========== SIGNATURE GENERATION END ==========");

        return signature;
    }

    @GetMapping("/debug/signatures")
    public ResponseEntity<Map<String, Object>> debugSignatureVariants(
            @RequestParam(defaultValue = "Pro Store Subscription") String itemName,
            @RequestParam(defaultValue = "100.00") String amount,
            @RequestParam(defaultValue = "100.00") String recurringAmount,
            @RequestParam(defaultValue = "3") String frequency,
            @RequestParam(defaultValue = "0") String cycles,
            @RequestParam(defaultValue = "pro_store") String planType,
            @RequestParam String userEmail
    ) {
        if (!payfastConfigured) {
            return ResponseEntity.status(503).body(Map.of("error", "PayFast not configured on this instance"));
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("merchant_id", payFastProperties.getMerchantId());
        params.put("merchant_key", payFastProperties.getMerchantKey());
        params.put("amount", amount);
        params.put("item_name", itemName);
        params.put("subscription_type", "1");
        params.put("recurring_amount", recurringAmount);
        params.put("frequency", frequency);
        params.put("cycles", cycles);
        params.put("custom_str1", planType);
        params.put("custom_str2", userEmail);

        Map<String, Object> out = new java.util.HashMap<>();

        // Build variants and base strings
        Map<String, Object> v1 = buildVariant(params, true, true);
        Map<String, Object> v2 = buildVariant(params, true, false);
        Map<String, Object> v3 = buildVariant(params, false, true);
        Map<String, Object> v4 = buildVariant(params, false, false);

        out.put("include_encoded", v1);
        out.put("include_plain", v2);
        out.put("exclude_encoded", v3);
        out.put("exclude_plain", v4);

        // Provide a ready-to-open URL example for each variant (signature only differs)
        // IMPORTANT: Parameters MUST be in alphabetical order to match signature computation
        String baseUrl = payFastProperties.getUrl() + "?";
        StringBuilder commonQs = new StringBuilder();
        params.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> commonQs.append(rfc3986Encode(e.getKey()))
                    .append("=")
                    .append(rfc3986Encode(e.getValue()))
                    .append("&"));
        String common = commonQs.toString();
        if (common.endsWith("&")) common = common.substring(0, common.length() - 1);

        out.put("example_url_include_encoded", baseUrl + common + "&signature=" + v1.get("signature"));
        out.put("example_url_include_plain", baseUrl + common + "&signature=" + v2.get("signature"));
        out.put("example_url_exclude_encoded", baseUrl + common + "&signature=" + v3.get("signature"));
        out.put("example_url_exclude_plain", baseUrl + common + "&signature=" + v4.get("signature"));

        return ResponseEntity.ok(out);
    }

    // Helper to build base string and signature for a variant
    private Map<String, Object> buildVariant(Map<String, String> params, boolean includeMerchantKey, boolean urlEncodeValues) {
        Map<String, Object> result = new java.util.HashMap<>();

        // Filter & sort like generateSignature
        Map<String, String> filtered = params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty() && !"signature".equals(e.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));

        if (!includeMerchantKey) filtered.remove("merchant_key");

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            String toAppend = urlEncodeValues ? rfc3986Encode(entry.getValue()) : entry.getValue();
            sb.append(entry.getKey()).append("=").append(toAppend).append("&");
        }
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '&') sb.setLength(sb.length() - 1);

        String passphrase = payFastProperties.getPassphrase();
        if (passphrase != null && !passphrase.isBlank()) {
            sb.append("&passphrase=").append(passphrase);
        }

        String baseString = sb.toString();
        String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(baseString);

        result.put("baseString", baseString);
        result.put("signature", md5);

        // Masked passphrase info
        if (passphrase != null) {
            String masked = passphrase.length() <= 4 ? "****" : passphrase.substring(0, 2) + "****" + passphrase.substring(Math.max(2, passphrase.length() - 2));
            result.put("passphrase_masked", masked);
            result.put("passphrase_sha256", org.apache.commons.codec.digest.DigestUtils.sha256Hex(passphrase));
            result.put("passphrase_length", passphrase.length());
        }

        return result;
    }


    // Helper to sanitize incoming request params (trim and remove surrounding quotes)
    private String sanitizeParam(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.length() >= 2 && ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'")))) {
            t = t.substring(1, t.length() - 1).trim();
        }
        // Remove any stray double-quote characters embedded at the end/start
        if (t.endsWith("\"")) t = t.substring(0, t.length() - 1);
        if (t.startsWith("\"")) t = t.substring(1);
        return t;
    }

    // RFC-3986 compatible percent-encoding helper (spaces -> %20, preserves ~)
    private String rfc3986Encode(String s) {
        if (s == null) return "";
        String encoded = URLEncoder.encode(s, StandardCharsets.UTF_8);
        // URLEncoder produces + for spaces; RFC-3986 requires %20. Also keep ~ unescaped like rawurlencode in PHP
        encoded = encoded.replace("+", "%20").replace("%7E", "~");
        return encoded;
    }
}

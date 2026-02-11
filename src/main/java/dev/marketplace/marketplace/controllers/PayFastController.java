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
            @RequestParam(required = false) String nameFirst,
            @RequestParam(required = false) String nameLast,
            @RequestParam(required = false) String emailAddress,
            @RequestParam(defaultValue = "Pro Store Subscription") String itemName,
            @RequestParam(defaultValue = "100.00") String amount,
            @RequestParam(defaultValue = "100.00") String recurringAmount,
            @RequestParam(defaultValue = "3") String frequency, // 3 = monthly
            @RequestParam(defaultValue = "0") String cycles, // 0 = indefinite
            @RequestParam(defaultValue = "pro_store") String planType, // plan type
            @RequestParam(required = false) String itemDescription
    ) {
        if (!payfastConfigured) {
            return ResponseEntity.status(503).body("PayFast not configured on this instance");
        }

        // Get authenticated user info if parameters not provided
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("User not authenticated");
        }

        String usernameOrEmail = authentication.getName();
        Optional<User> userOpt = userService.getUserByEmail(usernameOrEmail);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        User user = userOpt.get();

        // Use provided parameters or fall back to user data
        if (nameFirst == null || nameFirst.isBlank()) {
            nameFirst = user.getFirstName() != null ? user.getFirstName() : "User";
        }
        if (nameLast == null || nameLast.isBlank()) {
            nameLast = user.getLastName() != null ? user.getLastName() : "Account";
        }
        if (emailAddress == null || emailAddress.isBlank()) {
            emailAddress = user.getEmail();
        }

        log.info("[PayFast] ========== GENERATING SUBSCRIPTION URL ==========");
        log.info("[PayFast] Input parameters: nameFirst={}, nameLast={}, emailAddress={}, itemName={}, amount={}, recurringAmount={}, frequency={}, cycles={}, planType={}",
            nameFirst, nameLast, emailAddress, itemName, amount, recurringAmount, frequency, cycles, planType);

        // Sanitize inputs to avoid stray quote characters or whitespace affecting signature
        // Only decode request parameters (itemName, amount, etc.), not user fields
        String safeNameFirst = sanitizeParam(nameFirst);  // From user, don't decode
        String safeNameLast = sanitizeParam(nameLast);    // From user, don't decode
        String safeEmailAddress = sanitizeParam(emailAddress);  // From user, don't decode
        String safeItemName = sanitizeAndDecode(itemName);      // From request, decode
        String safeAmount = sanitizeAndDecode(amount);          // From request, decode
        String safeRecurringAmount = sanitizeAndDecode(recurringAmount);  // From request, decode
        String safeFrequency = sanitizeAndDecode(frequency);    // From request, decode
        String safeCycles = sanitizeAndDecode(cycles);          // From request, decode
        String safePlanType = sanitizeAndDecode(planType);      // From request, decode
        String safeItemDescription = itemDescription != null ? sanitizeAndDecode(itemDescription) : "";  // From request, decode

        // Build signature params - ONLY standard PayFast fields (no custom fields in signature)
        // IMPORTANT: Include merchant_key in signature (PayFast requires it)
        Map<String, String> signatureParams = new LinkedHashMap<>();
        signatureParams.put("amount", safeAmount);
        signatureParams.put("cycles", safeCycles);
        signatureParams.put("email_address", safeEmailAddress);
        signatureParams.put("frequency", safeFrequency);
        signatureParams.put("item_name", safeItemName);
        if (!safeItemDescription.isEmpty()) {
            signatureParams.put("item_description", safeItemDescription);
        }
        signatureParams.put("merchant_id", payFastProperties.getMerchantId());
        signatureParams.put("merchant_key", payFastProperties.getMerchantKey());  // INCLUDE merchant_key
        signatureParams.put("name_first", safeNameFirst);
        signatureParams.put("name_last", safeNameLast);
        signatureParams.put("recurring_amount", safeRecurringAmount);
        signatureParams.put("subscription_type", "1");

        log.info("[PayFast] Signature params (alphabetically sorted): {}", signatureParams);

        // Build the URL params FIRST - includes all standard fields plus custom fields for passing metadata
        Map<String, String> urlParams = new LinkedHashMap<>(signatureParams);
        // Add merchant_key to URL
        urlParams.put("merchant_key", payFastProperties.getMerchantKey());
        urlParams.put("custom_str1", safePlanType); // plan type for ITN callback
        urlParams.put("custom_str2", safeEmailAddress); // user email for ITN callback
        if (payFastProperties.getReturnUrl() != null && !payFastProperties.getReturnUrl().isEmpty()) {
            urlParams.put("return_url", payFastProperties.getReturnUrl());
        }
        if (payFastProperties.getCancelUrl() != null && !payFastProperties.getCancelUrl().isEmpty()) {
            urlParams.put("cancel_url", payFastProperties.getCancelUrl());
        }
        if (payFastProperties.getNotifyUrl() != null && !payFastProperties.getNotifyUrl().isEmpty()) {
            urlParams.put("notify_url", payFastProperties.getNotifyUrl());
        }

        // Generate signature from standard fields ONLY (NOT custom fields or URLs)
        String signature = generateSignatureForInitialRequest(signatureParams);
        log.info("[PayFast] Generated signature: {}", signature);

        // Build URL in alphabetical order
        StringBuilder url = new StringBuilder(payFastProperties.getUrl() + "?");
        urlParams.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> {
                url.append(rfc3986Encode(e.getKey()))
                   .append("=")
                   .append(rfc3986Encode(e.getValue()))
                   .append("&");
            });
        url.append("signature=").append(signature);

        log.info("[PayFast] Final URL generated: {}", url);
        log.info("[PayFast] ========== END SUBSCRIPTION URL GENERATION ==========");
        return ResponseEntity.ok(url.toString());
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

        // If configured to require a single canonical signature, only accept the include_encoded variant
        if (payFastProperties.isRequireSignature()) {
            String canonical = generateSignature(paramsForValidation, true, true); // include both merchant_id and merchant_key, RFC3986 encoded
            boolean canonicalMatch = receivedSignature != null && receivedSignature.equals(canonical);
            log.info("[PayFast ITN] requireSignature=true, canonical (include_encoded) expected={}, canonicalMatch={}", canonical, canonicalMatch);
            if (!canonicalMatch) {
                log.error("[PayFast ITN] SIGNATURE MISMATCH (strict). Received: {}, Expected (canonical include_encoded): {}", receivedSignature, canonical);
                return ResponseEntity.status(400).body("Signature mismatch");
            }
        } else if (!match) {
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

    // ...existing code...

    private String generateSignatureForInitialRequest(Map<String, String> params) {
        log.info("[PayFast Signature] ========== SIGNATURE GENERATION FOR INITIAL REQUEST ==========");

        // 1. Filter: exclude empty values and signature field
        Map<String, String> filtered = params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty() && !"signature".equals(e.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));

        log.info("[PayFast Signature] Filtered params (sorted alphabetically):");
        filtered.forEach((k, v) -> log.info("[PayFast Signature]   {}={}", k, v));

        // 2. Build base string: key=value&key=value&...
        // CRITICAL: Use raw values, NOT URL-encoded
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            String value = entry.getValue();
            sb.append(entry.getKey()).append("=").append(value).append("&");
        }

        // Trim trailing '&'
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '&') {
            sb.setLength(sb.length() - 1);
        }

        // 3. Append passphrase at the end (CRITICAL - this is required for initial request)
        String passphrase = payFastProperties.getPassphrase();
        if (passphrase != null && !passphrase.isBlank()) {
            sb.append("&passphrase=").append(passphrase);
            String masked = passphrase.length() <= 4 ? "****" : passphrase.substring(0, 2) + "****" + passphrase.substring(passphrase.length() - 2);
            log.info("[PayFast Signature] Passphrase appended (masked={}, length={})", masked, passphrase.length());
        } else {
            log.warn("[PayFast Signature] No passphrase configured!");
        }

        String signatureString = sb.toString();
        log.info("[PayFast Signature] ========== BASE STRING TO HASH ==========");
        log.info("[PayFast Signature] {}", signatureString);
        log.info("[PayFast Signature] ========== LENGTH: {} ==========", signatureString.length());

        // 4. MD5 hash
        String signature = org.apache.commons.codec.digest.DigestUtils.md5Hex(signatureString);
        log.info("[PayFast Signature] Generated MD5 signature (WITH merchant_key): {}", signature);

        // Also test WITHOUT merchant_key (PayFast might only use transaction data for subscription)
        StringBuilder sbWithoutKey = new StringBuilder();
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            if ("merchant_key".equals(entry.getKey())) continue;  // Skip merchant_key
            String value = entry.getValue();
            sbWithoutKey.append(entry.getKey()).append("=").append(value).append("&");
        }
        if (!sbWithoutKey.isEmpty() && sbWithoutKey.charAt(sbWithoutKey.length() - 1) == '&') {
            sbWithoutKey.setLength(sbWithoutKey.length() - 1);
        }
        sbWithoutKey.append("&passphrase=").append(passphrase);
        String signatureWithoutKey = org.apache.commons.codec.digest.DigestUtils.md5Hex(sbWithoutKey.toString());
        log.info("[PayFast Signature] Generated MD5 signature (WITHOUT merchant_key): {}", signatureWithoutKey);
        log.info("[PayFast Signature] Base string without key: {}", sbWithoutKey.toString());

        log.info("[PayFast Signature] ========== SIGNATURE GENERATION END ==========");

        // Return the signature WITH merchant_key (testing different approach)
        return signature;
    }

    // ...existing code...
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

    // Generate signature excluding BOTH merchant_id and merchant_key (only transaction data)
    private String generateSignatureExcludingMerchantData(Map<String, String> params, boolean urlEncodeValues) {
        log.info("[PayFast Signature] ========== SIGNATURE GENERATION START (exclude_both, urlEncodeValues={}) ==========", urlEncodeValues);

        Map<String, String> filtered = params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty() && !"signature".equals(e.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));

        // Exclude both merchant_id and merchant_key
        filtered.remove("merchant_id");
        filtered.remove("merchant_key");
        log.debug("[PayFast Signature] Removed merchant_id and merchant_key from signature");

        log.info("[PayFast Signature] Filtered params (sorted, no merchant data): {}", filtered);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            String value = entry.getValue();
            String toAppend = urlEncodeValues ? rfc3986Encode(value) : value;
            sb.append(entry.getKey()).append("=").append(toAppend).append("&");
            log.debug("[PayFast Signature] Adding param: {}={}", entry.getKey(), toAppend);
        }

        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '&') {
            sb.setLength(sb.length() - 1);
        }

        String passphrase = payFastProperties.getPassphrase();
        if (passphrase != null && !passphrase.isBlank()) {
            sb.append("&passphrase=").append(passphrase);
            String masked = passphrase.length() <= 4 ? "****" : passphrase.substring(0, 2) + "****" + passphrase.substring(passphrase.length() - 2);
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


    // Variant: Include standard PayFast params only (no custom_str fields) in signature
    private Map<String, Object> buildVariantStandardParamsOnly(Map<String, String> params, boolean urlEncodeValues) {
        Map<String, Object> result = new java.util.HashMap<>();

        // Filter to include ONLY standard PayFast parameters (exclude custom_str* fields)
        Map<String, String> filtered = params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty() && !"signature".equals(e.getKey()))
            .filter(e -> !e.getKey().startsWith("custom_")) // Exclude custom fields
            .sorted(Map.Entry.comparingByKey())
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));

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
        if (passphrase != null) {
            result.put("passphrase_masked", passphrase.length() <= 4 ? "****" : passphrase.substring(0, 2) + "****" + passphrase.substring(Math.max(2, passphrase.length() - 2)));
            result.put("passphrase_sha256", org.apache.commons.codec.digest.DigestUtils.sha256Hex(passphrase));
            result.put("passphrase_length", passphrase.length());
        }
        return result;
    }

    // Variant: Include standard PayFast params only (no custom_str fields) in signature, NO passphrase
    private Map<String, Object> buildVariantStandardParamsOnlyNoPassphrase(Map<String, String> params, boolean urlEncodeValues) {
        Map<String, Object> result = new java.util.HashMap<>();

        // Filter to include ONLY standard PayFast parameters (exclude custom_str* fields)
        Map<String, String> filtered = params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty() && !"signature".equals(e.getKey()))
            .filter(e -> !e.getKey().startsWith("custom_")) // Exclude custom fields
            .sorted(Map.Entry.comparingByKey())
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            String toAppend = urlEncodeValues ? rfc3986Encode(entry.getValue()) : entry.getValue();
            sb.append(entry.getKey()).append("=").append(toAppend).append("&");
        }
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '&') sb.setLength(sb.length() - 1);

        // NO passphrase appended
        String baseString = sb.toString();
        String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(baseString);

        result.put("baseString", baseString);
        result.put("signature", md5);
        return result;
    }

    // Variant: spaces encoded as + (URLEncoder default) instead of %20
    private Map<String, Object> buildVariantExcludingMerchantDataPlus(Map<String, String> params, boolean urlEncodeValues) {
        Map<String, Object> result = new java.util.HashMap<>();
        Map<String, String> filtered = params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty() && !"signature".equals(e.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));
        filtered.remove("merchant_id");
        filtered.remove("merchant_key");

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            String toAppend = entry.getValue();
            // use URLEncoder default (spaces -> +)
            if (urlEncodeValues) toAppend = URLEncoder.encode(toAppend, StandardCharsets.UTF_8);
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
        if (passphrase != null) {
            result.put("passphrase_masked", passphrase.length() <= 4 ? "****" : passphrase.substring(0, 2) + "****" + passphrase.substring(Math.max(2, passphrase.length() - 2)));
            result.put("passphrase_sha256", org.apache.commons.codec.digest.DigestUtils.sha256Hex(passphrase));
            result.put("passphrase_length", passphrase.length());
        }
        return result;
    }

    // Variant: encode values as RFC3986 but ALSO URL-encode the passphrase before appending
    private Map<String, Object> buildVariantExcludingMerchantDataEncodedPassphrase(Map<String, String> params, boolean urlEncodeValues) {
        Map<String, Object> result = new java.util.HashMap<>();
        Map<String, String> filtered = params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty() && !"signature".equals(e.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));
        filtered.remove("merchant_id");
        filtered.remove("merchant_key");

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            String toAppend = urlEncodeValues ? rfc3986Encode(entry.getValue()) : entry.getValue();
            sb.append(entry.getKey()).append("=").append(toAppend).append("&");
        }
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '&') sb.setLength(sb.length() - 1);

        String passphrase = payFastProperties.getPassphrase();
        if (passphrase != null && !passphrase.isBlank()) {
            sb.append("&passphrase=").append(URLEncoder.encode(passphrase, StandardCharsets.UTF_8));
        }

        String baseString = sb.toString();
        String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(baseString);

        result.put("baseString", baseString);
        result.put("signature", md5);
        if (passphrase != null) {
            result.put("passphrase_masked", passphrase.length() <= 4 ? "****" : passphrase.substring(0, 2) + "****" + passphrase.substring(Math.max(2, passphrase.length() - 2)));
            result.put("passphrase_sha256", org.apache.commons.codec.digest.DigestUtils.sha256Hex(passphrase));
            result.put("passphrase_length", passphrase.length());
        }
        return result;
    }

    // Variant: exclude both but do not append passphrase at all
    private Map<String, Object> buildVariantExcludingMerchantDataNoPassphrase(Map<String, String> params, boolean urlEncodeValues) {
        Map<String, Object> result = new java.util.HashMap<>();
        Map<String, String> filtered = params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty() && !"signature".equals(e.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));
        filtered.remove("merchant_id");
        filtered.remove("merchant_key");

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            String toAppend = urlEncodeValues ? rfc3986Encode(entry.getValue()) : entry.getValue();
            sb.append(entry.getKey()).append("=").append(toAppend).append("&");
        }
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '&') sb.setLength(sb.length() - 1);

        String baseString = sb.toString();
        String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(baseString);

        result.put("baseString", baseString);
        result.put("signature", md5);
        return result;
    }

    // Variant: include merchant_id and merchant_key but NO passphrase
    private Map<String, Object> buildVariantIncludeNoPassphrase(Map<String, String> params, boolean urlEncodeValues) {
        Map<String, Object> result = new java.util.HashMap<>();
        Map<String, String> filtered = params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty() && !"signature".equals(e.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));
        // Keep both merchant_id and merchant_key

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            String toAppend = urlEncodeValues ? rfc3986Encode(entry.getValue()) : entry.getValue();
            sb.append(entry.getKey()).append("=").append(toAppend).append("&");
        }
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '&') sb.setLength(sb.length() - 1);

        // NO passphrase appended
        String baseString = sb.toString();
        String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(baseString);

        result.put("baseString", baseString);
        result.put("signature", md5);
        return result;
    }

    private String generateSignatureFromAllParams(Map<String, String> params) {
        log.info("[PayFast Signature] Generating signature from ALL URL params (including custom fields)");
        log.info("[PayFast Signature] Params for signature: {}", params);

        // Filter and sort
        Map<String, String> filtered = params.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty() && !"signature".equals(e.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));

        // Build base string
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '&') {
            sb.setLength(sb.length() - 1);
        }

        // Append passphrase
        String passphrase = payFastProperties.getPassphrase();
        if (passphrase != null && !passphrase.isBlank()) {
            sb.append("&passphrase=").append(passphrase);
        }

        String baseString = sb.toString();
        log.info("[PayFast Signature] Base string (with ALL params): {}", baseString);

        String signature = org.apache.commons.codec.digest.DigestUtils.md5Hex(baseString);
        log.info("[PayFast Signature] Generated signature (from ALL params): {}", signature);

        return signature;
    }

    // Helper to sanitize incoming request params (trim, remove quotes, and decode URL encoding)
    private String sanitizeAndDecode(String s) {
        if (s == null) return null;
        try {
            // First decode URL encoding (%20 -> space, %40 -> @, etc.)
            String decoded = java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);
            // Then sanitize (trim and remove quotes)
            return sanitizeParam(decoded);
        } catch (Exception e) {
            log.warn("[PayFast] Error decoding parameter: {}", s, e);
            return sanitizeParam(s);
        }
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

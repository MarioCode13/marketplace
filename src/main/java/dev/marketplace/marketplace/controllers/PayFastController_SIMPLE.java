package dev.marketplace.marketplace.controllers;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
public class PayFastController_SIMPLE {

    private static final Logger log = LoggerFactory.getLogger(PayFastController_SIMPLE.class);

    private final PayFastProperties payFastProperties;

    @Autowired
    private UserService userService;

    @Autowired
    private SubscriptionService subscriptionService;

    public PayFastController_SIMPLE(PayFastProperties payFastProperties) {
        this.payFastProperties = payFastProperties;
    }

    // Changed return type to JSON (Map) so frontend can call res.json() reliably
    @GetMapping("/subscription-url")
    public ResponseEntity<Map<String, String>> getPayFastSubscriptionUrl(
            @RequestParam(required = false) String nameFirst,
            @RequestParam(required = false) String nameLast,
            @RequestParam(required = false) String emailAddress,
            @RequestParam(defaultValue = "Pro Store Subscription") String itemName,
            @RequestParam(defaultValue = "49.00") String amount,
            @RequestParam(defaultValue = "3") String frequency,
            @RequestParam(defaultValue = "12") String cycles,
            @RequestParam(defaultValue = "verified_user") String planType
    ) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

        Optional<User> userOpt = userService.getUserByEmail(auth.getName());
        if (userOpt.isEmpty())
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));

        User user = userOpt.get();

        if (nameFirst == null || nameFirst.isBlank())
            nameFirst = Optional.ofNullable(user.getFirstName()).orElse("User");
        if (nameLast == null || nameLast.isBlank())
            nameLast = Optional.ofNullable(user.getLastName()).orElse("Account");
        if (emailAddress == null || emailAddress.isBlank())
            emailAddress = user.getEmail();

        // EXACT documentation order
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("merchant_id", payFastProperties.getMerchantId());
        params.put("merchant_key", payFastProperties.getMerchantKey());
        params.put("return_url", payFastProperties.getReturnUrl());
        params.put("cancel_url", payFastProperties.getCancelUrl());
        params.put("notify_url", payFastProperties.getNotifyUrl());
        params.put("name_first", nameFirst);
        params.put("name_last", nameLast);
        params.put("email_address", emailAddress);
        params.put("amount", amount);
        params.put("item_name", itemName);
        params.put("custom_str1", planType);
        params.put("subscription_type", "1");
        params.put("frequency", frequency);
        params.put("cycles", cycles);

        String signature = generateSignature(params);

        StringBuilder url = new StringBuilder(payFastProperties.getUrl()).append("?");

        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) url.append("&");
            first = false;
            url.append(entry.getKey())
                    .append("=")
                    .append(encode(entry.getValue()));
        }

        url.append("&signature=").append(signature);

        log.info("[PayFast] Signature base string used: {}", buildBaseString(params));
        log.info("[PayFast] Signature generated: {}", signature);

        // Return JSON with url field so FE can parse without error
        return ResponseEntity.ok(Map.of("url", url.toString()));
    }

    private String generateSignature(LinkedHashMap<String, String> params) {
        String baseString = buildBaseString(params);
        return DigestUtils.md5Hex(baseString);
    }

    private String buildBaseString(LinkedHashMap<String, String> params) {

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, String> entry : params.entrySet()) {

            if (!first) sb.append("&");
            first = false;

            sb.append(entry.getKey())
                    .append("=")
                    .append(encode(entry.getValue()));
        }

        return sb.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @PostMapping("/itn")
    public ResponseEntity<String> handlePayFastITN(@RequestParam Map<String, String> payload) {

        String receivedSignature = payload.get("signature");

        // Alphabetical order for ITN
        SortedMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            if (!entry.getKey().equals("signature")) {
                sorted.put(entry.getKey(), entry.getValue());
            }
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (!first) sb.append("&");
            first = false;

            sb.append(entry.getKey())
                    .append("=")
                    .append(encode(entry.getValue()));
        }

        String expectedSignature = DigestUtils.md5Hex(sb.toString());

        if (!Objects.equals(receivedSignature, expectedSignature)) {
            log.error("[PayFast ITN] Signature mismatch");
            return ResponseEntity.status(400).body("Signature mismatch");
        }

        log.info("[PayFast ITN] ========== RECEIVED ITN CALLBACK ==========");
        log.info("[PayFast ITN] Signature validation passed!");

        String email = payload.get("custom_str2");
        String paymentStatus = payload.get("payment_status");
        String planTypeStr = payload.get("custom_str1");
        String paymentType = payload.get("payment_type");           // NEW: Detect initial vs renewal
        String amountStr = payload.get("amount");                   // NEW: Get amount for renewal
        log.info("[PayFast ITN] Parsed values: email={}, paymentStatus={}, planTypeStr={}, paymentType={}",
                 email, paymentStatus, planTypeStr, paymentType);

        if (email != null && !email.isEmpty() && "COMPLETE".equalsIgnoreCase(paymentStatus) && planTypeStr != null) {
            var userOpt = userService.getUserByEmail(email);
            if (userOpt.isPresent()) {
                var user = userOpt.get();
                log.info("[PayFast ITN] Found user: id={}, email={}", user.getId(), user.getEmail());
                try {
                    var planType = Subscription.PlanType.valueOf(planTypeStr.toUpperCase());
                    log.info("[PayFast ITN] Parsed planType enum: {}", planType);

                    // NEW: Detect if this is initial or renewal payment
                    if ("initial".equals(paymentType) || paymentType == null) {
                        log.info("[PayFast ITN] Processing initial subscription payment");
                        subscriptionService.createOrActivatePayFastSubscription(user.getId(), planType);
                        log.info("[PayFast ITN] Subscription activated for user {} with plan {}", email, planType);
                    }
                    else if ("recurring".equals(paymentType) || "subscription".equals(paymentType)) {
                        log.info("[PayFast ITN] Processing recurring subscription renewal payment");
                        var amount = new java.math.BigDecimal(amountStr != null ? amountStr : "0");
                        subscriptionService.renewPayFastSubscription(user.getId(), planType, amount);
                        log.info("[PayFast ITN] Subscription renewed for user {} with plan {}", email, planType);
                    }
                    else {
                        log.warn("[PayFast ITN] Unknown payment type: {}", paymentType);
                    }
                } catch (IllegalArgumentException e) {
                    log.error("[PayFast ITN] Invalid plan type: {}", planTypeStr, e);
                } catch (Exception e) {
                    log.error("[PayFast ITN] Exception during payment processing for user {}: {}", email, e.getMessage(), e);
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
}

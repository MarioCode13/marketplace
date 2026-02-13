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
    private boolean payfastConfigured = true;

    @Autowired
    private UserService userService;

    @Autowired
    private SubscriptionService subscriptionService;

    public PayFastController_SIMPLE(PayFastProperties payFastProperties) {
        this.payFastProperties = payFastProperties;
    }

    @PostConstruct
    public void validatePayFastConfig() {
        StringBuilder missing = new StringBuilder();

        if (payFastProperties.getMerchantId() == null || payFastProperties.getMerchantId().isBlank())
            missing.append("merchantId ");
        if (payFastProperties.getMerchantKey() == null || payFastProperties.getMerchantKey().isBlank())
            missing.append("merchantKey ");
        if (payFastProperties.getUrl() == null || payFastProperties.getUrl().isBlank())
            missing.append("payfastUrl ");
        if (payFastProperties.getPassphrase() == null || payFastProperties.getPassphrase().isBlank())
            missing.append("passphrase ");

        if (!missing.isEmpty()) {
            log.error("[PayFast] Missing config: {}. Disabled.", missing.toString().trim());
            payfastConfigured = false;
        } else {
            log.info("[PayFast] Enabled (merchant={})", payFastProperties.getMerchantId());
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
            @RequestParam(defaultValue = "pro_store") String planType
    ) {

        if (!payfastConfigured)
            return ResponseEntity.status(503).body("PayFast not configured");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(401).body("Not authenticated");

        Optional<User> userOpt = userService.getUserByEmail(auth.getName());
        if (userOpt.isEmpty())
            return ResponseEntity.status(404).body("User not found");

        User user = userOpt.get();

        if (nameFirst == null || nameFirst.isBlank())
            nameFirst = Optional.ofNullable(user.getFirstName()).orElse("User");
        if (nameLast == null || nameLast.isBlank())
            nameLast = Optional.ofNullable(user.getLastName()).orElse("Account");
        if (emailAddress == null || emailAddress.isBlank())
            emailAddress = user.getEmail();

        log.info("[PayFast] Building URL for {} {} ({})", nameFirst, nameLast, emailAddress);

        Map<String, String> params = new LinkedHashMap<>();

        // EXACT documentation order
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
        params.put("custom_str2", emailAddress);
        params.put("subscription_type", "1");
        params.put("recurring_amount", recurringAmount);
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

        log.info("[PayFast] Signature: {}", signature);

        return ResponseEntity.ok(url.toString());
    }

    private String generateSignature(Map<String, String> params) {

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, String> entry : params.entrySet()) {

            String value = entry.getValue();
            if (value == null || value.isBlank()) continue;

            if (!first) sb.append("&");
            first = false;

            sb.append(entry.getKey())
                    .append("=")
                    .append(encode(value));
        }

        sb.append("&passphrase=").append(encode(payFastProperties.getPassphrase()));

        String baseString = sb.toString();

        log.info("[PayFast] Signature base string: {}", baseString);

        return DigestUtils.md5Hex(baseString);
    }

    private String computeITNSignature(Map<String, String> payload) {

        SortedMap<String, String> sorted = new TreeMap<>();

        for (Map.Entry<String, String> entry : payload.entrySet()) {
            if (!entry.getKey().equals("signature")
                    && entry.getValue() != null
                    && !entry.getValue().isBlank()) {
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

        sb.append("&passphrase=").append(encode(payFastProperties.getPassphrase()));

        return DigestUtils.md5Hex(sb.toString());
    }

    private String encode(String s) {
        if (s == null) return "";
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @PostMapping("/itn")
    public ResponseEntity<String> handlePayFastITN(@RequestParam Map<String, String> payload) {

        String receivedSignature = payload.get("signature");
        String expectedSignature = computeITNSignature(payload);

        if (receivedSignature == null || !receivedSignature.equals(expectedSignature)) {
            log.error("[PayFast ITN] Signature mismatch!");
            return ResponseEntity.status(400).body("Signature mismatch");
        }

        String email = payload.get("custom_str2");
        String status = payload.get("payment_status");
        String plan = payload.get("custom_str1");

        if ("COMPLETE".equalsIgnoreCase(status) && email != null && plan != null) {
            Optional<User> userOpt = userService.getUserByEmail(email);
            userOpt.ifPresent(user ->
                    subscriptionService.createOrActivatePayFastSubscription(
                            user.getId(),
                            Subscription.PlanType.valueOf(plan.toUpperCase())
                    )
            );
        }

        return ResponseEntity.ok("OK");
    }
}

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

        Map<String, String> params = new HashMap<>();

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

        String signature = computeSignature(params, true);

        StringBuilder url = new StringBuilder(payFastProperties.getUrl()).append("?");
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        boolean first = true;
        for (String key : keys) {
            if (!first) url.append("&");
            first = false;
            url.append(key).append("=").append(encode(params.get(key)));
        }

        url.append("&signature=").append(signature);

        log.info("[PayFast] Redirect signature: {}", signature);

        return ResponseEntity.ok(url.toString());
    }

    private String computeSignature(Map<String, String> params, boolean includeMerchantKey) {

        SortedMap<String, String> sorted = new TreeMap<>();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value == null || value.isBlank())
                continue;

            if (!includeMerchantKey && key.equals("merchant_key"))
                continue;

            if (key.equals("signature"))
                continue;

            sorted.put(key, value);
        }

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> entry : sorted.entrySet()) {

            String encodedValue = encode(entry.getValue());

            sb.append(entry.getKey())
                    .append("=")
                    .append(encodedValue)
                    .append("&");
        }

        sb.append("passphrase=").append("dealioMarket1");

        String baseString = sb.toString();

        log.info("[PayFast] Signature base string (ENCODED): {}", baseString);

        return DigestUtils.md5Hex(baseString);
    }

    private String encode(String s) {
        if (s == null) return "";
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }

    @PostMapping("/itn")
    public ResponseEntity<String> handlePayFastITN(@RequestParam Map<String, String> payload) {

        if (!payfastConfigured) {
            log.warn("[PayFast ITN] Ignored (not configured)");
            return ResponseEntity.ok("OK");
        }

        log.info("[PayFast ITN] Received payload");

        String receivedSignature = payload.get("signature");
        String expectedSignature = computeSignature(payload, false);

        log.info("[PayFast ITN] Received signature: {}", receivedSignature);
        log.info("[PayFast ITN] Expected signature: {}", expectedSignature);

        if (receivedSignature == null || !receivedSignature.equals(expectedSignature)) {
            log.error("[PayFast ITN] Signature mismatch!");
            return ResponseEntity.status(400).body("Signature mismatch");
        }

        log.info("[PayFast ITN] Signature OK");

        String email = payload.get("custom_str2");
        String status = payload.get("payment_status");
        String plan = payload.get("custom_str1");

        if ("COMPLETE".equalsIgnoreCase(status) && email != null && plan != null) {
            Optional<User> userOpt = userService.getUserByEmail(email);
            if (userOpt.isPresent()) {
                try {
                    subscriptionService.createOrActivatePayFastSubscription(
                            userOpt.get().getId(),
                            Subscription.PlanType.valueOf(plan.toUpperCase())
                    );
                    log.info("[PayFast ITN] Subscription activated for {}", email);
                } catch (Exception e) {
                    log.error("[PayFast ITN] Subscription activation failed", e);
                }
            } else {
                log.error("[PayFast ITN] User not found: {}", email);
            }
        }

        return ResponseEntity.ok("OK");
    }

    @GetMapping("/user/subscription-status")
    public ResponseEntity<Map<String, Boolean>> getSubscriptionStatus() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(401).build();

        UUID userId = userService.getUserIdByUsername(auth.getName());
        boolean active = userService.hasActiveSubscription(userId);

        Map<String, Boolean> resp = new HashMap<>();
        resp.put("active", active);

        return ResponseEntity.ok(resp);
    }
}

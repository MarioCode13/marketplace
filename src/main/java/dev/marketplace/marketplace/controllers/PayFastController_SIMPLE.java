package dev.marketplace.marketplace.controllers;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import dev.marketplace.marketplace.config.PayFastProperties;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.service.SubscriptionService;
import dev.marketplace.marketplace.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/payfast")
public class PayFastController_SIMPLE {

    private final PayFastProperties payFastProperties;

    @Autowired
    private UserService userService;

    @Autowired
    private SubscriptionService subscriptionService;

    public PayFastController_SIMPLE(PayFastProperties payFastProperties) {
        this.payFastProperties = payFastProperties;
    }

    @GetMapping("/subscription-url")
    public ResponseEntity<String> getPayFastSubscriptionUrl(
            @RequestParam(defaultValue = "Testers") String itemName,
            @RequestParam(defaultValue = "49.00") String amount,
            @RequestParam(defaultValue = "3") String frequency,
            @RequestParam(defaultValue = "12") String cycles
    ) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(401).body("Not authenticated");

        User user = userService.getUserByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String nameFirst = user.getFirstName();
        String nameLast = user.getLastName();
        String email = user.getEmail();

        // EXACT ORDER from support example
        LinkedHashMap<String, String> params = new LinkedHashMap<>();

        params.put("merchant_id", payFastProperties.getMerchantId());
        params.put("merchant_key", payFastProperties.getMerchantKey());
        params.put("return_url", payFastProperties.getReturnUrl());
        params.put("cancel_url", payFastProperties.getCancelUrl());
        params.put("notify_url", payFastProperties.getNotifyUrl());
        params.put("name_first", nameFirst);
        params.put("name_last", nameLast);
        params.put("email_address", email);
        params.put("amount", amount);
        params.put("item_name", itemName);
        params.put("custom_str1", "Extra order information");
        params.put("subscription_type", "1");
        params.put("frequency", frequency);
        params.put("cycles", cycles);

        String signature = generateSignature(params);

        StringBuilder url = new StringBuilder("https://www.payfast.co.za/eng/process?");
        boolean first = true;

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) url.append("&");
            first = false;

            url.append(entry.getKey())
                    .append("=")
                    .append(encode(entry.getValue()));
        }

        url.append("&signature=").append(signature);

        return ResponseEntity.ok(url.toString());
    }

    private String generateSignature(LinkedHashMap<String, String> params) {

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) sb.append("&");
            first = false;

            sb.append(entry.getKey())
                    .append("=")
                    .append(encode(entry.getValue()));
        }

        // NO PASSPHRASE (matching support example)

        return DigestUtils.md5Hex(sb.toString());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @PostMapping("/itn")
    public ResponseEntity<String> handlePayFastITN(@RequestParam Map<String, String> payload) {

        String receivedSignature = payload.get("signature");

        // Alphabetical order for ITN (standard PayFast behaviour)
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
            return ResponseEntity.status(400).body("Signature mismatch");
        }

        return ResponseEntity.ok("OK");
    }
}

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

    @GetMapping("/subscription-url")
    public ResponseEntity<String> getPayFastSubscriptionUrl(
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

        String signature = generateSignatureExact();

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

        return ResponseEntity.ok(url.toString());
    }

//    TEST
private String generateSignatureExact() {

    String baseString =
            "merchant_id=12247699" +
                    "&merchant_key=fdsyodv8kudog" +
                    "&return_url=https%3A%2F%2Fwww.dealio.org.za%2Fsubscriptions%2Fsuccess" +
                    "&cancel_url=https%3A%2F%2Fwww.dealio.org.za%2Fsubscriptions%2Fcancel" +
                    "&notify_url=https%3A%2F%2Fapi.dealio.org.za%2Fapi%2Fpayments%2Fpayfast%2Fitn" +
                    "&name_first=Jimi" +
                    "&name_last=Hendrix" +
                    "&email_address=jimi%40gmail.com" +
                    "&amount=49.00" +
                    "&item_name=Testers" +
                    "&custom_str1=Extra+order+information" +
                    "&subscription_type=1" +
                    "&frequency=3" +
                    "&cycles=12";

    return DigestUtils.md5Hex(baseString);
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

        return ResponseEntity.ok("OK");
    }
}

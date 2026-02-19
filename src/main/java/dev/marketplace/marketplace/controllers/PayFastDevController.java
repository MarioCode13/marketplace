package dev.marketplace.marketplace.controllers;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;

import dev.marketplace.marketplace.service.UserService;
import dev.marketplace.marketplace.service.SubscriptionService;
import dev.marketplace.marketplace.model.Subscription;
import dev.marketplace.marketplace.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import jakarta.servlet.http.HttpServletRequest;

/**
 * LOCAL DEV ONLY: Direct subscription activation without payment
 *
 * This controller is ONLY enabled when payfast.enabled=false (dev mode)
 * Provides /subscription-activate endpoint that immediately activates subscriptions
 * without requiring PayFast payment gateway.
 */
@ConditionalOnProperty(prefix = "payfast", name = "enabled", havingValue = "false", matchIfMissing = true)
@RestController
@RequestMapping("/api/payments/payfast")
public class PayFastDevController {

    private static final Logger log = LoggerFactory.getLogger(PayFastDevController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private SubscriptionService subscriptionService;

    /**
     * LOCAL DEV ONLY: Direct subscription activation without payment
     *
     * When payfast.enabled=false, this endpoint activates a subscription immediately
     * without requiring PayFast payment gateway. Perfect for local testing.
     *
     * @param planType Plan type (SELLER_PLUS, RESELLER, PRO_STORE)
     * @return JSON with subscription details and success status
     */
    @PostMapping("/subscription-activate")
    public ResponseEntity<?> activateSubscriptionLocal(
            @RequestParam(required = false) String planType,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Not authenticated"
            ));
        }

        try {
            Optional<User> userOpt = userService.getUserByEmail(auth.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }

            User user = userOpt.get();
            log.info("[DEV MODE] User found: {}", user.getId());

            // Log incoming request details for debugging
            String q = request.getQueryString();
            String ct = request.getContentType();
            log.info("[DEV MODE] Incoming request contentType={}, query={} bodyPresent={}", ct, q, body != null);

            // 1) prefer explicit query param
            if (planType == null || planType.isBlank()) {
                // 2) try form or query parameters from the servlet request
                String pFromRequest = request.getParameter("planType");
                if (pFromRequest != null && !pFromRequest.isBlank()) {
                    planType = pFromRequest;
                    log.info("[DEV MODE] Extracted planType from servlet request parameter: {}", planType);
                }
            }

            // 3) if still null, try JSON body
            if ((planType == null || planType.isBlank()) && body != null && body.containsKey("planType")) {
                Object pt = body.get("planType");
                if (pt != null) planType = pt.toString();
                log.info("[DEV MODE] Extracted planType from request body: {}", planType);
            } else if (body != null && !body.isEmpty()) {
                log.info("[DEV MODE] Request body present but no planType key: {}", body.toString());
            }

            if (planType == null || planType.isBlank()) {
                planType = "verified_user"; // default
                log.info("[DEV MODE] planType not provided, using default: {}", planType);
            }

            // Map frontend plan type names to backend enum values
            String mappedPlanType = mapPlanType(planType);
            log.info("[DEV MODE] Mapped planType {} to {}", planType, mappedPlanType);

            Subscription.PlanType plan = Subscription.PlanType.valueOf(mappedPlanType.toUpperCase());
            log.info("[DEV MODE] Activating subscription for user {} to plan {}", user.getId(), plan);

            // Directly activate subscription (same as PayFast ITN callback would do)
            // In dev we force-set the plan (create or upgrade) and send confirmation email
            subscriptionService.forceSetSubscriptionPlan(user.getId(), plan);
            log.info("[DEV MODE] Subscription service called successfully");

            // Get the created subscription
            Optional<Subscription> sub = subscriptionService.getActiveSubscription(user.getId());
            log.info("[DEV MODE] Active subscription lookup result: {}", sub.isPresent());

            if (sub.isPresent()) {
                Subscription subscription = sub.get();
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Subscription activated successfully");
                response.put("userId", user.getId().toString());
                response.put("email", user.getEmail());
                response.put("planType", subscription.getPlanType().getDisplayName());
                response.put("status", "ACTIVE");
                response.put("mode", "DEV_LOCAL");
                response.put("subscriptionId", subscription.getId().toString());
                response.put("currentPeriodEnd", subscription.getCurrentPeriodEnd().toString());

                log.info("[DEV MODE] Subscription activated for user {} without payment", user.getId());
                return ResponseEntity.ok(response);
            } else {
                log.error("[DEV MODE] Failed to retrieve subscription after activation");
                return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Subscription activation failed - could not retrieve subscription"
                ));
            }

        } catch (IllegalArgumentException e) {
            log.error("[DEV MODE] Invalid plan type: {} - {}", planType, e.getMessage(), e);
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "message", "Invalid plan type. Accepted values: user_plus, reseller, pro_store"
            ));
        } catch (Exception e) {
            log.error("[DEV MODE] Error activating subscription - {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * DEV: Force-send subscription confirmation email for the authenticated user.
     * Useful to test email delivery when subscription already exists or when debugging.
     * @param planType frontend plan name (user_plus, reseller, pro_store)
     */
    @PostMapping("/subscription-send-email")
    public ResponseEntity<?> sendSubscriptionEmailLocal(
            @RequestParam(defaultValue = "verified_user") String planType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Not authenticated"
            ));
        }

        try {
            Optional<User> userOpt = userService.getUserByEmail(auth.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }
            User user = userOpt.get();
            String mappedPlanType = mapPlanType(planType);
            Subscription.PlanType plan = Subscription.PlanType.valueOf(mappedPlanType.toUpperCase());

            log.info("[DEV MODE] Forcing subscription email for user {} plan {}", user.getId(), plan);
            subscriptionService.sendSubscriptionConfirmationEmailForUser(user.getId(), plan);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Email send initiated"
            ));
        } catch (IllegalArgumentException e) {
            log.error("[DEV MODE] Invalid plan type for forced email: {} - {}", planType, e.getMessage(), e);
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "message", "Invalid plan type"
            ));
        } catch (Exception e) {
            log.error("[DEV MODE] Error forcing subscription email - {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * DEV: Force set subscription plan (create or upgrade) and send confirmation email.
     * Use this when you need to guarantee a plan change during local testing.
     */
    @PostMapping("/subscription-force-set")
    public ResponseEntity<?> forceSetSubscriptionPlan(
            @RequestParam(required = false) String planType,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Not authenticated"
            ));
        }

        try {
            Optional<User> userOpt = userService.getUserByEmail(auth.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }

            User user = userOpt.get();
            // extract planType similarly to activate
            String q = request.getQueryString();
            String ct = request.getContentType();
            log.info("[DEV MODE] ForceSet Incoming request contentType={}, query={} bodyPresent={}", ct, q, body != null);

            if (planType == null || planType.isBlank()) {
                String pFromRequest = request.getParameter("planType");
                if (pFromRequest != null && !pFromRequest.isBlank()) planType = pFromRequest;
            }
            if ((planType == null || planType.isBlank()) && body != null && body.containsKey("planType")) {
                Object pt = body.get("planType"); if (pt != null) planType = pt.toString();
            }
            if (planType == null || planType.isBlank()) planType = "verified_user";

            String mappedPlanType = mapPlanType(planType);
            Subscription.PlanType plan = Subscription.PlanType.valueOf(mappedPlanType.toUpperCase());

            log.info("[DEV MODE] Force-setting subscription for user {} to plan {}", user.getId(), plan);
            Subscription subscription = subscriptionService.forceSetSubscriptionPlan(user.getId(), plan);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Force subscription set successfully");
            response.put("subscriptionId", subscription.getId().toString());
            response.put("planType", subscription.getPlanType().getDisplayName());
            response.put("currentPeriodEnd", subscription.getCurrentPeriodEnd().toString());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("[DEV MODE] Invalid plan type for force set: {} - {}", planType, e.getMessage(), e);
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "message", "Invalid plan type"
            ));
        } catch (Exception e) {
            log.error("[DEV MODE] Error in force set subscription - {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Map frontend plan type names to backend enum values
     * Frontend uses: user_plus, reseller, pro_store
     * Backend enum: SELLER_PLUS, RESELLER, PRO_STORE
     */
    private String mapPlanType(String frontendPlanType) {
        if (frontendPlanType == null) {
            return "SELLER_PLUS";
        }

        return switch (frontendPlanType.toLowerCase()) {
            case "user_plus", "seller_plus" -> "SELLER_PLUS";
            case "reseller" -> "RESELLER";
            case "pro_store", "pro-store" -> "PRO_STORE";
            case "verified_user" -> "SELLER_PLUS"; // default
            default -> frontendPlanType; // pass through, will fail with proper error
        };
    }
}

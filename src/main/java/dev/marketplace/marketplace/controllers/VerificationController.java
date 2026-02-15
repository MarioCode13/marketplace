package dev.marketplace.marketplace.controllers;

import dev.marketplace.marketplace.service.OmnicheckService;
import dev.marketplace.marketplace.service.TrustRatingService;
import dev.marketplace.marketplace.service.SubscriptionService;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@RestController
@RequestMapping("/api/id-verify")
public class VerificationController {
    @Autowired
    private OmnicheckService omnicheckService;

    @Autowired
    private TrustRatingService trustRatingService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Payload: { idNumber, firstName, lastName, userId, requestId? }
     * requestId is optional but recommended to ensure idempotency for the account.
     *
     * NOTE: User must have an active SELLER_PLUS, RESELLER, or PRO_STORE subscription
     * to perform ID verification. FREE plan users are not allowed as verification incurs a fee.
     */
    @PostMapping
    public ResponseEntity<?> verifyId(@RequestBody Map<String, String> payload) {
        String idNumber = payload.get("idNumber");
        String firstName = payload.get("firstName");
        String lastName = payload.get("lastName");
        String userIdStr = payload.get("userId");
        String requestId = payload.get("requestId");
        if (idNumber == null || firstName == null || lastName == null || userIdStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
        }
        
        try {
            UUID userId = UUID.fromString(userIdStr);

            // Check if user exists
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            // Check if user has active subscription (not on FREE plan)
            if (!subscriptionService.hasActiveSubscription(userId)) {
                return ResponseEntity.status(403).body(Map.of(
                    "error", "ID verification requires an active subscription",
                    "reason", "Users on the FREE plan cannot perform ID verification as it incurs a fee. Please upgrade to SELLER_PLUS or higher."
                ));
            }

            // Proceed with verification
            OmnicheckService.VerificationResult result;
            boolean verified = false;

            // Use account-aware call: pass userId as accountId to enforce token checks/idempotency
            result = omnicheckService.verifySouthAfricanIdForAccount(userId.toString(), requestId, idNumber, firstName, lastName);

            if (result.isSuccess()) {
                // Mark verified via TrustRatingService which will create/update the TrustRating and recalculate
                trustRatingService.markVerifiedID(userId);
                verified = true;
            }

            return ResponseEntity.ok(Map.of(
                "success", result.isSuccess(),
                "omnicheckResult", result.getRawResponse() != null ? result.getRawResponse() : "",
                "verifiedID", verified,
                "error", result.getError() != null ? result.getError() : ""
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid userId format: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal error: " + e.getMessage()));
        }
    }
}

package dev.marketplace.marketplace.controllers;

import dev.marketplace.marketplace.service.OmnicheckService;
import dev.marketplace.marketplace.service.TrustRatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/id-verify")
public class VerificationController {
    @Autowired
    private OmnicheckService omnicheckService;

    @Autowired
    private TrustRatingService trustRatingService;

    /**
     * Payload: { idNumber, firstName, lastName, userId, requestId? }
     * requestId is optional but recommended to ensure idempotency for the account.
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
        
        OmnicheckService.VerificationResult result;
        boolean verified = false;
        try {
            java.util.UUID userId = java.util.UUID.fromString(userIdStr);
            // Use account-aware call: pass userId as accountId to enforce token checks/idempotency
            result = omnicheckService.verifySouthAfricanIdForAccount(userId.toString(), requestId, idNumber, firstName, lastName);

            if (result.isSuccess()) {
                // Mark verified via TrustRatingService which will create/update the TrustRating and recalculate
                trustRatingService.markVerifiedID(userId);
                verified = true;
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid userId format: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal error: " + e.getMessage()));
        }
        
        return ResponseEntity.ok(Map.of(
            "success", result.isSuccess(),
            "omnicheckResult", result.getRawResponse() != null ? result.getRawResponse() : "",
            "verifiedID", verified,
            "error", result.getError() != null ? result.getError() : ""
        ));
    }
}

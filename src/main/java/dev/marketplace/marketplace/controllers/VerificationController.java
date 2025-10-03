package dev.marketplace.marketplace.controllers;

import dev.marketplace.marketplace.model.TrustRating;
import dev.marketplace.marketplace.service.OmnicheckService;
import org.springframework.beans.factory.annotation.Autowired;
import dev.marketplace.marketplace.repository.TrustRatingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/id-verify")
public class VerificationController {
    @Autowired
    private OmnicheckService omnicheckService;

    @Autowired
    private TrustRatingRepository trustRatingRepository;

    @PostMapping
    public ResponseEntity<?> verifyId(@RequestBody Map<String, String> payload) {
        String idNumber = payload.get("idNumber");
        String firstName = payload.get("firstName");
        String lastName = payload.get("lastName");
        String userIdStr = payload.get("userId");
        if (idNumber == null || firstName == null || lastName == null || userIdStr == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }
        String result = omnicheckService.verifySouthAfricanId(idNumber, firstName, lastName);
        boolean verified = false;
        if (result != null && result.toLowerCase().contains("success")) {
            try {
                java.util.UUID userId = java.util.UUID.fromString(userIdStr);
                var trustOpt = trustRatingRepository.findByUserId(userId);
                if (trustOpt.isPresent()) {
                    TrustRating trustRating = trustOpt.get();
                    trustRating.setVerifiedID(true);
                    trustRatingRepository.save(trustRating);
                    verified = true;
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Invalid userId format");
            }
        }
        return ResponseEntity.ok(Map.of(
            "omnicheckResult", result,
            "verifiedID", verified
        ));
    }
}

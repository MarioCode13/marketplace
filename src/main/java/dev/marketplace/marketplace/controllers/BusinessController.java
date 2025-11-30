package dev.marketplace.marketplace.controllers;

import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.BusinessTrustRating;
import dev.marketplace.marketplace.repository.BusinessRepository;
import dev.marketplace.marketplace.repository.BusinessTrustRatingRepository;
import dev.marketplace.marketplace.service.OmnicheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BusinessController {
    private final BusinessRepository businessRepository;
    private final BusinessTrustRatingRepository businessTrustRatingRepository;
    private final OmnicheckService omnicheckService;

    @GetMapping("/business/verify-email")
    public ResponseEntity<String> verifyBusinessEmail(@RequestParam("token") String token) {
        Optional<Business> businessOpt = businessRepository.findAll().stream()
            .filter(b -> token.equals(b.getEmailVerificationToken()))
            .findFirst();
        if (businessOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid or expired verification token.");
        }
        Business business = businessOpt.get();
        business.setEmailVerified(true);
        business.setEmailVerificationToken(null);
        businessRepository.save(business);
        return ResponseEntity.ok("Business email verified successfully.");
    }

    @PostMapping("/api/verify-business")
    public ResponseEntity<?> verifyBusiness(@RequestBody Map<String, String> payload) {
        String businessIdStr = payload.get("businessId");
        if (businessIdStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing businessId"));
        }

        try {
            UUID businessId = UUID.fromString(businessIdStr);
            Optional<Business> businessOpt = businessRepository.findById(businessId);
            
            if (businessOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Business not found"));
            }

            Business business = businessOpt.get();
            String companyName = business.getName();

            if (companyName == null || companyName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Business name is required for verification"));
            }

            // Verify company using CIPC
            OmnicheckService.VerificationResult result = omnicheckService.verifyCompany(companyName);
            boolean verified = false;

            if (result.isSuccess()) {
                // Update business trust rating
                Optional<BusinessTrustRating> trustRatingOpt = businessTrustRatingRepository.findByBusinessId(businessId);
                BusinessTrustRating trustRating;
                
                if (trustRatingOpt.isPresent()) {
                    trustRating = trustRatingOpt.get();
                } else {
                    trustRating = new BusinessTrustRating();
                    trustRating.setBusiness(business);
                }
                
                trustRating.setVerifiedWithThirdParty(true);
                businessTrustRatingRepository.save(trustRating);
                verified = true;
            }

            return ResponseEntity.ok(Map.of(
                "success", result.isSuccess(),
                "verified", verified,
                "omnicheckResult", result.getRawResponse() != null ? result.getRawResponse() : "",
                "error", result.getError() != null ? result.getError() : ""
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid businessId format: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
}

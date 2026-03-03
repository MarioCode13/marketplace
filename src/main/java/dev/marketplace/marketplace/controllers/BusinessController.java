package dev.marketplace.marketplace.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.BusinessTrustRating;
import dev.marketplace.marketplace.repository.BusinessRepository;
import dev.marketplace.marketplace.repository.BusinessTrustRatingRepository;
import dev.marketplace.marketplace.service.OmnicheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

            // Require CIPC registration metadata before allowing Omnicheck verification.
            if (business.getCipcRegistrationNo() == null || business.getCipcRegistrationNo().trim().isEmpty()
                || business.getCipcBusinessName() == null || business.getCipcBusinessName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "CIPC registration number and registered name are required before verification",
                    "reason", "Please provide your CIPC registration details in Business Settings, then try verifying again."
                ));
            }

            // Fetch or create BusinessTrustRating for quota tracking / status
            BusinessTrustRating trustRating = businessTrustRatingRepository.findByBusinessId(businessId)
                .orElseGet(() -> {
                    BusinessTrustRating tr = BusinessTrustRating.builder()
                        .business(business)
                        .build();
                    return businessTrustRatingRepository.save(tr);
                });

            // Short-circuit: if already verified with Omnicheck, do not call Omnicheck again
            if (trustRating.isVerifiedWithThirdParty()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "verified", true,
                    "omnicheckResult", "ALREADY_VERIFIED",
                    "error", ""
                ));
            }

            // Enforce per-period attempt limits: allow up to 1 business verification per 30-day window
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime periodStart = trustRating.getBusinessVerificationPeriodStart();
            if (periodStart == null || periodStart.isBefore(now.minusDays(30))) {
                trustRating.setBusinessVerificationPeriodStart(now);
                trustRating.setBusinessVerificationAttempts(0);
            }

            int maxAttemptsPerPeriod = 1;
            if (trustRating.getBusinessVerificationAttempts() >= maxAttemptsPerPeriod) {
                return ResponseEntity.status(429).body(Map.of(
                    "error", "Business verification attempt limit reached",
                    "reason", "You have used all business verification attempts for this period. Please contact support or wait until your next billing cycle."
                ));
            }

            trustRating.setBusinessVerificationAttempts(trustRating.getBusinessVerificationAttempts() + 1);
            businessTrustRatingRepository.save(trustRating);

            // Verify company using CIPC
            OmnicheckService.VerificationResult result = omnicheckService.verifyCompany(companyName);
            boolean verified = false;

            if (result.isSuccess()) {
                // Persist Omnicheck / CIPC metadata on the business profile when available
                JsonNode root = result.getResponseJson();
                if (root != null && !root.isNull()) {
                    JsonNode matches = root.has("CompanyMatches") && !root.get("CompanyMatches").isNull()
                        ? root.get("CompanyMatches")
                        : root;

                    if (matches != null && !matches.isNull()) {
                        if (matches.has("CommercialID") && !matches.get("CommercialID").isNull()) {
                            business.setCipcCommercialId(matches.get("CommercialID").asText());
                        }
                        if (matches.has("RegistrationNo") && !matches.get("RegistrationNo").isNull()) {
                            business.setCipcRegistrationNo(matches.get("RegistrationNo").asText());
                        }
                        if (matches.has("Businessname") && !matches.get("Businessname").isNull()) {
                            business.setCipcBusinessName(matches.get("Businessname").asText());
                        }
                        if (matches.has("EnquiryID") && !matches.get("EnquiryID").isNull()) {
                            business.setCipcEnquiryId(matches.get("EnquiryID").asText());
                        }
                        if (matches.has("EnquiryResultID") && !matches.get("EnquiryResultID").isNull()) {
                            business.setCipcEnquiryResultId(matches.get("EnquiryResultID").asText());
                        }
                    }

                    businessRepository.save(business);
                }

                // Update business trust rating
                Optional<BusinessTrustRating> trustRatingOpt = businessTrustRatingRepository.findByBusinessId(businessId);

                
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

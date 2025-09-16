package dev.marketplace.marketplace.controllers;

import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class BusinessController {
    private final BusinessRepository businessRepository;

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
}

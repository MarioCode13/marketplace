package dev.marketplace.marketplace.controllers;

import dev.marketplace.marketplace.dto.EmailRequest;
import dev.marketplace.marketplace.service.EmailService;
import dev.marketplace.marketplace.service.UserService;
import jakarta.mail.MessagingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EmailController {
    private final EmailService emailService;
    private final UserService userService;

    public EmailController(EmailService emailService, UserService userService) {
        this.emailService = emailService;
        this.userService = userService;
    }

    @PostMapping("/send-email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest request) {
        try {
            String recipientEmail;
            
            // Prefer sellerId over direct email (more secure)
            if (request.getSellerId() != null) {
                // Look up email server-side - never expose to client
                recipientEmail = userService.getUserById(request.getSellerId()).getEmail();
            } else if (request.getTo() != null && !request.getTo().isEmpty()) {
                // Fallback for backward compatibility (deprecated)
                recipientEmail = request.getTo();
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Either sellerId or to (email) must be provided");
            }
            
            emailService.sendEmail(recipientEmail, request.getSubject(), request.getMessage());
            return ResponseEntity.ok("Email sent successfully");
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send email");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}


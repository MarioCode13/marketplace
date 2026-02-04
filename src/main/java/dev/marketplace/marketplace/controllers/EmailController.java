package dev.marketplace.marketplace.controllers;

import dev.marketplace.marketplace.dto.EmailRequest;
import dev.marketplace.marketplace.service.EmailService;
import dev.marketplace.marketplace.service.UserService;
import dev.marketplace.marketplace.repository.BusinessRepository;
import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.User;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EmailController {
    private final EmailService emailService;
    private final UserService userService;
    private final BusinessRepository businessRepository;
    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

    public EmailController(EmailService emailService, UserService userService, BusinessRepository businessRepository) {
        this.emailService = emailService;
        this.userService = userService;
        this.businessRepository = businessRepository;
    }

    @PostMapping("/send-email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest request) {
        try {
            String recipientEmail;
            String listingTitle = request.getListingTitle(); // Read from request
            String sellerName = null;

            // Prefer sellerId over direct email (more secure)
            if (request.getSellerId() != null) {
                logger.info("send-email called with sellerId={}", request.getSellerId());
                // Try resolving as a User first
                Optional<User> userOpt = userService.findById(request.getSellerId());
                if (userOpt.isPresent()) {
                    User u = userOpt.get();
                    recipientEmail = u.getEmail();
                    // Use firstName + lastName if available, otherwise use username
                    if (u.getFirstName() != null && !u.getFirstName().isBlank()) {
                        sellerName = u.getFirstName() + (u.getLastName() != null && !u.getLastName().isBlank() ? " " + u.getLastName() : "");
                    } else {
                        sellerName = u.getUsername();
                    }
                    logger.info("Resolved sellerId to user email={}", recipientEmail);
                } else {
                    // Try resolving as a Business id (some listings are owned by businesses)
                    Optional<Business> businessOpt = businessRepository.findById(request.getSellerId());
                    if (businessOpt.isPresent()) {
                        Business b = businessOpt.get();
                        recipientEmail = b.getBusinessEmail();
                        sellerName = b.getName();
                        logger.info("Resolved sellerId to business email={}", recipientEmail);
                    } else {
                        logger.warn("sellerId {} did not match user nor business", request.getSellerId());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("sellerId does not match a user or a business");
                    }
                }
            } else if (request.getTo() != null && !request.getTo().isEmpty()) {
                // Fallback for backward compatibility (deprecated)
                recipientEmail = request.getTo();
                logger.info("send-email called with direct to address={}", recipientEmail);
            } else {
                logger.warn("send-email called without sellerId or to");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Either sellerId or to (email) must be provided");
            }

            // Build buyer info
            String buyerEmail = request.getFromEmail();
            String buyerName = request.getFromName();

            // If fromEmail not provided, try to parse it from the message body (simple regex)
            if ((buyerEmail == null || buyerEmail.isBlank()) && request.getMessage() != null) {
                String msg = request.getMessage();
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
                java.util.regex.Matcher m = p.matcher(msg);
                if (m.find()) {
                    buyerEmail = m.group(0);
                }
            }

            // Create mailto reply link (mailto:buyer@example.com?subject=Re:%20listing)
            String replyMailto = "";
            if (buyerEmail != null && !buyerEmail.isBlank()) {
                String encodedSubject = URLEncoder.encode((request.getSubject() == null ? "Re: your listing" : "Re: " + request.getSubject()), StandardCharsets.UTF_8);
                replyMailto = "mailto:" + buyerEmail + "?subject=" + encodedSubject;
            }

            // Use templated email for Dealio branded contact message
            emailService.sendContactSellerEmail(recipientEmail, sellerName, listingTitle, buyerName, buyerEmail, request.getMessage(), replyMailto);

            return ResponseEntity.ok("Email sent successfully");
        } catch (MessagingException e) {
            logger.error("MessagingException when sending email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send email");
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request to send-email: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}

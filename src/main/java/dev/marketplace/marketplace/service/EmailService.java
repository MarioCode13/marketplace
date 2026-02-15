package dev.marketplace.marketplace.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${app.dev.email.override:}")
    private String devEmailOverride;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String body) throws MessagingException {
        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email address is required");
        }

        // Override recipient in dev profile if override is set
        if ("dev".equals(activeProfile) && devEmailOverride != null && !devEmailOverride.isEmpty()) {
            logger.info("Dev profile active - overriding recipient {} -> {}", to, devEmailOverride);
            to = devEmailOverride;
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        logger.info("Sending email to: {} subject: {}", to, subject);
        helper.setSubject(subject);
        helper.setText(body, true);

        if (senderEmail != null && !senderEmail.isEmpty()) {
            helper.setFrom(senderEmail);
        } else {
            logger.warn("spring.mail.username (sender email) is not set; using default from address may fail");
        }

        try {
            mailSender.send(message);
        } catch (MailException e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            // Wrap Spring MailException in a MessagingException so controllers handle uniformly
            MessagingException me = new MessagingException("Failed to send email");
            me.initCause(e);
            throw me;
        }
    }

    public void sendBusinessVerificationEmail(String to, String businessName, String verificationUrl) throws MessagingException {
        Context context = new Context();
        context.setVariable("businessName", businessName);
        context.setVariable("verificationUrl", verificationUrl);
        String body = templateEngine.process("business-verification.html", context);
        sendEmail(to, "Verify your business email for " + businessName, body);
    }

    // New: send a Dealio-branded contact-seller email using Thymeleaf template
    public void sendContactSellerEmail(String to, String sellerName, String listingTitle, String buyerName, String buyerEmail, String messageText, String replyMailto) throws MessagingException {
        Context context = new Context();
        context.setVariable("sellerName", sellerName == null ? "Seller" : sellerName);
        context.setVariable("listingTitle", listingTitle == null ? "your listing" : listingTitle);
        context.setVariable("buyerName", buyerName == null ? "Buyer" : buyerName);
        context.setVariable("buyerEmail", buyerEmail == null ? "" : buyerEmail);
        context.setVariable("message", messageText == null ? "" : messageText.replace("\n", "<br/>"));
        context.setVariable("replyMailto", replyMailto);

        String body = templateEngine.process("contact-seller.html", context);
        String subject = "Dealio: " + (listingTitle == null ? "New inquiry" : "Inquiry about " + listingTitle);
        sendEmail(to, subject, body);
    }

    /**
     * Send email verification link to new user
     * @param to User's email address
     * @param userName User's name
     * @param verificationUrl Full URL to verification endpoint
     */
    public void sendEmailVerificationEmail(String to, String userName, String verificationUrl) throws MessagingException {
        Context context = new Context();
        context.setVariable("userName", userName == null ? "User" : userName);
        context.setVariable("verificationUrl", verificationUrl);
        String body = templateEngine.process("verify-email.html", context);
        sendEmail(to, "Verify your email address - Dealio", body);
    }

    /**
     * Send subscription confirmation email
     * @param to User's email address
     * @param userName User's name
     * @param planName Name of subscription plan (e.g., "Seller+")
     * @param amount Price amount
     * @param billingCycle Billing cycle (MONTHLY or YEARLY)
     * @param nextBillingDate Next billing date as string
     */
    public void sendSubscriptionConfirmationEmail(String to, String userName, String planName, String amount, String billingCycle, String nextBillingDate) throws MessagingException {
        Context context = new Context();
        context.setVariable("userName", userName == null ? "User" : userName);
        context.setVariable("planName", planName == null ? "Premium Plan" : planName);
        context.setVariable("amount", amount == null ? "0.00" : amount);
        context.setVariable("billingCycle", billingCycle == null ? "MONTHLY" : billingCycle);
        context.setVariable("nextBillingDate", nextBillingDate == null ? "TBD" : nextBillingDate);

        String body = templateEngine.process("subscription-confirmation.html", context);
        sendEmail(to, "Subscription Confirmed - Dealio", body);
    }

    /**
     * Send review request email to buyer after seller marks item as sold
     * @param to Buyer's email address
     * @param buyerName Buyer's name
     * @param sellerName Seller's name
     * @param listingTitle Title of the item purchased
     * @param reviewUrl URL to leave review
     */
    public void sendReviewRequestEmail(String to, String buyerName, String sellerName, String listingTitle, String reviewUrl) throws MessagingException {
        Context context = new Context();
        context.setVariable("buyerName", buyerName == null ? "Buyer" : buyerName);
        context.setVariable("sellerName", sellerName == null ? "Seller" : sellerName);
        context.setVariable("listingTitle", listingTitle == null ? "item" : listingTitle);
        context.setVariable("reviewUrl", reviewUrl);

        String body = templateEngine.process("review-request.html", context);
        sendEmail(to, "Please review your purchase from " + (sellerName == null ? "your seller" : sellerName) + " - Dealio", body);
    }

    /**
     * Send listing expiration warning to seller
     * @param to Seller's email address
     * @param sellerName Seller's name
     * @param listingTitle Title of the listing
     * @param daysRemaining Days until listing expires
     * @param renewUrl URL to renew listing
     */
    public void sendListingExpirationWarningEmail(String to, String sellerName, String listingTitle, int daysRemaining, String renewUrl) throws MessagingException {
        Context context = new Context();
        context.setVariable("sellerName", sellerName == null ? "Seller" : sellerName);
        context.setVariable("listingTitle", listingTitle == null ? "your listing" : listingTitle);
        context.setVariable("daysRemaining", daysRemaining);
        context.setVariable("renewUrl", renewUrl);

        String body = templateEngine.process("listing-expiration-warning.html", context);
        sendEmail(to, "Your listing expires in " + daysRemaining + " days - Dealio", body);
    }

    /**
     * Send subscription expiration warning to user
     * @param to User's email address
     * @param userName User's name
     * @param planName Name of subscription plan
     * @param expiryDate Expiration date as string
     * @param renewUrl URL to renew subscription (if applicable)
     * @param isAutoRenewal Whether subscription auto-renews
     */
    public void sendSubscriptionExpirationWarningEmail(String to, String userName, String planName, String expiryDate, String renewUrl, boolean isAutoRenewal) throws MessagingException {
        Context context = new Context();
        context.setVariable("userName", userName == null ? "User" : userName);
        context.setVariable("planName", planName == null ? "Premium Plan" : planName);
        context.setVariable("expiryDate", expiryDate == null ? "TBD" : expiryDate);
        context.setVariable("renewUrl", renewUrl);
        context.setVariable("isAutoRenewal", isAutoRenewal);

        String body = templateEngine.process("subscription-expiration-warning.html", context);
        sendEmail(to, "Your " + (planName == null ? "subscription" : planName + " subscription") + " expires soon - Dealio", body);
    }
}

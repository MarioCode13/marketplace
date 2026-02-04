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
}

package dev.marketplace.marketplace.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

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
        // Override recipient in dev profile if override is set
        if ("dev".equals(activeProfile) && devEmailOverride != null && !devEmailOverride.isEmpty()) {
            to = devEmailOverride;
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        System.out.println("Sending email to: " + to);
        helper.setSubject(subject);
        helper.setText(body, true);
        helper.setFrom(senderEmail);

        mailSender.send(message);
    }

    public void sendBusinessVerificationEmail(String to, String businessName, String verificationUrl) throws MessagingException {
        Context context = new Context();
        context.setVariable("businessName", businessName);
        context.setVariable("verificationUrl", verificationUrl);
        String body = templateEngine.process("business-verification.html", context);
        sendEmail(to, "Verify your business email for " + businessName, body);
    }
}

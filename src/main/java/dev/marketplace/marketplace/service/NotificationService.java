package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.enums.BusinessUserRole;
import dev.marketplace.marketplace.model.Notification;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.NotificationRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailService emailService;

    public List<Notification> getNotificationsForUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepository.findByUser(user);
    }

    public Notification createNotification(User user, String type, String message, String data, boolean actionRequired) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message);
        notification.setData(data);
        notification.setActionRequired(actionRequired);
        notification.setRead(false);
        return notificationRepository.save(notification);
    }

    public boolean markNotificationRead(UUID notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.setRead(true);
            notificationRepository.save(notification);
            return true;
        }
        return false;
    }

    public Optional<Notification> getNotification(UUID notificationId) {
        return notificationRepository.findById(notificationId);
    }

    public void sendBusinessInviteNotification(User user, dev.marketplace.marketplace.model.Business business, User inviter) {
        String message = String.format("You have been invited to join the business '%s' by %s.", business.getName(), inviter.getEmail());
        createNotification(user, "BUSINESS_INVITE", message, String.valueOf(business.getId()), true);
    }

    public void sendRoleChangeNotification(User user, dev.marketplace.marketplace.model.Business business, BusinessUserRole newRole, User changer) {
        String message = String.format("Your role in business '%s' has been changed to %s by %s.", business.getName(), newRole.name(), changer.getEmail());
        createNotification(user, "ROLE_CHANGE", message, String.valueOf(business.getId()), false);
    }

    public void sendBusinessVerificationEmail(String email, String token, String businessName) {
        String subject = "Verify your business email for " + businessName;
        String verificationUrl = "https://yourdomain.com/business/verify-email?token=" + token;
        try {
            emailService.sendBusinessVerificationEmail(email, businessName, verificationUrl);
        } catch (Exception e) {
            // Log error or handle as needed
            System.err.println("Failed to send business verification email: " + e.getMessage());
        }
    }
}

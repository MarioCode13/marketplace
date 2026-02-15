package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.enums.BusinessUserRole;
import dev.marketplace.marketplace.model.Notification;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.model.Business;
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
        // Create in-app notification
        Notification notification = createNotification(
                user,
                "BUSINESS_INVITE",
                inviter.getUsername() + " invited you to join " + business.getName(),
                business.getId().toString(),
                true
        );
    }

    public void sendRoleChangeNotification(User user, dev.marketplace.marketplace.model.Business business, BusinessUserRole newRole, User changer) {
        // Create in-app notification
        Notification notification = createNotification(
                user,
                "ROLE_CHANGE",
                "Your role in " + business.getName() + " has been changed to " + newRole.name(),
                business.getId().toString(),
                false
        );
    }

    public void sendEmailVerificationNotification(User user, String verificationUrl) {
        // Create in-app notification
        Notification notification = createNotification(
                user,
                "EMAIL_VERIFICATION_REQUIRED",
                "Please verify your email address to activate your account",
                user.getId().toString(),
                true
        );

        // Send email
        try {
            emailService.sendEmailVerificationEmail(user.getEmail(), user.getUsername(), verificationUrl);
        } catch (Exception e) {
            // Log error but don't fail registration - user can still manually verify
            System.err.println("Failed to send email verification email to " + user.getEmail() + ": " + e.getMessage());
        }
    }

    public void sendBusinessVerificationEmail(String email, String token, String businessName) {
        // This would send a verification email
        // Implementation depends on EmailService
    }
}


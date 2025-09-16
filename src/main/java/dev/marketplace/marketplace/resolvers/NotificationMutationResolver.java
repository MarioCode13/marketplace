package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.Notification;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.BusinessUser;
import dev.marketplace.marketplace.enums.BusinessUserRole;
import dev.marketplace.marketplace.service.NotificationService;
import dev.marketplace.marketplace.repository.BusinessRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import dev.marketplace.marketplace.repository.BusinessUserRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.Optional;
import java.util.UUID;

@Controller
public class NotificationMutationResolver {
    private final NotificationService notificationService;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final BusinessUserRepository businessUserRepository;

    public NotificationMutationResolver(NotificationService notificationService,
                                        BusinessRepository businessRepository,
                                        UserRepository userRepository,
                                        BusinessUserRepository businessUserRepository) {
        this.notificationService = notificationService;
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.businessUserRepository = businessUserRepository;
    }

    @MutationMapping
    public Boolean markNotificationRead(@Argument UUID notificationId) {
        return notificationService.markNotificationRead(notificationId);
    }

    @MutationMapping
    public Boolean acceptBusinessInvitation(@Argument UUID notificationId) {
        Optional<Notification> notificationOpt = notificationService.getNotification(notificationId);
        if (notificationOpt.isEmpty()) return false;
        Notification notification = notificationOpt.get();
        if (!notification.isActionRequired() || !"BUSINESS_INVITE".equals(notification.getType())) return false;
        notification.setRead(true);
        notification.setActionRequired(false);
        notificationService.markNotificationRead(notificationId);
        // Parse businessId from notification data (assume data contains businessId as UUID string)
        try {
            UUID businessId = UUID.fromString(notification.getData());
            Business business = businessRepository.findById(businessId).orElse(null);
            User user = notification.getUser();
            if (business != null && user != null) {
                // Link user to business as CONTRIBUTOR by default
                BusinessUser businessUser = new BusinessUser();
                businessUser.setBusiness(business);
                businessUser.setUser(user);
                businessUser.setRole(BusinessUserRole.CONTRIBUTOR);
                businessUserRepository.save(businessUser);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    @MutationMapping
    public Boolean declineBusinessInvitation(@Argument UUID notificationId) {
        Optional<Notification> notificationOpt = notificationService.getNotification(notificationId);
        if (notificationOpt.isEmpty()) return false;
        Notification notification = notificationOpt.get();
        if (!notification.isActionRequired() || !"BUSINESS_INVITE".equals(notification.getType())) return false;
        notification.setRead(true);
        notification.setActionRequired(false);
        notificationService.markNotificationRead(notificationId);
        // Do not link user to business
        return true;
    }
}

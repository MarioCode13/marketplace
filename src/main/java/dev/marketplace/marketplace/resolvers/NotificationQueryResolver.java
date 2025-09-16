package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.Notification;
import dev.marketplace.marketplace.service.NotificationService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class NotificationQueryResolver {
    private final NotificationService notificationService;

    public NotificationQueryResolver(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @QueryMapping
    public List<Notification> notifications(@Argument UUID userId) {
        return notificationService.getNotificationsForUser(userId);
    }
}

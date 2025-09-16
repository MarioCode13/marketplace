package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.Notification;
import dev.marketplace.marketplace.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, java.util.UUID> {
    List<Notification> findByUser(User user);
}

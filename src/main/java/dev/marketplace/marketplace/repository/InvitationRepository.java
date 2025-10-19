package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.Invitation;
import dev.marketplace.marketplace.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    List<Invitation> findByRecipient(User recipient);
    List<Invitation> findBySender(User sender);
}

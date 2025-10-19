package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.Invitation;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.model.BusinessUser;
import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.repository.InvitationRepository;
import dev.marketplace.marketplace.repository.BusinessUserRepository;
import dev.marketplace.marketplace.repository.BusinessRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import dev.marketplace.marketplace.enums.BusinessUserRole;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final BusinessUserRepository businessUserRepository;
    private final BusinessRepository businessRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    public InvitationService(InvitationRepository invitationRepository,
                             BusinessUserRepository businessUserRepository,
                             BusinessRepository businessRepository,
                             UserService userService,
                             NotificationService notificationService) {
        this.invitationRepository = invitationRepository;
        this.businessUserRepository = businessUserRepository;
        this.businessRepository = businessRepository;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    public Invitation createInvitation(User sender, User recipient) {
        Invitation invitation = new Invitation();
        invitation.setSender(sender);
        invitation.setRecipient(recipient);
        invitation.setStatus(Invitation.InvitationStatus.PENDING);
        return invitationRepository.save(invitation);
    }

    public List<Invitation> getInvitationsForRecipient(User recipient) {
        return invitationRepository.findByRecipient(recipient);
    }

    // responderId is the user accepting/declining the invitation
    public Invitation respondToInvitation(UUID invitationId, Invitation.InvitationStatus status, UUID responderId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        if (invitation.getStatus() != Invitation.InvitationStatus.PENDING) {
            throw new IllegalStateException("Invitation already responded to");
        }

        // If accepting, ensure the responder exists and add to business_user
        if (status == Invitation.InvitationStatus.ACCEPTED) {
            // determine recipient user: either stored on invitation or find by email
            User recipient = invitation.getRecipient();
            if (recipient == null) {
                String email = invitation.getRecipientEmail();
                if (email == null) {
                    throw new IllegalArgumentException("No recipient specified for invitation");
                }
                recipient = userService.findByEmail(email)
                        .orElseThrow(() -> new IllegalArgumentException("User with email not found: " + email));
                invitation.setRecipient(recipient);
            }

            // ensure responder is the same as recipient
            if (!recipient.getId().equals(responderId)) {
                throw new SecurityException("Only the invited recipient can accept the invitation");
            }

            // create BusinessUser entry if not exists
            Business business = invitation.getBusiness();
            if (business == null) {
                throw new IllegalArgumentException("Invitation has no business associated");
            }

            boolean exists = businessUserRepository.existsByBusinessAndUser(business, recipient);
            if (!exists) {
                BusinessUser bu = new BusinessUser();
                bu.setBusiness(business);
                bu.setUser(recipient);
                bu.setRole(invitation.getRole() != null ? invitation.getRole() : BusinessUserRole.CONTRIBUTOR);
                businessUserRepository.save(bu);
            }
        }

        invitation.setStatus(status);
        Invitation saved = invitationRepository.save(invitation);
        // Optionally notify inviter that the invitation was accepted/declined
        try {
            User inviter = saved.getSender();
            if (inviter != null) {
                String type = status == Invitation.InvitationStatus.ACCEPTED ? "INVITE_ACCEPTED" : "INVITE_DECLINED";
                String recipientEmail = saved.getRecipient() != null
                        ? saved.getRecipient().getEmail()
                        : saved.getRecipientEmail();
                String businessName = saved.getBusiness() != null ? saved.getBusiness().getName() : "the business";
                String businessIdStr = saved.getBusiness() != null ? saved.getBusiness().getId().toString() : null;
                String msg = status == Invitation.InvitationStatus.ACCEPTED
                        ? String.format("%s accepted your invitation to join '%s'.", recipientEmail, businessName)
                        : String.format("%s declined your invitation to join '%s'.", recipientEmail, businessName);
                notificationService.createNotification(inviter, type, msg, businessIdStr, false);
            }
        } catch (Exception ignored) {}
        return saved;
    }

    // now include businessId so invitation is tied to a business
    public Invitation sendInvitation(User sender, UUID businessId, String recipientEmail, BusinessUserRole role) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found"));

        Invitation invitation = new Invitation();
        invitation.setSender(sender);
        invitation.setRecipientEmail(recipientEmail);
        // if recipient already exists in system, link
        userService.findByEmail(recipientEmail).ifPresent(invitation::setRecipient);
        invitation.setBusiness(business);
        invitation.setRole(role != null ? role : BusinessUserRole.CONTRIBUTOR);
        invitation.setStatus(Invitation.InvitationStatus.PENDING);
        // Fallback: set timestamps in case JPA auditing isn't active in some environments
        if (invitation.getCreatedAt() == null) {
            invitation.setCreatedAt(java.time.LocalDateTime.now());
        }
        if (invitation.getUpdatedAt() == null) {
            invitation.setUpdatedAt(invitation.getCreatedAt());
        }
        Invitation saved = invitationRepository.save(invitation);
        // If the recipient already exists (has a user record), create an actionable notification
        if (saved.getRecipient() != null) {
            notificationService.createNotification(
                    saved.getRecipient(),
                    "BUSINESS_INVITE",
                    String.format("You have been invited to join '%s' as %s.",
                            business.getName(), saved.getRole() != null ? saved.getRole().name() : BusinessUserRole.CONTRIBUTOR.name()),
                    business.getId().toString(),
                    true
            );
        }
        return saved;
    }
}

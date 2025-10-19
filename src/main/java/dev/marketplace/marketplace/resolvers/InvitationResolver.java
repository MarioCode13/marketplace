package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.Invitation;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.service.InvitationService;
import dev.marketplace.marketplace.service.UserService;
import dev.marketplace.marketplace.enums.BusinessUserRole;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

@Controller
public class InvitationResolver {

    private final InvitationService invitationService;
    private final UserService userService;

    public InvitationResolver(InvitationService invitationService, UserService userService) {
        this.invitationService = invitationService;
        this.userService = userService;
    }

    @QueryMapping
    public List<Invitation> invitationsForRecipient(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userService.getUserIdByUsername(userDetails.getUsername());
        User recipient = userService.getUserById(userId);
        return invitationService.getInvitationsForRecipient(recipient);
    }

    @MutationMapping
    public Invitation sendInvitation(
            @Argument UUID businessId,
            @Argument String recipientEmail,
            @Argument BusinessUserRole role,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID senderId = userService.getUserIdByUsername(userDetails.getUsername());
        User sender = userService.getUserById(senderId);
        return invitationService.sendInvitation(sender, businessId, recipientEmail, role);
    }

    @MutationMapping
    public Invitation acceptInvitation(@Argument UUID invitationId, @AuthenticationPrincipal UserDetails userDetails) {
        UUID responderId = userService.getUserIdByUsername(userDetails.getUsername());
        return invitationService.respondToInvitation(invitationId, Invitation.InvitationStatus.ACCEPTED, responderId);
    }

    @MutationMapping
    public Invitation declineInvitation(@Argument UUID invitationId, @AuthenticationPrincipal UserDetails userDetails) {
        UUID responderId = userService.getUserIdByUsername(userDetails.getUsername());
        return invitationService.respondToInvitation(invitationId, Invitation.InvitationStatus.DECLINED, responderId);
    }
}

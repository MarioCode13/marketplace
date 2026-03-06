package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.ContentApprovalQueue;
import dev.marketplace.marketplace.model.FlaggedSlug;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.service.AdminApprovalService;
import dev.marketplace.marketplace.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.UUID;

/**
 * GraphQL mutations for admin content approval workflows
 * Requires ADMIN role
 */
@CrossOrigin(origins = "http://localhost:3000")
@Controller
public class AdminApprovalMutationResolver {

    private final AdminApprovalService adminApprovalService;
    private final UserService userService;

    public AdminApprovalMutationResolver(AdminApprovalService adminApprovalService,
                                         UserService userService) {
        this.adminApprovalService = adminApprovalService;
        this.userService = userService;
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AdminApprovalQueryResolver.ContentApprovalQueueItemDTO approveListing(
            @Argument UUID approvalQueueId,
            @Argument String approvalNotes,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = userService.getUserIdByUsername(userDetails.getUsername());
        User adminUser = userService.getUserById(adminId);

        ContentApprovalQueue approval = adminApprovalService.approveListing(
                approvalQueueId,
                approvalNotes != null ? approvalNotes : "Approved",
                adminUser
        );

        return convertToDTO(approval);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AdminApprovalQueryResolver.ContentApprovalQueueItemDTO declineListing(
            @Argument UUID approvalQueueId,
            @Argument String declineReason,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = userService.getUserIdByUsername(userDetails.getUsername());
        User adminUser = userService.getUserById(adminId);

        ContentApprovalQueue approval = adminApprovalService.declineListing(
                approvalQueueId,
                declineReason,
                adminUser
        );

        return convertToDTO(approval);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AdminApprovalQueryResolver.FlaggedSlugDTO approveFlaggedSlug(
            @Argument UUID flaggedSlugId,
            @Argument String approvalNotes,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = userService.getUserIdByUsername(userDetails.getUsername());
        User adminUser = userService.getUserById(adminId);

        FlaggedSlug flaggedSlug = adminApprovalService.approveFlaggedSlug(
                flaggedSlugId,
                approvalNotes != null ? approvalNotes : "Approved",
                adminUser
        );

        return convertToSlugDTO(flaggedSlug);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AdminApprovalQueryResolver.FlaggedSlugDTO rejectFlaggedSlug(
            @Argument UUID flaggedSlugId,
            @Argument String rejectionReason,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = userService.getUserIdByUsername(userDetails.getUsername());
        User adminUser = userService.getUserById(adminId);

        FlaggedSlug flaggedSlug = adminApprovalService.rejectFlaggedSlug(
                flaggedSlugId,
                rejectionReason,
                adminUser
        );

        return convertToSlugDTO(flaggedSlug);
    }

    // Helper methods
    private AdminApprovalQueryResolver.ContentApprovalQueueItemDTO convertToDTO(ContentApprovalQueue item) {
        // Ensure listing is not null - it's required per the schema
        if (item.getListing() == null) {
            throw new IllegalStateException("ContentApprovalQueue item must have a non-null listing");
        }
        return new AdminApprovalQueryResolver.ContentApprovalQueueItemDTO(
                item.getId().toString(),
                item.getListing(),
                item.getFlagType().name(),
                item.getFlaggedData(),
                item.getStatus().name(),
                item.getApprovalNotes(),
                item.getCreatedAt().toString(),
                item.getUpdatedAt().toString(),
                item.getReviewedAt() != null ? item.getReviewedAt().toString() : null,
                item.getReviewedBy() != null ? item.getReviewedBy().getUsername() : null
        );
    }

    private AdminApprovalQueryResolver.FlaggedSlugDTO convertToSlugDTO(FlaggedSlug slug) {
        return new AdminApprovalQueryResolver.FlaggedSlugDTO(
                slug.getId().toString(),
                slug.getSlug(),
                slug.getBusiness().getId().toString(),
                slug.getReason(),
                slug.getStatus().name(),
                slug.getReviewNotes(),
                slug.getReviewedBy() != null ? slug.getReviewedBy().getUsername() : null,
                slug.getCreatedAt().toString(),
                slug.getUpdatedAt().toString(),
                slug.getReviewedAt() != null ? slug.getReviewedAt().toString() : null
        );
    }
}


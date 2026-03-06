package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.enums.ContentApprovalStatus;
import dev.marketplace.marketplace.enums.FlaggedSlugStatus;
import dev.marketplace.marketplace.model.ContentApprovalQueue;
import dev.marketplace.marketplace.model.FlaggedSlug;
import dev.marketplace.marketplace.service.AdminApprovalService;
import dev.marketplace.marketplace.service.AdminApprovalService.ApprovalDashboardStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.UUID;

/**
 * GraphQL queries for admin content approval workflows
 * Requires ADMIN role
 */
@CrossOrigin(origins = "http://localhost:3000")
@Controller
public class AdminApprovalQueryResolver {

    private final AdminApprovalService adminApprovalService;

    public AdminApprovalQueryResolver(AdminApprovalService adminApprovalService) {
        this.adminApprovalService = adminApprovalService;
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public NSFWContentPageDTO getPendingApprovals(
            @Argument Integer page,
            @Argument Integer size) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        Page<ContentApprovalQueue> result = adminApprovalService.getPendingApprovals(pageable);
        return convertToPageDTO(result, pageNum, pageSize);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ContentApprovalQueueItemDTO getApprovalQueueItem(@Argument UUID id) {
        ContentApprovalQueue item = adminApprovalService.getApprovalQueueEntry(id);
        return convertToDTO(item);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public NSFWContentPageDTO getApprovalsByType(
            @Argument String flagType,
            @Argument String status,
            @Argument Integer page,
            @Argument Integer size) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        ContentApprovalQueue.FlagType type = ContentApprovalQueue.FlagType.valueOf(flagType);
        ContentApprovalStatus approvalStatus = status != null ? ContentApprovalStatus.valueOf(status) : null;

        Page<ContentApprovalQueue> result = adminApprovalService.getApprovalsByType(type, approvalStatus, pageable);
        return convertToPageDTO(result, pageNum, pageSize);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public FlaggedSlugPageDTO getPendingFlaggedSlugs(
            @Argument Integer page,
            @Argument Integer size) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        Page<FlaggedSlug> result = adminApprovalService.getPendingFlaggedSlugs(pageable);
        return convertToSlugPageDTO(result, pageNum, pageSize);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public FlaggedSlugPageDTO getFlaggedSlugsByStatus(
            @Argument String status,
            @Argument Integer page,
            @Argument Integer size) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        Page<FlaggedSlug> result = adminApprovalService.getFlaggedSlugsByStatus(
                FlaggedSlugStatus.valueOf(status), pageable);
        return convertToSlugPageDTO(result, pageNum, pageSize);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public FlaggedSlugDTO getFlaggedSlug(@Argument UUID id) {
        FlaggedSlug slug = adminApprovalService.getFlaggedSlug(id);
        return convertToSlugDTO(slug);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ContentApprovalQueueItemDTO> getApprovalsForListing(@Argument UUID listingId) {
        List<ContentApprovalQueue> approvals = adminApprovalService.getApprovalsForListing(listingId);
        return approvals.stream().map(this::convertToDTO).toList();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApprovalDashboardStatsDTO getAdminDashboardStats() {
        ApprovalDashboardStats stats = adminApprovalService.getDashboardStats();
        return new ApprovalDashboardStatsDTO(
                (int) stats.totalPendingApprovals(),
                (int) stats.pendingNsfwApprovals(),
                (int) stats.pendingSlugApprovals(),
                (int) stats.pendingFlaggedSlugs()
        );
    }

    // Helper methods
    private NSFWContentPageDTO convertToPageDTO(Page<ContentApprovalQueue> page, int pageNum, int pageSize) {
        List<ContentApprovalQueueItemDTO> items = page.getContent().stream()
                .map(this::convertToDTO)
                .toList();
        return new NSFWContentPageDTO(
                items,
                (int) page.getTotalElements(),
                pageNum,
                pageSize,
                page.hasNext()
        );
    }

    private FlaggedSlugPageDTO convertToSlugPageDTO(Page<FlaggedSlug> page, int pageNum, int pageSize) {
        List<FlaggedSlugDTO> items = page.getContent().stream()
                .map(this::convertToSlugDTO)
                .toList();
        return new FlaggedSlugPageDTO(
                items,
                (int) page.getTotalElements(),
                pageNum,
                pageSize,
                page.hasNext()
        );
    }

    private ContentApprovalQueueItemDTO convertToDTO(ContentApprovalQueue item) {
        // Ensure listing is not null - it's required per the schema
        if (item.getListing() == null) {
            throw new IllegalStateException("ContentApprovalQueue item must have a non-null listing");
        }
        return new ContentApprovalQueueItemDTO(
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

    private FlaggedSlugDTO convertToSlugDTO(FlaggedSlug slug) {
        return new FlaggedSlugDTO(
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

    // DTOs
    public record NSFWContentPageDTO(
            List<ContentApprovalQueueItemDTO> items,
            int totalCount,
            int pageNumber,
            int pageSize,
            boolean hasNextPage
    ) {}

    public record FlaggedSlugPageDTO(
            List<FlaggedSlugDTO> items,
            int totalCount,
            int pageNumber,
            int pageSize,
            boolean hasNextPage
    ) {}

    public record ContentApprovalQueueItemDTO(
            String id,
            dev.marketplace.marketplace.model.Listing listing,
            String flagType,
            String flaggedData,
            String status,
            String approvalNotes,
            String createdAt,
            String updatedAt,
            String reviewedAt,
            String reviewedByUsername
    ) {}

    public record FlaggedSlugDTO(
            String id,
            String slug,
            String businessId,
            String reason,
            String status,
            String reviewNotes,
            String reviewedByUsername,
            String createdAt,
            String updatedAt,
            String reviewedAt
    ) {}

    public record ApprovalDashboardStatsDTO(
            int totalPendingApprovals,
            int pendingNsfwApprovals,
            int pendingSlugApprovals,
            int pendingFlaggedSlugs
    ) {}
}




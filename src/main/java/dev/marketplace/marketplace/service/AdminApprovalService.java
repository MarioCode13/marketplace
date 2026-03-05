package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.enums.ContentApprovalStatus;
import dev.marketplace.marketplace.enums.FlaggedSlugStatus;
import dev.marketplace.marketplace.exceptions.ResourceNotFoundException;
import dev.marketplace.marketplace.model.*;
import dev.marketplace.marketplace.repository.ContentApprovalQueueRepository;
import dev.marketplace.marketplace.repository.FlaggedSlugRepository;
import dev.marketplace.marketplace.repository.ListingRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for admin approval workflows
 * Handles approving/declining NSFW listings and flagged slugs
 */
@Service
@Transactional
public class AdminApprovalService {

    private final ContentApprovalQueueRepository contentApprovalQueueRepository;
    private final FlaggedSlugRepository flaggedSlugRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public AdminApprovalService(ContentApprovalQueueRepository contentApprovalQueueRepository,
                                FlaggedSlugRepository flaggedSlugRepository,
                                ListingRepository listingRepository,
                                UserRepository userRepository) {
        this.contentApprovalQueueRepository = contentApprovalQueueRepository;
        this.flaggedSlugRepository = flaggedSlugRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    /**
     * Approve a flagged listing
     */
    public ContentApprovalQueue approveListing(UUID approvalQueueId, String approvalNotes, User adminUser) {
        ContentApprovalQueue approval = contentApprovalQueueRepository.findById(approvalQueueId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval queue entry not found"));

        approval.setStatus(ContentApprovalStatus.APPROVED);
        approval.setApprovalNotes(approvalNotes);
        approval.setReviewedBy(adminUser);
        approval.setReviewedAt(LocalDateTime.now());

        // Update listing NSFW status
        if (approval.getFlagType() == ContentApprovalQueue.FlagType.NSFW_IMAGE) {
            Listing listing = approval.getListing();
            listing.setNsfwApprovalStatus(ContentApprovalStatus.APPROVED);
            listing.setNsfwReviewNotes(approvalNotes);
            listing.setNsfwReviewedAt(LocalDateTime.now());
            listing.setNsfwReviewedBy(adminUser);
            listingRepository.save(listing);
        }

        return contentApprovalQueueRepository.save(approval);
    }

    /**
     * Decline a flagged listing
     */
    public ContentApprovalQueue declineListing(UUID approvalQueueId, String declineReason, User adminUser) {
        ContentApprovalQueue approval = contentApprovalQueueRepository.findById(approvalQueueId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval queue entry not found"));

        approval.setStatus(ContentApprovalStatus.DECLINED);
        approval.setApprovalNotes(declineReason);
        approval.setReviewedBy(adminUser);
        approval.setReviewedAt(LocalDateTime.now());

        // Update listing NSFW status
        if (approval.getFlagType() == ContentApprovalQueue.FlagType.NSFW_IMAGE) {
            Listing listing = approval.getListing();
            listing.setNsfwApprovalStatus(ContentApprovalStatus.DECLINED);
            listing.setNsfwReviewNotes(declineReason);
            listing.setNsfwReviewedAt(LocalDateTime.now());
            listing.setNsfwReviewedBy(adminUser);
            listingRepository.save(listing);
        }

        return contentApprovalQueueRepository.save(approval);
    }

    /**
     * Approve a flagged slug
     */
    public FlaggedSlug approveFlaggedSlug(UUID flaggedSlugId, String approvalNotes, User adminUser) {
        FlaggedSlug flaggedSlug = flaggedSlugRepository.findById(flaggedSlugId)
                .orElseThrow(() -> new ResourceNotFoundException("Flagged slug not found"));

        flaggedSlug.setStatus(FlaggedSlugStatus.APPROVED);
        flaggedSlug.setReviewNotes(approvalNotes);
        flaggedSlug.setReviewedBy(adminUser);
        flaggedSlug.setReviewedAt(LocalDateTime.now());

        return flaggedSlugRepository.save(flaggedSlug);
    }

    /**
     * Reject a flagged slug
     */
    public FlaggedSlug rejectFlaggedSlug(UUID flaggedSlugId, String rejectionReason, User adminUser) {
        FlaggedSlug flaggedSlug = flaggedSlugRepository.findById(flaggedSlugId)
                .orElseThrow(() -> new ResourceNotFoundException("Flagged slug not found"));

        flaggedSlug.setStatus(FlaggedSlugStatus.REJECTED);
        flaggedSlug.setReviewNotes(rejectionReason);
        flaggedSlug.setReviewedBy(adminUser);
        flaggedSlug.setReviewedAt(LocalDateTime.now());

        return flaggedSlugRepository.save(flaggedSlug);
    }

    /**
     * Get pending content approvals with pagination
     */
    public Page<ContentApprovalQueue> getPendingApprovals(Pageable pageable) {
        return contentApprovalQueueRepository.findPendingApprovals(pageable);
    }

    /**
     * Get content approvals filtered by type
     */
    public Page<ContentApprovalQueue> getApprovalsByType(ContentApprovalQueue.FlagType flagType, ContentApprovalStatus status, Pageable pageable) {
        return contentApprovalQueueRepository.findByFlagTypeAndStatus(flagType, status, pageable);
    }

    /**
     * Get pending flagged slugs
     */
    public Page<FlaggedSlug> getPendingFlaggedSlugs(Pageable pageable) {
        return flaggedSlugRepository.findPendingFlaggedSlugs(FlaggedSlugStatus.PENDING, pageable);
    }

    /**
     * Get flagged slugs by status
     */
    public Page<FlaggedSlug> getFlaggedSlugsByStatus(FlaggedSlugStatus status, Pageable pageable) {
        return flaggedSlugRepository.findByStatus(status, pageable);
    }

    /**
     * Get approval queue entry by ID
     */
    public ContentApprovalQueue getApprovalQueueEntry(UUID approvalQueueId) {
        return contentApprovalQueueRepository.findById(approvalQueueId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval queue entry not found"));
    }

    /**
     * Get flagged slug by ID
     */
    public FlaggedSlug getFlaggedSlug(UUID flaggedSlugId) {
        return flaggedSlugRepository.findById(flaggedSlugId)
                .orElseThrow(() -> new ResourceNotFoundException("Flagged slug not found"));
    }

    /**
     * Get approval queue entries for a listing
     */
    public List<ContentApprovalQueue> getApprovalsForListing(UUID listingId) {
        return contentApprovalQueueRepository.findByListingId(listingId);
    }

    /**
     * Get approval dashboard stats
     */
    public ApprovalDashboardStats getDashboardStats() {
        long totalPending = contentApprovalQueueRepository.countPending();
        long nsfw_pending = contentApprovalQueueRepository.countPendingByFlagType(ContentApprovalQueue.FlagType.NSFW_IMAGE);
        long slug_pending = contentApprovalQueueRepository.countPendingByFlagType(ContentApprovalQueue.FlagType.PROBLEMATIC_SLUG);
        long flaggedSlugs = flaggedSlugRepository.countPending();

        return new ApprovalDashboardStats(totalPending, nsfw_pending, slug_pending, flaggedSlugs);
    }

    /**
     * DTO for dashboard statistics
     */
    public record ApprovalDashboardStats(
            long totalPendingApprovals,
            long pendingNsfwApprovals,
            long pendingSlugApprovals,
            long pendingFlaggedSlugs
    ) {}
}






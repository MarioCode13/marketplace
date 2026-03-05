package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.enums.FlaggedSlugStatus;
import dev.marketplace.marketplace.exceptions.ResourceNotFoundException;
import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.FlaggedSlug;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.BusinessRepository;
import dev.marketplace.marketplace.repository.FlaggedSlugRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing flagged slugs
 * Handles the workflow for slug approval/rejection
 */
@Service
@Transactional
public class SlugApprovalService {

    private final FlaggedSlugRepository flaggedSlugRepository;
    private final BusinessRepository businessRepository;

    public SlugApprovalService(FlaggedSlugRepository flaggedSlugRepository,
                               BusinessRepository businessRepository) {
        this.flaggedSlugRepository = flaggedSlugRepository;
        this.businessRepository = businessRepository;
    }

    /**
     * Flag a slug that needs review
     * Called when slug similarity check returns a potential conflict
     */
    public FlaggedSlug flagSlugForReview(UUID businessId, String slug, String reason) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        // Check if this slug is already flagged for this business
        Optional<FlaggedSlug> existing = flaggedSlugRepository.findBySlugAndBusinessId(slug, businessId);
        if (existing.isPresent() && existing.get().getStatus() == FlaggedSlugStatus.PENDING) {
            // Already flagged, return existing flag
            return existing.get();
        }

        FlaggedSlug flaggedSlug = FlaggedSlug.builder()
                .slug(slug)
                .business(business)
                .reason(reason)
                .status(FlaggedSlugStatus.PENDING)
                .build();

        return flaggedSlugRepository.save(flaggedSlug);
    }

    /**
     * Approve a flagged slug - business can now use it
     */
    public FlaggedSlug approveSlug(UUID flaggedSlugId, String approvalNotes, User adminUser) {
        FlaggedSlug flaggedSlug = flaggedSlugRepository.findById(flaggedSlugId)
                .orElseThrow(() -> new ResourceNotFoundException("Flagged slug not found"));

        flaggedSlug.setStatus(FlaggedSlugStatus.APPROVED);
        flaggedSlug.setReviewNotes(approvalNotes);
        flaggedSlug.setReviewedBy(adminUser);
        flaggedSlug.setReviewedAt(LocalDateTime.now());

        return flaggedSlugRepository.save(flaggedSlug);
    }

    /**
     * Reject a flagged slug - business cannot use this slug
     */
    public FlaggedSlug rejectSlug(UUID flaggedSlugId, String rejectionReason, User adminUser) {
        FlaggedSlug flaggedSlug = flaggedSlugRepository.findById(flaggedSlugId)
                .orElseThrow(() -> new ResourceNotFoundException("Flagged slug not found"));

        flaggedSlug.setStatus(FlaggedSlugStatus.REJECTED);
        flaggedSlug.setReviewNotes(rejectionReason);
        flaggedSlug.setReviewedBy(adminUser);
        flaggedSlug.setReviewedAt(LocalDateTime.now());

        return flaggedSlugRepository.save(flaggedSlug);
    }

    /**
     * Get pending slugs for a specific business
     */
    public Page<FlaggedSlug> getPendingSlugsForBusiness(UUID businessId, Pageable pageable) {
        // This would need a custom query in repository
        // For now, get all and filter
        return flaggedSlugRepository.findByBusinessId(businessId).stream()
                .filter(fs -> fs.getStatus() == FlaggedSlugStatus.PENDING)
                .toList()
                .isEmpty() ? Page.empty(pageable) :
                flaggedSlugRepository.findByStatus(FlaggedSlugStatus.PENDING, pageable);
    }

    /**
     * Get all pending flagged slugs
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
     * Get a flagged slug by ID
     */
    public FlaggedSlug getFlaggedSlug(UUID flaggedSlugId) {
        return flaggedSlugRepository.findById(flaggedSlugId)
                .orElseThrow(() -> new ResourceNotFoundException("Flagged slug not found"));
    }

    /**
     * Check if a business has a pending slug review
     */
    public boolean hasPendingSlugReview(UUID businessId, String slug) {
        return flaggedSlugRepository.findBySlugAndBusinessId(slug, businessId)
                .map(fs -> fs.getStatus() == FlaggedSlugStatus.PENDING)
                .orElse(false);
    }

    /**
     * Check if a slug is approved for use
     */
    public boolean isSlugApproved(UUID businessId, String slug) {
        return flaggedSlugRepository.findBySlugAndBusinessId(slug, businessId)
                .map(fs -> fs.getStatus() == FlaggedSlugStatus.APPROVED)
                .orElse(false);
    }

    /**
     * Check if a slug is rejected (cannot be used)
     */
    public boolean isSlugRejected(UUID businessId, String slug) {
        return flaggedSlugRepository.findBySlugAndBusinessId(slug, businessId)
                .map(fs -> fs.getStatus() == FlaggedSlugStatus.REJECTED)
                .orElse(false);
    }

    /**
     * Get the status of a slug for a business
     */
    public Optional<FlaggedSlug> getSlugStatus(UUID businessId, String slug) {
        return flaggedSlugRepository.findBySlugAndBusinessId(slug, businessId);
    }

    /**
     * Get count of pending slugs
     */
    public long countPendingFlaggedSlugs() {
        return flaggedSlugRepository.countPending();
    }

    /**
     * Get slug approval status message
     */
    public String getSlugStatusMessage(UUID businessId, String slug) {
        return flaggedSlugRepository.findBySlugAndBusinessId(slug, businessId)
                .map(fs -> {
                    FlaggedSlugStatus status = fs.getStatus();
                    if (status == FlaggedSlugStatus.PENDING) {
                        return "Slug pending admin review";
                    } else if (status == FlaggedSlugStatus.APPROVED) {
                        return "Slug approved for use";
                    } else if (status == FlaggedSlugStatus.REJECTED) {
                        return "Slug rejected - cannot be used. Reason: " + fs.getReviewNotes();
                    } else {
                        return "Slug status unknown";
                    }
                })
                .orElse("Slug not flagged");
    }
}



package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.enums.ContentApprovalStatus;
import dev.marketplace.marketplace.model.ContentApprovalQueue;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.repository.ContentApprovalQueueRepository;
import dev.marketplace.marketplace.repository.ListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for flagging content that needs admin review
 * Creates approval queue entries when content is flagged
 */
@Service
@Transactional
public class ContentFlaggingService {

    private final ContentApprovalQueueRepository contentApprovalQueueRepository;
    private final ListingRepository listingRepository;

    public ContentFlaggingService(ContentApprovalQueueRepository contentApprovalQueueRepository,
                                  ListingRepository listingRepository) {
        this.contentApprovalQueueRepository = contentApprovalQueueRepository;
        this.listingRepository = listingRepository;
    }

    /**
     * Flag a listing as NSFW - creates an approval queue entry
     * Called when NSFW detection happens on frontend or backend
     */
    public ContentApprovalQueue flagListingAsNSFW(UUID listingId, String flaggedImageUrls) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        // Mark listing as flagged
        listing.setNsfwFlagged(true);
        listing.setNsfwApprovalStatus(ContentApprovalStatus.PENDING);
        listingRepository.save(listing);

        // Create approval queue entry
        ContentApprovalQueue approvalEntry = ContentApprovalQueue.builder()
                .listing(listing)
                .flagType(ContentApprovalQueue.FlagType.NSFW_IMAGE)
                .flaggedData(flaggedImageUrls) // JSON array of flagged image URLs
                .status(ContentApprovalStatus.PENDING)
                .build();

        return contentApprovalQueueRepository.save(approvalEntry);
    }

    /**
     * Flag a listing with a problematic slug - creates an approval queue entry
     * Called when slug validation detects similarity to reserved brands
     */
    public ContentApprovalQueue flagListingForProblematicSlug(UUID listingId, String slugDetails) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        // Create approval queue entry for slug issue
        ContentApprovalQueue approvalEntry = ContentApprovalQueue.builder()
                .listing(listing)
                .flagType(ContentApprovalQueue.FlagType.PROBLEMATIC_SLUG)
                .flaggedData(slugDetails) // JSON with slug and reason
                .status(ContentApprovalStatus.PENDING)
                .build();

        return contentApprovalQueueRepository.save(approvalEntry);
    }

    /**
     * Unflag a listing - removes the NSFW flag and cleans up pending approvals
     * Called when content is manually reviewed and deemed safe
     */
    public void unflagListing(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        listing.setNsfwFlagged(false);
        listingRepository.save(listing);

        // Remove any pending NSFW approval entries for this listing
        List<ContentApprovalQueue> approvals = contentApprovalQueueRepository.findByListingId(listingId);
        approvals.stream()
                .filter(a -> a.getStatus() == ContentApprovalStatus.PENDING &&
                        a.getFlagType() == ContentApprovalQueue.FlagType.NSFW_IMAGE)
                .forEach(contentApprovalQueueRepository::delete);
    }

    /**
     * Check if a listing has pending approvals
     */
    public boolean hasPendingApproval(UUID listingId) {
        return contentApprovalQueueRepository.findByListingIdAndStatus(
                listingId, ContentApprovalStatus.PENDING
        ).isPresent();
    }

    /**
     * Get pending approval count for a listing
     */
    public long getPendingApprovalCount(UUID listingId) {
        List<ContentApprovalQueue> approvals = contentApprovalQueueRepository.findByListingId(listingId);
        return approvals.stream()
                .filter(a -> a.getStatus() == ContentApprovalStatus.PENDING)
                .count();
    }
}


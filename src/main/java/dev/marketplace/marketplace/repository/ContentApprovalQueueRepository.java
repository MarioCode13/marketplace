package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.ContentApprovalQueue;
import dev.marketplace.marketplace.enums.ContentApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentApprovalQueueRepository extends JpaRepository<ContentApprovalQueue, UUID> {

    // Find all pending approvals
    Page<ContentApprovalQueue> findByStatus(ContentApprovalStatus status, Pageable pageable);

    // Find pending approvals ordered by creation date (oldest first)
    @Query("SELECT caq FROM ContentApprovalQueue caq WHERE caq.status = 'PENDING' ORDER BY caq.createdAt ASC")
    Page<ContentApprovalQueue> findPendingApprovals(Pageable pageable);

    // Find approvals for a specific listing
    List<ContentApprovalQueue> findByListingId(UUID listingId);

    // Find pending approval for a listing
    Optional<ContentApprovalQueue> findByListingIdAndStatus(UUID listingId, ContentApprovalStatus status);

    // Find by flag type
    Page<ContentApprovalQueue> findByFlagType(ContentApprovalQueue.FlagType flagType, Pageable pageable);

    // Find by flag type and status
    Page<ContentApprovalQueue> findByFlagTypeAndStatus(ContentApprovalQueue.FlagType flagType, ContentApprovalStatus status, Pageable pageable);

    // Count pending approvals
    @Query("SELECT COUNT(caq) FROM ContentApprovalQueue caq WHERE caq.status = 'PENDING'")
    long countPending();

    // Count pending by flag type
    @Query("SELECT COUNT(caq) FROM ContentApprovalQueue caq WHERE caq.status = 'PENDING' AND caq.flagType = :flagType")
    long countPendingByFlagType(@Param("flagType") ContentApprovalQueue.FlagType flagType);

    // Find approvals reviewed after a certain date
    List<ContentApprovalQueue> findByReviewedAtAfter(LocalDateTime date);

    // Find approvals by specific admin
    Page<ContentApprovalQueue> findByReviewedBy(UUID adminId, Pageable pageable);
}


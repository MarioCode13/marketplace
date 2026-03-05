package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.FlaggedSlug;
import dev.marketplace.marketplace.enums.FlaggedSlugStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlaggedSlugRepository extends JpaRepository<FlaggedSlug, UUID> {

    // Find all flagged slugs for a business
    List<FlaggedSlug> findByBusinessId(UUID businessId);

    // Find pending flagged slugs
    Page<FlaggedSlug> findByStatus(FlaggedSlugStatus status, Pageable pageable);

    // Find pending slugs ordered by creation date
    @Query("SELECT fs FROM FlaggedSlug fs WHERE fs.status = :status ORDER BY fs.createdAt ASC")
    Page<FlaggedSlug> findPendingFlaggedSlugs(@Param("status") FlaggedSlugStatus status, Pageable pageable);

    // Find by slug value
    Optional<FlaggedSlug> findBySlugAndBusinessId(String slug, UUID businessId);

    // Check if slug exists for business
    boolean existsBySlugAndBusinessId(String slug, UUID businessId);

    // Find by business and status
    List<FlaggedSlug> findByBusinessIdAndStatus(UUID businessId, FlaggedSlugStatus status);

    // Count pending for dashboard
    @Query("SELECT COUNT(fs) FROM FlaggedSlug fs WHERE fs.status = 'PENDING'")
    long countPending();
}


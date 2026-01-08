package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.StoreBranding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreBrandingRepository extends JpaRepository<StoreBranding, UUID> {
    Optional<StoreBranding> findByBusiness(Business business);

    @Modifying
    @Query("UPDATE StoreBranding sb SET sb.version = 0 WHERE sb.businessId = :businessId AND sb.version IS NULL")
    void setVersionToZeroIfNull(@Param("businessId") UUID businessId);
}

package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.StoreBranding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreBrandingRepository extends JpaRepository<StoreBranding, UUID> {
    Optional<StoreBranding> findByBusiness(Business business);
}

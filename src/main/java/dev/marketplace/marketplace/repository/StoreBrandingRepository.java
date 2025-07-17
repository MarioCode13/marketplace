package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.StoreBranding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreBrandingRepository extends JpaRepository<StoreBranding, Long> {
    Optional<StoreBranding> findBySlug(String slug);
} 
package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.ReservedSlug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservedSlugRepository extends JpaRepository<ReservedSlug, UUID> {
    Optional<ReservedSlug> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<ReservedSlug> findAll();
}


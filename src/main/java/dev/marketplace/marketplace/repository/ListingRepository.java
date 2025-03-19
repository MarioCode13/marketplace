package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.Listing;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findByUserId(Long userId);
    List<Listing> findByCategoryId(Long categoryId);
    List<Listing> findBySoldFalse();

    @NotNull
    Page<Listing> findAll(@NotNull Pageable pageable);

    @Query("SELECT l FROM Listing l WHERE "
            + "(:categoryId IS NULL OR l.category.id = :categoryId) "
            + "AND (:minPrice IS NULL OR l.price >= :minPrice) "
            + "AND (:maxPrice IS NULL OR l.price <= :maxPrice)")
    Page<Listing> findFilteredListings(Long categoryId, Double minPrice, Double maxPrice, Pageable pageable);
}


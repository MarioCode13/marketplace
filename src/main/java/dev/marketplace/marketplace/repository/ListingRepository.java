package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.enums.Condition;
import dev.marketplace.marketplace.model.Listing;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    @Query("SELECT l FROM Listing l JOIN FETCH l.user WHERE l.user.id = :userId AND l.sold = false")
    List<Listing> findByUserId(Long userId);
    
    @Query("SELECT l FROM Listing l JOIN FETCH l.user WHERE l.user.id = :userId")
    List<Listing> findAllByUserId(Long userId);
    
    @Query("SELECT l FROM Listing l JOIN FETCH l.user WHERE l.category.id = :categoryId AND l.sold = false")
    List<Listing> findByCategoryId(Long categoryId);
    
    List<Listing> findBySoldFalse();

    @Query("SELECT l FROM Listing l WHERE l.sold = false")
    @NotNull
    Page<Listing> findAll(@NotNull Pageable pageable);

    // Enhanced filtering with all options
    @Query("SELECT l FROM Listing l WHERE "
            + "(:categoryId IS NULL OR l.category.id = :categoryId) "
            + "AND (:minPrice IS NULL OR l.price >= :minPrice) "
            + "AND (:maxPrice IS NULL OR l.price <= :maxPrice) "
            + "AND (:condition IS NULL OR l.condition = :condition) "
            + "AND (:cityId IS NULL OR l.city.id = :cityId) "
            + "AND (:searchTerm = '' OR l.title LIKE CONCAT('%', :searchTerm, '%') OR l.description LIKE CONCAT('%', :searchTerm, '%')) "
            + "AND (:userId IS NULL OR l.user.id = :userId) "
            + "AND l.sold = false")
    Page<Listing> findFilteredListings(
            @Param("categoryId") Long categoryId, 
            @Param("minPrice") Double minPrice, 
            @Param("maxPrice") Double maxPrice,
            @Param("condition") Condition condition,
            @Param("cityId") Long cityId,
            @Param("searchTerm") String searchTerm,
            @Param("userId") Long userId,
            Pageable pageable);

    // Legacy method for backward compatibility
    @Query("SELECT l FROM Listing l WHERE "
            + "(:categoryId IS NULL OR l.category.id = :categoryId) "
            + "AND (:minPrice IS NULL OR l.price >= :minPrice) "
            + "AND (:maxPrice IS NULL OR l.price <= :maxPrice) "
            + "AND l.sold = false")
    Page<Listing> findFilteredListings(Long categoryId, Double minPrice, Double maxPrice, Pageable pageable);

    @Query("SELECT l FROM Listing l JOIN FETCH l.user WHERE "
            + "(:categoryId IS NULL OR l.category.id = :categoryId) "
            + "AND (:minPrice IS NULL OR l.price >= :minPrice) "
            + "AND (:maxPrice IS NULL OR l.price <= :maxPrice)")
    List<Listing> findFilteredListingsWithUser(Long categoryId, Double minPrice, Double maxPrice);

    // Find listings by condition
    @Query("SELECT l FROM Listing l WHERE l.condition = :condition AND l.sold = false")
    Page<Listing> findByCondition(@Param("condition") Condition condition, Pageable pageable);

    // Find recent listings
    @Query("SELECT l FROM Listing l WHERE l.createdAt >= :since AND l.sold = false")
    Page<Listing> findRecentListings(@Param("since") LocalDateTime since, Pageable pageable);

    // Search listings by title or description
    @Query("SELECT l FROM Listing l WHERE "
            + "(l.title LIKE CONCAT('%', :searchTerm, '%') "
            + "OR l.description LIKE CONCAT('%', :searchTerm, '%')) "
            + "AND l.sold = false")
    Page<Listing> searchListings(@Param("searchTerm") String searchTerm, Pageable pageable);
}


package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByReviewedUserId(UUID reviewedUserId);

    List<Review> findByReviewerId(UUID reviewerId);

    List<Review> findByTransactionId(UUID transactionId);

    @Query("SELECT r FROM Review r WHERE r.reviewedUser.id = :userId AND r.isPositive = true")
    List<Review> findPositiveReviewsByUserId(@Param("userId") UUID userId);

    @Query("SELECT r FROM Review r WHERE r.reviewedUser.id = :userId AND r.isPositive = false")
    List<Review> findNegativeReviewsByUserId(@Param("userId") UUID userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewedUser.id = :userId")
    BigDecimal getAverageRatingByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewedUser.id = :userId")
    Long countReviewsByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewedUser.id = :userId AND r.isPositive = true")
    Long countPositiveReviewsByUserId(@Param("userId") UUID userId);

    @Query("SELECT r FROM Review r WHERE r.reviewer.id = :reviewerId AND r.transaction.id = :transactionId")
    Optional<Review> findByReviewerIdAndTransactionId(@Param("reviewerId") UUID reviewerId,
                                                      @Param("transactionId") UUID transactionId);

    @Query("SELECT r FROM Review r WHERE r.reviewedUser.id = :userId ORDER BY r.createdAt DESC")
    List<Review> findRecentReviewsByUserId(@Param("userId") UUID userId);

    @Query("SELECT r FROM Review r WHERE r.rating >= :minRating ORDER BY r.createdAt DESC")
    List<Review> findReviewsByMinimumRating(@Param("minRating") BigDecimal minRating);
    
    boolean existsByReviewerIdAndTransactionId(UUID reviewerId, UUID transactionId);

    @Query("SELECT r FROM Review r WHERE r.business.id = :businessId")
    List<Review> findByBusinessId(@Param("businessId") UUID businessId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.business.id = :businessId")
    BigDecimal getAverageRatingByBusinessId(@Param("businessId") UUID businessId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.business.id = :businessId")
    Long countReviewsByBusinessId(@Param("businessId") UUID businessId);
}

package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    List<Review> findByReviewedUserId(Long reviewedUserId);
    
    List<Review> findByReviewerId(Long reviewerId);
    
    List<Review> findByTransactionId(Long transactionId);
    
    @Query("SELECT r FROM Review r WHERE r.reviewedUser.id = :userId AND r.isPositive = true")
    List<Review> findPositiveReviewsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT r FROM Review r WHERE r.reviewedUser.id = :userId AND r.isPositive = false")
    List<Review> findNegativeReviewsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewedUser.id = :userId")
    BigDecimal getAverageRatingByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewedUser.id = :userId")
    Long countReviewsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewedUser.id = :userId AND r.isPositive = true")
    Long countPositiveReviewsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT r FROM Review r WHERE r.reviewer.id = :reviewerId AND r.transaction.id = :transactionId")
    Optional<Review> findByReviewerIdAndTransactionId(@Param("reviewerId") Long reviewerId, 
                                                      @Param("transactionId") Long transactionId);
    
    @Query("SELECT r FROM Review r WHERE r.reviewedUser.id = :userId ORDER BY r.createdAt DESC")
    List<Review> findRecentReviewsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT r FROM Review r WHERE r.rating >= :minRating ORDER BY r.createdAt DESC")
    List<Review> findReviewsByMinimumRating(@Param("minRating") BigDecimal minRating);
    
    boolean existsByReviewerIdAndTransactionId(Long reviewerId, Long transactionId);
} 
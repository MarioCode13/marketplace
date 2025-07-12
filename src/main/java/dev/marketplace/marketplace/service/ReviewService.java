package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.Review;
import dev.marketplace.marketplace.model.Transaction;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.ReviewRepository;
import dev.marketplace.marketplace.repository.TransactionRepository;
import dev.marketplace.marketplace.repository.ListingRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final TransactionRepository transactionRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final TrustRatingService trustRatingService;
    
    @Transactional
    public Review createReview(Long reviewerId, 
                             Long reviewedUserId, 
                             Long transactionId, 
                             BigDecimal rating, 
                             String comment) {
        log.info("Creating review: reviewer={}, reviewed={}, transaction={}, rating={}", 
                reviewerId, reviewedUserId, transactionId, rating);
        
        // Validate rating
        if (rating.compareTo(BigDecimal.valueOf(0.5)) < 0 || rating.compareTo(BigDecimal.valueOf(5.0)) > 0) {
            throw new IllegalArgumentException("Rating must be between 0.5 and 5.0");
        }
        
        // Check if review already exists for this transaction by this reviewer
        if (reviewRepository.existsByReviewerIdAndTransactionId(reviewerId, transactionId)) {
            throw new IllegalArgumentException("Review already exists for this transaction by this user");
        }
        
        // Validate users and transaction exist
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found with ID: " + reviewerId));
        
        User reviewedUser = userRepository.findById(reviewedUserId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewed user not found with ID: " + reviewedUserId));
        
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));
        
        // Validate that the transaction is completed
        if (transaction.getStatus() != Transaction.TransactionStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only review completed transactions");
        }
        
        // Validate that the reviewer is the buyer and reviewed user is the seller
        if (!transaction.getBuyer().getId().equals(reviewerId)) {
            throw new IllegalArgumentException("Only the buyer can review the seller");
        }
        
        if (!transaction.getSeller().getId().equals(reviewedUserId)) {
            throw new IllegalArgumentException("Can only review the seller of the transaction");
        }
        
        // Create review
        Review review = Review.builder()
                .reviewer(reviewer)
                .reviewedUser(reviewedUser)
                .transaction(transaction)
                .rating(rating)
                .comment(comment)
                .build();
        
        Review saved = reviewRepository.save(review);
        
        // Update trust rating for the reviewed user
        trustRatingService.calculateAndUpdateTrustRating(reviewedUserId);
        
        log.info("Review created successfully: {}", saved.getId());
        
        return saved;
    }
    
    @Transactional
    public Review updateReview(Long reviewId, Long reviewerId, BigDecimal rating, String comment) {
        log.info("Updating review: {}, by user: {}", reviewId, reviewerId);
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with ID: " + reviewId));
        
        // Check if user owns the review
        if (!review.getReviewer().getId().equals(reviewerId)) {
            throw new IllegalArgumentException("User not authorized to update this review");
        }
        
        // Validate rating
        if (rating.compareTo(BigDecimal.valueOf(0.5)) < 0 || rating.compareTo(BigDecimal.valueOf(5.0)) > 0) {
            throw new IllegalArgumentException("Rating must be between 0.5 and 5.0");
        }
        
        // Update review
        review.setRating(rating);
        review.setComment(comment);
        
        Review saved = reviewRepository.save(review);
        
        // Update trust rating for the reviewed user
        trustRatingService.calculateAndUpdateTrustRating(review.getReviewedUser().getId());
        
        log.info("Review updated successfully: {}", reviewId);
        
        return saved;
    }
    
    @Transactional
    public void deleteReview(Long reviewId, Long reviewerId) {
        log.info("Deleting review: {}, by user: {}", reviewId, reviewerId);
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with ID: " + reviewId));
        
        // Check if user owns the review
        if (!review.getReviewer().getId().equals(reviewerId)) {
            throw new IllegalArgumentException("User not authorized to delete this review");
        }
        
        Long reviewedUserId = review.getReviewedUser().getId();
        
        // Delete review
        reviewRepository.delete(review);
        
        // Update trust rating for the reviewed user
        trustRatingService.calculateAndUpdateTrustRating(reviewedUserId);
        
        log.info("Review deleted successfully: {}", reviewId);
    }
    
    @Transactional(readOnly = true)
    public List<Review> getUserReviews(Long userId) {
        return reviewRepository.findByReviewedUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public List<Review> getUserPositiveReviews(Long userId) {
        return reviewRepository.findPositiveReviewsByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public List<Review> getUserNegativeReviews(Long userId) {
        return reviewRepository.findNegativeReviewsByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public List<Review> getTransactionReviews(Long transactionId) {
        return reviewRepository.findByTransactionId(transactionId);
    }
    
    @Transactional(readOnly = true)
    public Optional<Review> getReview(Long reviewId) {
        return reviewRepository.findById(reviewId);
    }
    
    @Transactional(readOnly = true)
    public Optional<Review> getUserReviewForTransaction(Long reviewerId, Long transactionId) {
        return reviewRepository.findByReviewerIdAndTransactionId(reviewerId, transactionId);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getUserAverageRating(Long userId) {
        BigDecimal average = reviewRepository.getAverageRatingByUserId(userId);
        return average != null ? average : BigDecimal.ZERO;
    }
    
    @Transactional(readOnly = true)
    public Long getUserReviewCount(Long userId) {
        return reviewRepository.countReviewsByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public Long getUserPositiveReviewCount(Long userId) {
        return reviewRepository.countPositiveReviewsByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public List<Review> getRecentUserReviews(Long userId, int limit) {
        // This would need a custom query in the repository
        // For now, return all reviews and limit in service
        List<Review> reviews = reviewRepository.findRecentReviewsByUserId(userId);
        return reviews.stream().limit(limit).toList();
    }
    
    @Transactional(readOnly = true)
    public List<Review> getReviewsByMinimumRating(BigDecimal minRating) {
        return reviewRepository.findReviewsByMinimumRating(minRating);
    }
} 
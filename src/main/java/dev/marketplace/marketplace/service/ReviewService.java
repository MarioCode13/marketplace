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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final TransactionRepository transactionRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final TrustRatingService trustRatingService;
    private final BusinessService businessService;

    @Transactional
    public Review createReview(UUID reviewerId,
                             UUID reviewedUserId,
                             UUID transactionId,
                             BigDecimal rating,
                             String comment) {
        log.info("Creating review: reviewer={}, reviewed={}, transaction={}, rating={}", 
                reviewerId, reviewedUserId, transactionId, rating);

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

        if (transaction.getStatus() != Transaction.TransactionStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only review completed transactions");
        }
        
        // Validate that reviewer and reviewed user are opposite parties in the transaction
        boolean isBuyerReviewingSeller = transaction.getBuyer().getId().equals(reviewerId) &&
                                          transaction.getSeller().getId().equals(reviewedUserId);
        boolean isSellerReviewingBuyer = transaction.getSeller().getId().equals(reviewerId) &&
                                          transaction.getBuyer().getId().equals(reviewedUserId);

        if (!isBuyerReviewingSeller && !isSellerReviewingBuyer) {
            throw new IllegalArgumentException("Can only review the other party in this transaction");
        }

        Review review = Review.builder()
                .reviewer(reviewer)
                .reviewedUser(reviewedUser)
                .transaction(transaction)
                .rating(rating)
                .comment(comment)
                .build();
        Review savedReview = reviewRepository.save(review);

        // Recalculate trust rating for the reviewed user (seller)
        trustRatingService.calculateAndUpdateTrustRating(reviewedUserId);

        // Recalculate business trust rating if this is a business review
        if (savedReview.getBusiness() != null) {
            businessService.getBusinessTrustRating(savedReview.getBusiness().getId());
        }
        return savedReview;
    }
    
    @Transactional
    public Review updateReview(UUID reviewId, BigDecimal rating, String comment) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        review.setRating(rating);
        review.setComment(comment);
        Review updatedReview = reviewRepository.save(review);

        // Recalculate trust rating for the reviewed user
        trustRatingService.calculateAndUpdateTrustRating(updatedReview.getReviewedUser().getId());

        // Recalculate business trust rating if this is a business review
        if (updatedReview.getBusiness() != null) {
            businessService.getBusinessTrustRating(updatedReview.getBusiness().getId());
        }
        return updatedReview;
    }
    
    @Transactional
    public void deleteReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        UUID reviewedUserId = review.getReviewedUser().getId();
        UUID businessId = review.getBusiness() != null ? review.getBusiness().getId() : null;
        reviewRepository.delete(review);

        // Recalculate trust rating for the reviewed user
        trustRatingService.calculateAndUpdateTrustRating(reviewedUserId);

        // Recalculate business trust rating if this was a business review
        if (businessId != null) {
            businessService.getBusinessTrustRating(businessId);
        }
    }
    
    @Transactional(readOnly = true)
    public List<Review> getUserReviews(UUID userId) {
        return reviewRepository.findByReviewedUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public List<Review> getUserPositiveReviews(UUID userId) {
        return reviewRepository.findPositiveReviewsByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public List<Review> getUserNegativeReviews(UUID userId) {
        return reviewRepository.findNegativeReviewsByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public List<Review> getTransactionReviews(UUID transactionId) {
        return reviewRepository.findByTransactionId(transactionId);
    }
    
    @Transactional(readOnly = true)
    public Optional<Review> getReview(UUID reviewId) {
        return reviewRepository.findById(reviewId);
    }
    
    @Transactional(readOnly = true)
    public Optional<Review> getUserReviewForTransaction(UUID reviewerId, UUID transactionId) {
        return reviewRepository.findByReviewerIdAndTransactionId(reviewerId, transactionId);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getUserAverageRating(UUID userId) {
        BigDecimal average = reviewRepository.getAverageRatingByUserId(userId);
        return average != null ? average : BigDecimal.ZERO;
    }
    
    @Transactional(readOnly = true)
    public Long getUserReviewCount(UUID userId) {
        return reviewRepository.countReviewsByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public Long getUserPositiveReviewCount(UUID userId) {
        return reviewRepository.countPositiveReviewsByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public List<Review> getRecentUserReviews(UUID userId, int limit) {
        List<Review> reviews = reviewRepository.findRecentReviewsByUserId(userId);
        return reviews.stream().limit(limit).toList();
    }
    
    @Transactional(readOnly = true)
    public List<Review> getReviewsByMinimumRating(BigDecimal minRating) {
        return reviewRepository.findReviewsByMinimumRating(minRating);
    }
}

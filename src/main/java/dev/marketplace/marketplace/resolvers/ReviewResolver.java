package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.Review;
import dev.marketplace.marketplace.service.ReviewService;
import dev.marketplace.marketplace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class ReviewResolver {
    
    private final ReviewService reviewService;
    private final UserService userService;
    
    /**
     * Create a review for a completed transaction
     */
    @MutationMapping
    public Review createReview(@Argument UUID transactionId,
                              @Argument UUID reviewedUserId,
                              @Argument BigDecimal rating,
                              @Argument String comment,
                              @AuthenticationPrincipal UserDetails userDetails) {
        UUID reviewerId = userService.getUserIdByUsername(userDetails.getUsername());
        return reviewService.createReview(reviewerId, reviewedUserId, transactionId, rating, comment);
    }
    
    /**
     * Update a review
     */
    @MutationMapping
    public Review updateReview(@Argument UUID reviewId,
                              @Argument BigDecimal rating,
                              @Argument String comment,
                              @AuthenticationPrincipal UserDetails userDetails) {
        return reviewService.updateReview(reviewId, rating, comment);
    }
    
    /**
     * Delete a review
     */
    @MutationMapping
    public boolean deleteReview(@Argument UUID reviewId,
                               @AuthenticationPrincipal UserDetails userDetails) {
        reviewService.deleteReview(reviewId);
        return true;
    }
    
    /**
     * Get reviews for a user
     */
    @QueryMapping
    public List<Review> getUserReviews(@Argument UUID userId) {
        return reviewService.getUserReviews(userId);
    }
    
    /**
     * Get positive reviews for a user
     */
    @QueryMapping
    public List<Review> getUserPositiveReviews(@Argument UUID userId) {
        return reviewService.getUserPositiveReviews(userId);
    }
    
    /**
     * Get negative reviews for a user
     */
    @QueryMapping
    public List<Review> getUserNegativeReviews(@Argument UUID userId) {
        return reviewService.getUserNegativeReviews(userId);
    }
    
    /**
     * Get reviews for a transaction
     */
    @QueryMapping
    public List<Review> getTransactionReviews(@Argument UUID transactionId) {
        return reviewService.getTransactionReviews(transactionId);
    }
    
    /**
     * Get a specific review
     */
    @QueryMapping
    public Review getReview(@Argument UUID reviewId) {
        return reviewService.getReview(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with ID: " + reviewId));
    }
    
    /**
     * Get current user's review for a specific transaction
     */
    @QueryMapping
    public Review getMyReviewForTransaction(@Argument UUID transactionId,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        UUID reviewerId = userService.getUserIdByUsername(userDetails.getUsername());
        return reviewService.getUserReviewForTransaction(reviewerId, transactionId)
                .orElse(null);
    }
    /**
     * Get average rating for a user
     */
    @QueryMapping
    public BigDecimal getUserAverageRating(@Argument UUID userId) {
        return reviewService.getUserAverageRating(userId);
    }
    /**
     * Get review count for a user
     */
    @QueryMapping
    public Long getUserReviewCount(@Argument UUID userId) {
        return reviewService.getUserReviewCount(userId);
    }
    /**
     * Get positive review count for a user
     */
    @QueryMapping
    public Long getUserPositiveReviewCount(@Argument UUID userId) {
        return reviewService.getUserPositiveReviewCount(userId);
    }
    /**
     * Get recent reviews for a user
     */
    @QueryMapping
    public List<Review> getRecentUserReviews(@Argument UUID userId, @Argument Integer limit) {
        return reviewService.getRecentUserReviews(userId, limit != null ? limit : 10);
    }
    /**
     * Get reviews by minimum rating
     */
    @QueryMapping
    public List<Review> getReviewsByMinimumRating(@Argument BigDecimal minRating) {
        return reviewService.getReviewsByMinimumRating(minRating);
    }
    /**
     * Get reviews for a user (reviews they received)
     */
    @QueryMapping
    public List<Review> reviewsByUser(@Argument UUID userId) {
        return reviewService.getUserReviews(userId);
    }
}

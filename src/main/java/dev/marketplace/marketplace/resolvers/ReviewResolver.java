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

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ReviewResolver {
    
    private final ReviewService reviewService;
    private final UserService userService;
    
    /**
     * Create a review for a completed transaction
     */
    @MutationMapping
    public Review createReview(@Argument Long transactionId,
                              @Argument Long reviewedUserId,
                              @Argument BigDecimal rating,
                              @Argument String comment,
                              @AuthenticationPrincipal UserDetails userDetails) {
        Long reviewerId = userService.getUserIdByUsername(userDetails.getUsername());
        return reviewService.createReview(reviewerId, reviewedUserId, transactionId, rating, comment);
    }
    
    /**
     * Update a review
     */
    @MutationMapping
    public Review updateReview(@Argument Long reviewId,
                              @Argument BigDecimal rating,
                              @Argument String comment,
                              @AuthenticationPrincipal UserDetails userDetails) {
        Long reviewerId = userService.getUserIdByUsername(userDetails.getUsername());
        return reviewService.updateReview(reviewId, reviewerId, rating, comment);
    }
    
    /**
     * Delete a review
     */
    @MutationMapping
    public boolean deleteReview(@Argument Long reviewId,
                               @AuthenticationPrincipal UserDetails userDetails) {
        Long reviewerId = userService.getUserIdByUsername(userDetails.getUsername());
        reviewService.deleteReview(reviewId, reviewerId);
        return true;
    }
    
    /**
     * Get reviews for a user
     */
    @QueryMapping
    public List<Review> getUserReviews(@Argument Long userId) {
        return reviewService.getUserReviews(userId);
    }
    
    /**
     * Get positive reviews for a user
     */
    @QueryMapping
    public List<Review> getUserPositiveReviews(@Argument Long userId) {
        return reviewService.getUserPositiveReviews(userId);
    }
    
    /**
     * Get negative reviews for a user
     */
    @QueryMapping
    public List<Review> getUserNegativeReviews(@Argument Long userId) {
        return reviewService.getUserNegativeReviews(userId);
    }
    
    /**
     * Get reviews for a transaction
     */
    @QueryMapping
    public List<Review> getTransactionReviews(@Argument Long transactionId) {
        return reviewService.getTransactionReviews(transactionId);
    }
    
    /**
     * Get a specific review
     */
    @QueryMapping
    public Review getReview(@Argument Long reviewId) {
        return reviewService.getReview(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with ID: " + reviewId));
    }
    
    /**
     * Get current user's review for a specific transaction
     */
    @QueryMapping
    public Review getMyReviewForTransaction(@Argument Long transactionId,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        Long reviewerId = userService.getUserIdByUsername(userDetails.getUsername());
        return reviewService.getUserReviewForTransaction(reviewerId, transactionId)
                .orElse(null);
    }
    
    /**
     * Get average rating for a user
     */
    @QueryMapping
    public BigDecimal getUserAverageRating(@Argument Long userId) {
        return reviewService.getUserAverageRating(userId);
    }
    
    /**
     * Get review count for a user
     */
    @QueryMapping
    public Long getUserReviewCount(@Argument Long userId) {
        return reviewService.getUserReviewCount(userId);
    }
    
    /**
     * Get positive review count for a user
     */
    @QueryMapping
    public Long getUserPositiveReviewCount(@Argument Long userId) {
        return reviewService.getUserPositiveReviewCount(userId);
    }
    
    /**
     * Get recent reviews for a user
     */
    @QueryMapping
    public List<Review> getRecentUserReviews(@Argument Long userId, @Argument Integer limit) {
        return reviewService.getRecentUserReviews(userId, limit != null ? limit : 10);
    }
    
    /**
     * Get reviews by minimum rating
     */
    @QueryMapping
    public List<Review> getReviewsByMinimumRating(@Argument BigDecimal minRating) {
        return reviewService.getReviewsByMinimumRating(minRating);
    }
} 
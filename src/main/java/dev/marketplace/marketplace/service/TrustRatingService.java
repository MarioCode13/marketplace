package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.*;
import dev.marketplace.marketplace.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

/**
 * Trust rating for users: single verification attribute (Omnicheck ID verification only)
 * and composite scores for profile, reviews, transactions, subscription.
 *
 * <h3>Current behaviour (simplified)</h3>
 * <ul>
 *   <li><b>verifiedId</b> (boolean): The only verification attribute. Set to true when the user
 *       completes Omnicheck ID verification successfully. No document upload/approval verification.</li>
 *   <li><b>verificationScore</b> (0–100): Derived from verifiedId only: 100 if verifiedId is true, 0 otherwise.</li>
 *   <li><b>profileScore</b>: See "Profile score" below.</li>
 *   <li><b>reviewScore</b>: 70% positive-review percentage + 30% average star rating (as % of 5).</li>
 *   <li><b>transactionScore</b>: Percentage of completed transactions (as buyer + seller).</li>
 *   <li><b>subscriptionScore</b>: 100 if user has SUBSCRIBED role, else 0.</li>
 *   <li><b>overallScore</b>: Simple average of profile, verification, review, transaction, subscription (each 0–100).</li>
 * </ul>
 *
 * <h3>Profile score (what it consists of)</h3>
 * Profile score is the percentage of these four profile fields that are filled:
 * <ol>
 *   <li>Profile photo (profileImageUrl set)</li>
 *   <li>Bio (bio non-empty)</li>
 *   <li>Contact number (contactNumber non-empty)</li>
 *   <li>Location (city or customCity set)</li>
 * </ol>
 * Document uploads (ID, driver's license, proof of address, etc.) are not part of profile score or verification.
 *
 * <h3>Improved calculation (implemented)</h3>
 * Verification is binary (Omnicheck only): verificationScore = verifiedId ? 100 : 0.
 * Profile score uses only the four profile fields above (no document counts).
 * Overall remains the average of the five 0–100 components for a stable 0–100 scale.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrustRatingService {
    
    private final TrustRatingRepository trustRatingRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final ProfileCompletionRepository profileCompletionRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BusinessTrustRatingRepository businessTrustRatingRepository;
    private final BusinessRepository businessRepository;

    @Transactional
    public TrustRating calculateAndUpdateTrustRating(UUID userId) {
        log.info("Calculating trust rating for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        // Get or create trust rating
        TrustRating trustRating = trustRatingRepository.findByUserId(userId)
                .orElse(TrustRating.builder().user(user).build());

        // Verification: only Omnicheck ID verification (verifiedId). No document verification.
        BigDecimal verificationScore = trustRating.getVerifiedId()
                ? BigDecimal.valueOf(100)
                : BigDecimal.ZERO;
        log.debug("User {}: verifiedID={}, verificationScore={}", userId, trustRating.getVerifiedId(), verificationScore);

        // Calculate other scores
        BigDecimal profileCompletionScore = calculateProfileCompletionScore(userId);
        BigDecimal reviewScore = calculateReviewScore(userId);
        BigDecimal transactionScore = calculateTransactionScore(userId);
        BigDecimal subscriptionScore = calculateSubscriptionScore(userId);

        log.debug("User {}: profileScore={}, reviewScore={}, transactionScore={}, subscriptionScore={}",
            userId, profileCompletionScore, reviewScore, transactionScore, subscriptionScore);

        // Overall = average of five 0-100 components
        BigDecimal overallScore = profileCompletionScore
                .add(verificationScore)
                .add(reviewScore)
                .add(transactionScore)
                .add(subscriptionScore)
                .divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP);

        // Update trust rating
        trustRating.setProfileScore(profileCompletionScore);
        trustRating.setVerificationScore(verificationScore);
        trustRating.setReviewScore(reviewScore);
        trustRating.setTransactionScore(transactionScore);
        trustRating.setOverallScore(overallScore);

        // Update review counts
        Long totalReviews = reviewRepository.countReviewsByUserId(userId);
        Long positiveReviews = reviewRepository.countPositiveReviewsByUserId(userId);
        trustRating.setTotalReviews(totalReviews.intValue());
        trustRating.setPositiveReviews(positiveReviews.intValue());

        // TODO: Update transaction counts if implemented
        trustRating.setTotalTransactions(0);
        trustRating.setSuccessfulTransactions(0);

        TrustRating saved = trustRatingRepository.save(trustRating);
        log.info("Updated trust rating for user {}: overall score = {}", userId, overallScore);

        return saved;
    }
    
    /**
     * Profile score: percentage of four profile fields that are filled (photo, bio, contact, location).
     * Document uploads are not part of profile score.
     *
     * NOTE: Always check User entity directly for authoritative profile data.
     * ProfileCompletion is a cache that should be updated via updateProfileCompletion(),
     * but we don't rely on it for calculations to avoid stale data issues.
     */
    private BigDecimal calculateProfileCompletionScore(UUID userId) {
        int totalFields = 4;
        int completedFields = 0;

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            boolean hasPhoto = user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty();
            boolean hasBio = user.getBio() != null && !user.getBio().isEmpty();
            boolean hasContact = user.getContactNumber() != null && !user.getContactNumber().isEmpty();
            boolean hasLocation = user.getCity() != null || (user.getCustomCity() != null && !user.getCustomCity().isEmpty());

            if (hasPhoto) completedFields++;
            if (hasBio) completedFields++;
            if (hasContact) completedFields++;
            if (hasLocation) completedFields++;

            log.debug("User {}: profile fields - photo={}, bio={}, contact={}, location={}, completed={}/{}",
                userId, hasPhoto, hasBio, hasContact, hasLocation, completedFields, totalFields);
        }

        if (totalFields == 0) return BigDecimal.ZERO;
        BigDecimal score = BigDecimal.valueOf(completedFields).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalFields), 2, RoundingMode.HALF_UP);
        log.debug("User {}: calculated profileScore={}", userId, score);
        return score;
    }

    // Review score (same as before)
    private BigDecimal calculateReviewScore(UUID userId) {
        Long totalReviews = reviewRepository.countReviewsByUserId(userId);
        if (totalReviews == 0) {
            return BigDecimal.ZERO;
        }
        Long positiveReviews = reviewRepository.countPositiveReviewsByUserId(userId);
        BigDecimal positivePercentage = BigDecimal.valueOf(positiveReviews)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalReviews), 2, RoundingMode.HALF_UP);
        BigDecimal averageRating = reviewRepository.getAverageRatingByUserId(userId);
        if (averageRating == null) {
            averageRating = BigDecimal.ZERO;
        }
        BigDecimal ratingPercentage = averageRating.multiply(BigDecimal.valueOf(20)); // 5 stars = 100%
        // Weight: 70% positive percentage, 30% average rating
        return positivePercentage.multiply(BigDecimal.valueOf(0.7))
                .add(ratingPercentage.multiply(BigDecimal.valueOf(0.3)));
    }

    // Transaction score (ratio of successful to total, or 0 if no transactions)
    private BigDecimal calculateTransactionScore(UUID userId) {
        // Count total and successful transactions as seller and buyer
        long totalTransactions = 0;
        long successfulTransactions = 0;
        // As seller
        totalTransactions += transactionRepository.countBySellerId(userId);
        successfulTransactions += transactionRepository.countBySellerIdAndStatus(userId, dev.marketplace.marketplace.model.Transaction.TransactionStatus.COMPLETED);
        // As buyer
        totalTransactions += transactionRepository.countByBuyerId(userId);
        successfulTransactions += transactionRepository.countByBuyerIdAndStatus(userId, dev.marketplace.marketplace.model.Transaction.TransactionStatus.COMPLETED);
        if (totalTransactions == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(successfulTransactions)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(totalTransactions), 2, RoundingMode.HALF_UP);
    }

    // Subscription score: 100 if subscribed, 0 if not
    private BigDecimal calculateSubscriptionScore(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getRole() == dev.marketplace.marketplace.enums.Role.SUBSCRIBED) {
            return BigDecimal.valueOf(100);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Add subscription bonus to user's trust rating
     */
    @Transactional
    public void addSubscriptionBonus(UUID userId) {
        log.info("Adding subscription bonus to user: {}", userId);
        calculateAndUpdateTrustRating(userId);
    }

    /**
     * Remove subscription bonus from user's trust rating
     */
    @Transactional
    public void removeSubscriptionBonus(UUID userId) {
        log.info("Removing subscription bonus from user: {}", userId);
        calculateAndUpdateTrustRating(userId);
    }
    
    @Transactional
    public TrustRating getTrustRating(UUID userId) {
        return trustRatingRepository.findByUserId(userId)
                .orElseGet(() -> calculateAndUpdateTrustRating(userId));
    }

    /**
     * Mark a user's ID as verified. Creates a TrustRating if missing, marks verifiedID=true
     * and recalculates the trust rating.
     */
    @Transactional
    public void markVerifiedID(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        TrustRating trustRating = trustRatingRepository.findByUserId(userId)
                .orElse(TrustRating.builder().user(user).build());

        trustRating.setVerifiedID(true);
        trustRatingRepository.save(trustRating);

        // Recalculate full trust rating after marking verified ID
        calculateAndUpdateTrustRating(userId);
    }

    @Transactional
    public void updateProfileCompletion(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        ProfileCompletion profileCompletion = profileCompletionRepository.findByUserId(userId)
                .orElse(ProfileCompletion.builder().user(user).build());
        
        // Update completion flags based on user data
        profileCompletion.setHasProfilePhoto(user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty());
        profileCompletion.setHasBio(user.getBio() != null && !user.getBio().isEmpty());
        profileCompletion.setHasContactNumber(user.getContactNumber() != null && !user.getContactNumber().isEmpty());
        // City is an entity; consider location present if city is set or customCity string is non-empty
        boolean hasLocation = user.getCity() != null || (user.getCustomCity() != null && !user.getCustomCity().isEmpty());
        profileCompletion.setHasLocation(hasLocation);
        // Ensure relation is set in case this is a newly-created ProfileCompletion
        profileCompletion.setUser(user);
        // No more verified/verification fields
        // Calculate completion percentage
        profileCompletion.calculateCompletionPercentage();

        profileCompletionRepository.save(profileCompletion);
        
        // Recalculate trust rating
        calculateAndUpdateTrustRating(userId);
    }
    
    public BigDecimal getAverageTrustScore() {
        BigDecimal average = trustRatingRepository.getAverageTrustScore();
        return average != null ? average : BigDecimal.ZERO;
    }
    
    public Long getUsersWithMinimumTrustScore(BigDecimal minScore) {
        return trustRatingRepository.countByMinimumScore(minScore);
    }

    @Transactional
    public BusinessTrustRating calculateAndUpdateBusinessTrustRating(UUID businessId) {
        log.info("Calculating trust rating for business: {}", businessId);
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found with ID: " + businessId));
        BusinessTrustRating trustRating = businessTrustRatingRepository.findByBusinessId(businessId)
                .orElse(BusinessTrustRating.builder().business(business).build());
        // Calculate scores
        BigDecimal profileScore = calculateBusinessProfileScore(business);
        BigDecimal verificationScore = calculateBusinessVerificationScore(businessId);
        BigDecimal reviewScore = calculateBusinessReviewScore(businessId);
        BigDecimal transactionScore = calculateBusinessTransactionScore(businessId);
        // Average for overall
        BigDecimal overallScore = profileScore.add(verificationScore).add(reviewScore).add(transactionScore)
                .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
        trustRating.setProfileScore(profileScore);
        trustRating.setVerificationScore(verificationScore);
        trustRating.setReviewScore(reviewScore);
        trustRating.setTransactionScore(transactionScore);
        trustRating.setOverallScore(overallScore);
        trustRating.setLastCalculated(java.time.LocalDateTime.now());
        // TODO: Set review/transaction counts if available
        BusinessTrustRating saved = businessTrustRatingRepository.save(trustRating);
        log.info("Updated business trust rating for business {}: overall score = {}", businessId, overallScore);
        return saved;
    }
    // Helper methods for business trust rating
    private BigDecimal calculateBusinessProfileScore(Business business) {
        int totalFields = 4;
        int completedFields = 0;
        String logoUrl = business.getStoreBranding() != null ? business.getStoreBranding().getLogoUrl() : null;
        String about = business.getStoreBranding() != null ? business.getStoreBranding().getAbout() : null;
        if (logoUrl != null && !logoUrl.isEmpty()) completedFields++;
        if (about != null && !about.isEmpty()) completedFields++;
        if (business.getContactNumber() != null && !business.getContactNumber().isEmpty()) completedFields++;
        if (business.getAddressLine1() != null && !business.getAddressLine1().isEmpty()) completedFields++;
        return BigDecimal.valueOf(completedFields).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalFields), 2, RoundingMode.HALF_UP);
    }
    private BigDecimal calculateBusinessVerificationScore(UUID businessId) {
        int totalDocs = 0;
        int verifiedDocs = 0;
        for (VerificationDocument.DocumentType type : VerificationDocument.DocumentType.values()) {
            Optional<VerificationDocument> doc = verificationDocumentRepository.findByBusinessIdAndDocumentType(businessId, type);
            if (doc.isPresent()) {
                totalDocs++;
                if (doc.get().getStatus() == VerificationDocument.VerificationStatus.APPROVED) verifiedDocs++;
            }
        }
        if (totalDocs == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(verifiedDocs).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalDocs), 2, RoundingMode.HALF_UP);
    }
    private BigDecimal calculateBusinessReviewScore(UUID businessId) {
        // Placeholder: implement if business reviews are available
        return BigDecimal.ZERO;
    }
    private BigDecimal calculateBusinessTransactionScore(UUID businessId) {
        // Placeholder: implement if business transactions are available
        return BigDecimal.ZERO;
    }
}

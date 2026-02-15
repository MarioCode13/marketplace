package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.*;
import dev.marketplace.marketplace.repository.*;
import dev.marketplace.marketplace.service.dto.TrustComponentsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Trust rating for users: single verification attribute (Omnicheck ID verification only)
 * and composite scores for profile, reviews, transactions, subscription.
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
    private final ListingRepository listingRepository;

    @Transactional
    public TrustRating calculateAndUpdateTrustRating(UUID userId) {
        log.info("Calculating trust rating for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        // Get or create trust rating
        TrustRating trustRating = trustRatingRepository.findByUserId(userId)
                .orElse(TrustRating.builder().user(user).build());

        // Always use the new model now
        TrustComponentsDTO components = calculateTrustComponents(userId);
        trustRating.setReviewScore(components.getReviewScore());
        trustRating.setTransactionScore(components.getTransactionScore());
        trustRating.setProfileScore(components.getProfileScore());
        trustRating.setVerificationScore(components.getVerificationScore());
        trustRating.setOverallScore(components.getOverallScore());

        // Update counts (unchanged)
        Long totalReviews = reviewRepository.countReviewsByUserId(userId);
        Long positiveReviews = reviewRepository.countPositiveReviewsByUserId(userId);
        trustRating.setTotalReviews(totalReviews.intValue());
        trustRating.setPositiveReviews(positiveReviews.intValue());

        long totalTransactions = transactionRepository.countBySellerId(userId) + transactionRepository.countByBuyerId(userId);
        long successfulTransactions = transactionRepository.countBySellerIdAndStatus(userId, dev.marketplace.marketplace.model.Transaction.TransactionStatus.COMPLETED)
                + transactionRepository.countByBuyerIdAndStatus(userId, dev.marketplace.marketplace.model.Transaction.TransactionStatus.COMPLETED);
        trustRating.setTotalTransactions((int) totalTransactions);
        trustRating.setSuccessfulTransactions((int) successfulTransactions);

        TrustRating saved = trustRatingRepository.save(trustRating);
        log.info("Updated trust rating (new model) for user {}: overall score = {}", userId, components.getOverallScore());
        return saved;
    }
    
    /**
     * Public API: calculate the new TrustComponentsDTO using the improved model.
     * This method does NOT persist any changes; it is a pure calculation helper that
     * fetches necessary aggregates from repositories and returns the computed components.
     */
    public TrustComponentsDTO calculateTrustComponents(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Profile score
        BigDecimal profileScore = calculateProfileCompletionScore(userId);
        // Verification score: keep existing semantics: binary Omnicheck ID
        TrustRating existing = trustRatingRepository.findByUserId(userId).orElse(null);
        BigDecimal verificationScore = (existing != null && existing.getVerifiedId()) ? BigDecimal.valueOf(70) : BigDecimal.ZERO;
        // Review score: Bayesian-weighted score mapped to 0-100
        BigDecimal reviewScore = calculateBayesianReviewScoreForUser(userId);
        // Transaction score: completion rate + log-volume boost (capped)
        BigDecimal transactionScore = calculateTransactionScoreWithVolumeBoost(userId);

        // If subscription exists, keep existing subscription handling as 100/0 for now (not part of new model doc)
        BigDecimal subscriptionScore = calculateSubscriptionScore(userId);

        // Derive overall using new weights from the model doc:
        // review 35%, transactions 30%, verification 20%, profile 15% (sum=100)
        BigDecimal overall = reviewScore.multiply(BigDecimal.valueOf(0.35))
                .add(transactionScore.multiply(BigDecimal.valueOf(0.30)))
                .add(verificationScore.multiply(BigDecimal.valueOf(0.20)))
                .add(profileScore.multiply(BigDecimal.valueOf(0.15)));
        // Round to scale 2
        overall = overall.setScale(2, RoundingMode.HALF_UP);

        // Ensure component scales
        reviewScore = reviewScore.setScale(2, RoundingMode.HALF_UP);
        transactionScore = transactionScore.setScale(2, RoundingMode.HALF_UP);
        verificationScore = verificationScore.setScale(2, RoundingMode.HALF_UP);
        profileScore = profileScore.setScale(2, RoundingMode.HALF_UP);

        return new TrustComponentsDTO(reviewScore, transactionScore, verificationScore, profileScore, overall);
    }

    /**
     * New Bayesian review score helper. Implements weighted average: (v/(v+m))*R + (m/(v+m))*C
     * where v = number of reviews for the user, R = user's average rating (0-5),
     * C = global average rating (0-5), m = prior weight (default 10). The result is mapped to 0-100.
     *
     * For new users with zero reviews: returns global average mapped to 0-100 (around 84 if global avg is 4.2).
     */
    private BigDecimal calculateBayesianReviewScoreForUser(UUID userId) {
        long v = Optional.ofNullable(reviewRepository.countReviewsByUserId(userId)).orElse(0L);
        BigDecimal R = Optional.ofNullable(reviewRepository.getAverageRatingByUserId(userId)).orElse(BigDecimal.ZERO);
        // Global average: use 4.2 as default (reasonable marketplace average)
        BigDecimal C = Optional.ofNullable(reviewRepository.getGlobalAverageRating()).orElse(BigDecimal.valueOf(4.2));
        int m = 10; // prior weight (minimum number of reviews to reach user's true average)

        // If no reviews, apply Bayesian formula with zero user rating
        // This gives: (0/(0+10))*0 + (10/(0+10))*4.2 = 4.2
        // Mapped to 0-100: 4.2 * 20 = 84.00
        if (v == 0) {
            // Use full Bayesian formula even with zero reviews
            // weightedRating = (0/10)*0 + (10/10)*C = C
            return C.multiply(BigDecimal.valueOf(20)).setScale(2, RoundingMode.HALF_UP);
        }

        // User has reviews: apply Bayesian weighting
        // weightedRating = (v/(v+m))*R + (m/(v+m))*C
        BigDecimal bdV = BigDecimal.valueOf(v);
        BigDecimal weighted = bdV.divide(bdV.add(BigDecimal.valueOf(m)), 10, RoundingMode.HALF_UP)
                .multiply(R).add(
                        BigDecimal.valueOf(m).divide(bdV.add(BigDecimal.valueOf(m)), 10, RoundingMode.HALF_UP).multiply(C)
                );
        // Map 0-5 to 0-100
        return weighted.multiply(BigDecimal.valueOf(20));
    }

    /**
     * New transaction score: completionRate*60 + min(log10(totalTx+1)*15, 15)
     * completionRate is successful/total as 0-100. volume boost uses log10(totalTx+1)
     * to produce diminishing returns; cap boost at 15 points.
     */
    private BigDecimal calculateTransactionScoreWithVolumeBoost(UUID userId) {
        long totalTransactions = transactionRepository.countBySellerId(userId) + transactionRepository.countByBuyerId(userId);
        long successfulTransactions = transactionRepository.countBySellerIdAndStatus(userId, dev.marketplace.marketplace.model.Transaction.TransactionStatus.COMPLETED)
                + transactionRepository.countByBuyerIdAndStatus(userId, dev.marketplace.marketplace.model.Transaction.TransactionStatus.COMPLETED);
        if (totalTransactions == 0) return BigDecimal.ZERO;
        BigDecimal completionRate = BigDecimal.valueOf(successfulTransactions)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalTransactions), 10, RoundingMode.HALF_UP);
        // completion component: scale to 0-60
        BigDecimal completionComponent = completionRate.multiply(BigDecimal.valueOf(0.6));
        // volume boost
        double logBoost = Math.log10((double) totalTransactions + 1.0) * 15.0;
        double capped = Math.min(logBoost, 15.0);
        BigDecimal volumeComponent = BigDecimal.valueOf(capped);
        BigDecimal total = completionComponent.add(volumeComponent).setScale(2, RoundingMode.HALF_UP);
        // Ensure it doesn't exceed 100
        if (total.compareTo(BigDecimal.valueOf(100)) > 0) return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        return total;
    }

    /**
     * Keep existing profile completion calculator (percentage of 4 fields)
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

        BigDecimal score = BigDecimal.valueOf(completedFields).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalFields), 2, RoundingMode.HALF_UP);
        log.debug("User {}: calculated profileScore={}", userId, score);
        return score;
    }
    /*
     * Removed legacy review/transaction calculators to fully switch to the new model.
     */

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
    
    /**
     * Public API: calculate the new BusinessTrustComponentsDTO using the improved model.
     * This method does NOT persist any changes; it is a pure calculation helper.
     */
    public dev.marketplace.marketplace.service.dto.BusinessTrustComponentsDTO calculateBusinessComponents(UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found with ID: " + businessId));

        // Calculate components
        BigDecimal profileScore = calculateBusinessProfileScore(business);
        BigDecimal verificationScore = calculateBusinessVerificationScore(businessId);
        BigDecimal reviewScore = calculateBusinessReviewScore(businessId);
        BigDecimal transactionScore = calculateBusinessTransactionScore(businessId);

        // Derive overall using weighted formula
        BigDecimal overall = reviewScore.multiply(BigDecimal.valueOf(0.35))
                .add(transactionScore.multiply(BigDecimal.valueOf(0.30)))
                .add(verificationScore.multiply(BigDecimal.valueOf(0.20)))
                .add(profileScore.multiply(BigDecimal.valueOf(0.15)))
                .setScale(2, RoundingMode.HALF_UP);

        // Ensure component scales
        reviewScore = reviewScore.setScale(2, RoundingMode.HALF_UP);
        transactionScore = transactionScore.setScale(2, RoundingMode.HALF_UP);
        verificationScore = verificationScore.setScale(2, RoundingMode.HALF_UP);
        profileScore = profileScore.setScale(2, RoundingMode.HALF_UP);

        return new dev.marketplace.marketplace.service.dto.BusinessTrustComponentsDTO(reviewScore, transactionScore, verificationScore, profileScore, overall);
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
        
        // Calculate using new weighted model
        BigDecimal profileScore = calculateBusinessProfileScore(business);
        BigDecimal verificationScore = calculateBusinessVerificationScore(businessId);
        BigDecimal reviewScore = calculateBusinessReviewScore(businessId);
        BigDecimal transactionScore = calculateBusinessTransactionScore(businessId);
        
        // Weighted overall: review 35%, transaction 30%, verification 20%, profile 15%
        BigDecimal overallScore = reviewScore.multiply(BigDecimal.valueOf(0.35))
                .add(transactionScore.multiply(BigDecimal.valueOf(0.30)))
                .add(verificationScore.multiply(BigDecimal.valueOf(0.20)))
                .add(profileScore.multiply(BigDecimal.valueOf(0.15)))
                .setScale(2, RoundingMode.HALF_UP);
        
        // Update counts
        long totalReviews = Optional.ofNullable(reviewRepository.countReviewsByBusinessId(businessId)).orElse(0L);
        
        // Get all listings for the business to count transactions
        List<Listing> listings = listingRepository.findByBusinessId(businessId);
        long totalTransactions = 0;
        long successfulTransactions = 0;
        if (listings != null && !listings.isEmpty()) {
            List<UUID> listingIds = listings.stream().map(Listing::getId).toList();
            List<dev.marketplace.marketplace.model.Transaction> transactions = transactionRepository.findByListingIdIn(listingIds);
            totalTransactions = transactions.size();
            successfulTransactions = transactions.stream()
                    .filter(t -> t.getStatus() == dev.marketplace.marketplace.model.Transaction.TransactionStatus.COMPLETED)
                    .count();
        }
        
        trustRating.setProfileScore(profileScore);
        trustRating.setVerificationScore(verificationScore);
        trustRating.setReviewScore(reviewScore);
        trustRating.setTransactionScore(transactionScore);
        trustRating.setOverallScore(overallScore);
        trustRating.setTotalReviews((int) totalReviews);
        trustRating.setTotalTransactions((int) totalTransactions);
        trustRating.setSuccessfulTransactions((int) successfulTransactions);
        trustRating.setLastCalculated(java.time.LocalDateTime.now());
        
        BusinessTrustRating saved = businessTrustRatingRepository.save(trustRating);
        log.info("Updated business trust rating (new model) for business {}: overall score = {}", businessId, overallScore);
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
        long v = Optional.ofNullable(reviewRepository.countReviewsByBusinessId(businessId)).orElse(0L);
        BigDecimal R = Optional.ofNullable(reviewRepository.getAverageRatingByBusinessId(businessId)).orElse(BigDecimal.ZERO);
        // Global average: use 4.2 as default (consistent with user trust)
        BigDecimal C = Optional.ofNullable(reviewRepository.getGlobalAverageRating()).orElse(BigDecimal.valueOf(4.2));
        int m = 10; // prior weight

        // If no reviews, apply Bayesian formula with zero rating
        // This gives: 4.2 * 20 = 84.00
        if (v == 0) {
            return C.multiply(BigDecimal.valueOf(20)).setScale(2, RoundingMode.HALF_UP);
        }

        // Business has reviews: apply Bayesian weighting
        // weightedRating = (v/(v+m))*R + (m/(v+m))*C
        BigDecimal bdV = BigDecimal.valueOf(v);
        BigDecimal weighted = bdV.divide(bdV.add(BigDecimal.valueOf(m)), 10, RoundingMode.HALF_UP)
                .multiply(R).add(
                        BigDecimal.valueOf(m).divide(bdV.add(BigDecimal.valueOf(m)), 10, RoundingMode.HALF_UP).multiply(C)
                );
        // Map 0-5 to 0-100
        return weighted.multiply(BigDecimal.valueOf(20));
    }
    private BigDecimal calculateBusinessTransactionScore(UUID businessId) {
        // Get all listings for the business
        List<Listing> listings = listingRepository.findByBusinessId(businessId);
        if (listings == null || listings.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Get all transactions for these listings
        List<UUID> listingIds = listings.stream().map(Listing::getId).toList();
        List<dev.marketplace.marketplace.model.Transaction> transactions = transactionRepository.findByListingIdIn(listingIds);
        
        if (transactions == null || transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long totalTransactions = transactions.size();
        long successfulTransactions = transactions.stream()
                .filter(t -> t.getStatus() == dev.marketplace.marketplace.model.Transaction.TransactionStatus.COMPLETED)
                .count();

        // Completion rate component: (successful / total) * 60 (0-60 points)
        BigDecimal completionRate = BigDecimal.valueOf(successfulTransactions)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalTransactions), 10, RoundingMode.HALF_UP);
        BigDecimal completionComponent = completionRate.multiply(BigDecimal.valueOf(0.6));

        // Volume boost: min(log10(total+1)*15, 15) (0-15 points)
        double logBoost = Math.log10((double) totalTransactions + 1.0) * 15.0;
        double capped = Math.min(logBoost, 15.0);
        BigDecimal volumeComponent = BigDecimal.valueOf(capped);

        BigDecimal total = completionComponent.add(volumeComponent).setScale(2, RoundingMode.HALF_UP);
        
        // Ensure it doesn't exceed 100
        if (total.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        }
        return total;
    }
}

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

@Service
@RequiredArgsConstructor
@Slf4j
public class TrustRatingService {
    
    private final TrustRatingRepository trustRatingRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final ProfileCompletionRepository profileCompletionRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository; // Added transactionRepository
    private final BusinessTrustRatingRepository businessTrustRatingRepository;
    private final BusinessRepository businessRepository;

    // Trust score weights (out of 100)
    private static final BigDecimal DOCUMENT_WEIGHT = BigDecimal.valueOf(30); // 30%
    private static final BigDecimal PROFILE_WEIGHT = BigDecimal.valueOf(20); // 20%
    private static final BigDecimal REVIEW_WEIGHT = BigDecimal.valueOf(30); // 30%
    private static final BigDecimal TRANSACTION_WEIGHT = BigDecimal.valueOf(20); // 20%
    
    // Document upload scores (just uploading gives points)
    private static final BigDecimal ID_UPLOAD_SCORE = BigDecimal.valueOf(5);
    private static final BigDecimal ADDRESS_UPLOAD_SCORE = BigDecimal.valueOf(3);
    private static final BigDecimal PROFILE_PHOTO_UPLOAD_SCORE = BigDecimal.valueOf(2);
    
    // Document verification bonus scores (verified gets additional points)
    private static final BigDecimal ID_VERIFICATION_BONUS = BigDecimal.valueOf(10);
    private static final BigDecimal ADDRESS_VERIFICATION_BONUS = BigDecimal.valueOf(7);
    private static final BigDecimal PROFILE_PHOTO_VERIFICATION_BONUS = BigDecimal.valueOf(3);
    
    // Subscription bonus score
    private static final BigDecimal SUBSCRIPTION_BONUS = BigDecimal.valueOf(15);
    
    @Transactional
    public TrustRating calculateAndUpdateTrustRating(UUID userId) {
        log.info("Calculating trust rating for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        // Get or create trust rating
        TrustRating trustRating = trustRatingRepository.findByUserId(userId)
                .orElse(TrustRating.builder().user(user).build());
        
        // Calculate individual scores
        BigDecimal profileCompletionScore = calculateProfileCompletionScore(userId); // user info + uploaded docs
        BigDecimal verificationScore = calculateVerificationScore(userId); // only admin-verified docs
        BigDecimal reviewScore = calculateReviewScore(userId);
        BigDecimal transactionScore = calculateTransactionScore(userId);
        BigDecimal subscriptionScore = calculateSubscriptionScore(userId);

        // All components are 0-100, so average them for overall score
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
    
    // Profile completion = user info + uploaded docs (not verified)
    private BigDecimal calculateProfileCompletionScore(UUID userId) {
        int totalFields = 4; // user info fields: photo, bio, contact, location
        int completedFields = 0;
        Optional<ProfileCompletion> profileCompletionOpt = profileCompletionRepository.findByUserId(userId);
        if (profileCompletionOpt.isPresent()) {
            ProfileCompletion pc = profileCompletionOpt.get();
            if (Boolean.TRUE.equals(pc.getHasProfilePhoto())) completedFields++;
            if (Boolean.TRUE.equals(pc.getHasBio())) completedFields++;
            if (Boolean.TRUE.equals(pc.getHasContactNumber())) completedFields++;
            if (Boolean.TRUE.equals(pc.getHasLocation())) completedFields++;
        }
        // Uploaded docs (ID, driver's license, proof of address, profile photo)
        int totalDocs = 4;
        int uploadedDocs = 0;
        if (verificationDocumentRepository.findByUserIdAndDocumentType(userId, VerificationDocument.DocumentType.ID_CARD).isPresent()) uploadedDocs++;
        if (verificationDocumentRepository.findByUserIdAndDocumentType(userId, VerificationDocument.DocumentType.DRIVERS_LICENSE).isPresent()) uploadedDocs++;
        if (verificationDocumentRepository.findByUserIdAndDocumentType(userId, VerificationDocument.DocumentType.PROOF_OF_ADDRESS).isPresent()) uploadedDocs++;
        if (verificationDocumentRepository.findByUserIdAndDocumentType(userId, VerificationDocument.DocumentType.PROFILE_PHOTO).isPresent()) uploadedDocs++;
        int total = totalFields + totalDocs;
        int complete = completedFields + uploadedDocs;
        return BigDecimal.valueOf(complete).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    // Verification score = % of uploaded docs that are admin-verified
    private BigDecimal calculateVerificationScore(UUID userId) {
        int totalDocs = 0;
        int verifiedDocs = 0;
        Optional<VerificationDocument> idDoc = verificationDocumentRepository.findByUserIdAndDocumentType(userId, VerificationDocument.DocumentType.ID_CARD);
        if (idDoc.isPresent()) {
            totalDocs++;
            if (idDoc.get().getStatus() == VerificationDocument.VerificationStatus.APPROVED) verifiedDocs++;
        }
        Optional<VerificationDocument> dlDoc = verificationDocumentRepository.findByUserIdAndDocumentType(userId, VerificationDocument.DocumentType.DRIVERS_LICENSE);
        if (dlDoc.isPresent()) {
            totalDocs++;
            if (dlDoc.get().getStatus() == VerificationDocument.VerificationStatus.APPROVED) verifiedDocs++;
        }
        Optional<VerificationDocument> addressDoc = verificationDocumentRepository.findByUserIdAndDocumentType(userId, VerificationDocument.DocumentType.PROOF_OF_ADDRESS);
        if (addressDoc.isPresent()) {
            totalDocs++;
            if (addressDoc.get().getStatus() == VerificationDocument.VerificationStatus.APPROVED) verifiedDocs++;
        }
        Optional<VerificationDocument> photoDoc = verificationDocumentRepository.findByUserIdAndDocumentType(userId, VerificationDocument.DocumentType.PROFILE_PHOTO);
        if (photoDoc.isPresent()) {
            totalDocs++;
            if (photoDoc.get().getStatus() == VerificationDocument.VerificationStatus.APPROVED) verifiedDocs++;
        }
        if (totalDocs == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(verifiedDocs).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalDocs), 2, RoundingMode.HALF_UP);
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
        boolean hasLocation = (user.getCity() != null) || (user.getCustomCity() != null && !user.getCustomCity().isEmpty());
        profileCompletion.setHasLocation(hasLocation);
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

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

@Service
@RequiredArgsConstructor
@Slf4j
public class TrustRatingService {
    
    private final TrustRatingRepository trustRatingRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final ProfileCompletionRepository profileCompletionRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    
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
    public TrustRating calculateAndUpdateTrustRating(Long userId) {
        log.info("Calculating trust rating for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        // Get or create trust rating
        TrustRating trustRating = trustRatingRepository.findByUserId(userId)
                .orElse(TrustRating.builder().user(user).build());
        
        // Calculate individual scores
        BigDecimal documentScore = calculateDocumentScore(userId);
        BigDecimal profileScore = calculateProfileScore(userId);
        BigDecimal reviewScore = calculateReviewScore(userId);
        BigDecimal transactionScore = calculateTransactionScore(userId);
        BigDecimal subscriptionBonus = calculateSubscriptionBonus(userId);
        
        // Calculate overall score (subscription bonus is added directly, not weighted)
        BigDecimal overallScore = documentScore.multiply(DOCUMENT_WEIGHT)
                .add(profileScore.multiply(PROFILE_WEIGHT))
                .add(reviewScore.multiply(REVIEW_WEIGHT))
                .add(transactionScore.multiply(TRANSACTION_WEIGHT))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .add(subscriptionBonus);
        
        // Update trust rating
        trustRating.setDocumentScore(documentScore);
        trustRating.setProfileScore(profileScore);
        trustRating.setReviewScore(reviewScore);
        trustRating.setTransactionScore(transactionScore);
        trustRating.setOverallScore(overallScore);
        
        // Update review counts
        Long totalReviews = reviewRepository.countReviewsByUserId(userId);
        Long positiveReviews = reviewRepository.countPositiveReviewsByUserId(userId);
        trustRating.setTotalReviews(totalReviews.intValue());
        trustRating.setPositiveReviews(positiveReviews.intValue());
        
        // For now, set transaction counts to 0 (will be updated when transaction system is implemented)
        trustRating.setTotalTransactions(0);
        trustRating.setSuccessfulTransactions(0);
        
        TrustRating saved = trustRatingRepository.save(trustRating);
        log.info("Updated trust rating for user {}: overall score = {}", userId, overallScore);
        
        return saved;
    }
    
    private BigDecimal calculateDocumentScore(Long userId) {
        BigDecimal score = BigDecimal.ZERO;
        
        // Check for ID document
        Optional<VerificationDocument> idDoc = verificationDocumentRepository
                .findByUserIdAndDocumentType(userId, VerificationDocument.DocumentType.ID_CARD);
        if (idDoc.isPresent()) {
            // Upload points (just for uploading)
            score = score.add(ID_UPLOAD_SCORE);
            
            // Verification bonus (if approved)
            if (idDoc.get().getStatus() == VerificationDocument.VerificationStatus.APPROVED) {
                score = score.add(ID_VERIFICATION_BONUS);
            }
        }
        
        // Check for address document
        Optional<VerificationDocument> addressDoc = verificationDocumentRepository
                .findByUserIdAndDocumentType(userId, VerificationDocument.DocumentType.PROOF_OF_ADDRESS);
        if (addressDoc.isPresent()) {
            // Upload points (just for uploading)
            score = score.add(ADDRESS_UPLOAD_SCORE);
            
            // Verification bonus (if approved)
            if (addressDoc.get().getStatus() == VerificationDocument.VerificationStatus.APPROVED) {
                score = score.add(ADDRESS_VERIFICATION_BONUS);
            }
        }
        
        // Check for profile photo
        Optional<VerificationDocument> photoDoc = verificationDocumentRepository
                .findByUserIdAndDocumentType(userId, VerificationDocument.DocumentType.PROFILE_PHOTO);
        if (photoDoc.isPresent()) {
            // Upload points (just for uploading)
            score = score.add(PROFILE_PHOTO_UPLOAD_SCORE);
            
            // Verification bonus (if approved)
            if (photoDoc.get().getStatus() == VerificationDocument.VerificationStatus.APPROVED) {
                score = score.add(PROFILE_PHOTO_VERIFICATION_BONUS);
            }
        }
        
        return score;
    }
    
    private BigDecimal calculateProfileScore(Long userId) {
        Optional<ProfileCompletion> profileCompletion = profileCompletionRepository.findByUserId(userId);
        if (profileCompletion.isPresent()) {
            return profileCompletion.get().getCompletionPercentage();
        }
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculateReviewScore(Long userId) {
        Long totalReviews = reviewRepository.countReviewsByUserId(userId);
        if (totalReviews == 0) {
            return BigDecimal.ZERO;
        }
        
        Long positiveReviews = reviewRepository.countPositiveReviewsByUserId(userId);
        BigDecimal positivePercentage = BigDecimal.valueOf(positiveReviews)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalReviews), 2, RoundingMode.HALF_UP);
        
        // Get average rating
        BigDecimal averageRating = reviewRepository.getAverageRatingByUserId(userId);
        if (averageRating == null) {
            averageRating = BigDecimal.ZERO;
        }
        
        // Convert 5-star rating to percentage and combine with positive percentage
        BigDecimal ratingPercentage = averageRating.multiply(BigDecimal.valueOf(20)); // 5 stars = 100%
        
        // Weight: 70% positive percentage, 30% average rating
        return positivePercentage.multiply(BigDecimal.valueOf(0.7))
                .add(ratingPercentage.multiply(BigDecimal.valueOf(0.3)));
    }
    
    private BigDecimal calculateTransactionScore(Long userId) {
        // For now, return 0. This will be implemented when transaction system is added
        // Transaction score will be based on successful transactions vs total transactions
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculateSubscriptionBonus(Long userId) {
        // Check if user has active subscription
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getRole() == dev.marketplace.marketplace.enums.Role.SUBSCRIBED) {
            return SUBSCRIPTION_BONUS;
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Add subscription bonus to user's trust rating
     */
    @Transactional
    public void addSubscriptionBonus(Long userId) {
        log.info("Adding subscription bonus to user: {}", userId);
        calculateAndUpdateTrustRating(userId);
    }
    
    /**
     * Remove subscription bonus from user's trust rating
     */
    @Transactional
    public void removeSubscriptionBonus(Long userId) {
        log.info("Removing subscription bonus from user: {}", userId);
        calculateAndUpdateTrustRating(userId);
    }
    
    @Transactional
    public TrustRating getTrustRating(Long userId) {
        return trustRatingRepository.findByUserId(userId)
                .orElseGet(() -> calculateAndUpdateTrustRating(userId));
    }
    
    @Transactional
    public void updateProfileCompletion(Long userId) {
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
        profileCompletion.setHasVerifiedEmail(true); // Assuming email is verified if user exists
        profileCompletion.setHasVerifiedPhone(false); // Will be updated when phone verification is implemented
        
        // Check for ID document (uploaded = true, verified = bonus)
        Optional<VerificationDocument> idDoc = verificationDocumentRepository
                .findByUserIdAndDocumentType(userId, VerificationDocument.DocumentType.ID_CARD);
        profileCompletion.setHasIdVerification(idDoc.isPresent() && 
                idDoc.get().getStatus() == VerificationDocument.VerificationStatus.APPROVED);
        
        // Check for address document (uploaded = true, verified = bonus)
        Optional<VerificationDocument> addressDoc = verificationDocumentRepository
                .findByUserIdAndDocumentType(userId, VerificationDocument.DocumentType.PROOF_OF_ADDRESS);
        profileCompletion.setHasAddressVerification(addressDoc.isPresent() && 
                addressDoc.get().getStatus() == VerificationDocument.VerificationStatus.APPROVED);
        
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
} 
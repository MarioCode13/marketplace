package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.Transaction;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.*;
import dev.marketplace.marketplace.service.dto.TrustComponentsDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TrustRatingServiceTest {

    @Mock
    private TrustRatingRepository trustRatingRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TrustRatingService trustRatingService;

    @Test
    public void testCalculateComponents_noReviews_usesGlobalPrior() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        // no profile fields set

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(trustRatingRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(reviewRepository.countReviewsByUserId(userId)).thenReturn(0L);
        when(reviewRepository.getAverageRatingByUserId(userId)).thenReturn(null);
        when(reviewRepository.getGlobalAverageRating()).thenReturn(BigDecimal.valueOf(4.2));
        when(transactionRepository.countBySellerId(userId)).thenReturn(0L);
        when(transactionRepository.countByBuyerId(userId)).thenReturn(0L);

        TrustComponentsDTO components = trustRatingService.calculateTrustComponents(userId);

        // reviewScore should equal 4.2 * 20 = 84.00
        assertEquals(BigDecimal.valueOf(84.00).setScale(2, RoundingMode.HALF_UP), components.getReviewScore());
        // overall = review*0.35 = 84*0.35 = 29.40 (other components 0)
        assertEquals(BigDecimal.valueOf(29.40).setScale(2, RoundingMode.HALF_UP), components.getOverallScore());
    }

    @Test
    public void testCalculateComponents_singleReview_pullsToPrior() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(trustRatingRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(reviewRepository.countReviewsByUserId(userId)).thenReturn(1L);
        when(reviewRepository.getAverageRatingByUserId(userId)).thenReturn(BigDecimal.valueOf(5.0));
        when(reviewRepository.getGlobalAverageRating()).thenReturn(BigDecimal.valueOf(4.2));
        when(transactionRepository.countBySellerId(userId)).thenReturn(0L);
        when(transactionRepository.countByBuyerId(userId)).thenReturn(0L);

        TrustComponentsDTO components = trustRatingService.calculateTrustComponents(userId);

        // Weighted rating approx: ((1/11)*5 + (10/11)*4.2) * 20 = ~85.45
        assertEquals(BigDecimal.valueOf(85.45).setScale(2, RoundingMode.HALF_UP), components.getReviewScore());
    }

    @Test
    public void testTransactionScore_withCompletionAndVolumeBoost() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(trustRatingRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Simulate 20 total transactions (as seller), 18 completed
        when(transactionRepository.countBySellerId(userId)).thenReturn(20L);
        when(transactionRepository.countByBuyerId(userId)).thenReturn(0L);
        when(transactionRepository.countBySellerIdAndStatus(userId, Transaction.TransactionStatus.COMPLETED)).thenReturn(18L);
        when(transactionRepository.countByBuyerIdAndStatus(userId, Transaction.TransactionStatus.COMPLETED)).thenReturn(0L);

        // No reviews
        when(reviewRepository.countReviewsByUserId(userId)).thenReturn(0L);
        when(reviewRepository.getGlobalAverageRating()).thenReturn(BigDecimal.valueOf(3.5));

        TrustComponentsDTO components = trustRatingService.calculateTrustComponents(userId);

        // completion rate = 18/20 = 90 -> completionComponent = 90*0.6 = 54
        // volume boost = min(log10(20+1)*15, 15) -> capped at 15
        // total transaction score = 54 + 15 = 69.00
        assertEquals(BigDecimal.valueOf(69.00).setScale(2, RoundingMode.HALF_UP), components.getTransactionScore());
    }
}

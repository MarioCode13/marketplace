package dev.marketplace.marketplace.integration;

import dev.marketplace.marketplace.model.Transaction;
import dev.marketplace.marketplace.model.TrustRating;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.*;
import dev.marketplace.marketplace.service.TrustRatingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
public class TrustRatingNewModelIntegrationTest {

    @InjectMocks
    private TrustRatingService trustRatingService;

    @Mock
    private TrustRatingRepository trustRatingRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Test
    public void calculateAndUpdateTrustRating_usesNewModel_whenFlagEnabled() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        // Existing TrustRating - marked verified to test verification component
        TrustRating existing = TrustRating.builder().user(user).verifiedID(true).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(trustRatingRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        // Reviews: 1 review, avg=5.0, global avg 4.2
        when(reviewRepository.countReviewsByUserId(userId)).thenReturn(1L);
        when(reviewRepository.getAverageRatingByUserId(userId)).thenReturn(BigDecimal.valueOf(5.0));
        when(reviewRepository.getGlobalAverageRating()).thenReturn(BigDecimal.valueOf(4.2));
        when(reviewRepository.countPositiveReviewsByUserId(userId)).thenReturn(1L);

        // Transactions: 20 total (seller), 18 completed
        when(transactionRepository.countBySellerId(userId)).thenReturn(20L);
        when(transactionRepository.countByBuyerId(userId)).thenReturn(0L);
        when(transactionRepository.countBySellerIdAndStatus(userId, Transaction.TransactionStatus.COMPLETED)).thenReturn(18L);
        when(transactionRepository.countByBuyerIdAndStatus(userId, Transaction.TransactionStatus.COMPLETED)).thenReturn(0L);

        // Capture saved entity
        ArgumentCaptor<TrustRating> captor = ArgumentCaptor.forClass(TrustRating.class);
        when(trustRatingRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        trustRatingService.calculateAndUpdateTrustRating(userId);
        TrustRating captured = captor.getValue();

        // Expected component values based on existing helpers
        BigDecimal expectedReview = BigDecimal.valueOf(85.45).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedTransaction = BigDecimal.valueOf(69.00).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedVerification = BigDecimal.valueOf(70).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedProfile = BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedOverall = expectedReview.multiply(BigDecimal.valueOf(0.35))
                .add(expectedTransaction.multiply(BigDecimal.valueOf(0.30)))
                .add(expectedVerification.multiply(BigDecimal.valueOf(0.20)))
                .add(expectedProfile.multiply(BigDecimal.valueOf(0.15)))
                .setScale(2, RoundingMode.HALF_UP);

        assertEquals(expectedReview, captured.getReviewScore());
        assertEquals(expectedTransaction, captured.getTransactionScore());
        assertEquals(expectedVerification, captured.getVerificationScore());
        assertEquals(expectedProfile, captured.getProfileScore());
        assertEquals(expectedOverall, captured.getOverallScore());
    }
}

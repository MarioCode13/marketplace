package dev.marketplace.marketplace.unit;

import dev.marketplace.marketplace.dto.BusinessTrustRatingDTO;
import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.BusinessTrustRating;
import dev.marketplace.marketplace.resolvers.BusinessQueryResolver;
import dev.marketplace.marketplace.service.BusinessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessQueryResolverTest {

    @Mock
    private BusinessService businessService;

    @InjectMocks
    private BusinessQueryResolver resolver;

    private UUID businessId;

    @BeforeEach
    void setup() {
        businessId = UUID.randomUUID();
    }

    @Test
    void trustRating_includesVerifiedWithThirdParty() {
        BusinessTrustRating rating = BusinessTrustRating.builder()
                .business(new Business())
                .overallScore(BigDecimal.valueOf(4.5))
                .totalReviews(10)
                .verifiedWithThirdParty(true)
                .build();

        when(businessService.getBusinessTrustRating(businessId)).thenReturn(rating);

        BusinessTrustRatingDTO dto = resolver.businessTrustRating(businessId);

        assertTrue(dto.isVerifiedWithThirdParty());
    }
}


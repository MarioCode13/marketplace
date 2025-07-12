package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.TrustRating;
import dev.marketplace.marketplace.model.VerificationDocument;
import dev.marketplace.marketplace.service.TrustRatingService;
import dev.marketplace.marketplace.service.VerificationDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class TrustQueryResolver {
    
    private final TrustRatingService trustRatingService;
    private final VerificationDocumentService verificationDocumentService;
    
    /**
     * Get trust rating for a user
     */
    @QueryMapping
    public TrustRating getTrustRating(@Argument Long userId) {
        return trustRatingService.getTrustRating(userId);
    }
    
    /**
     * Get all verification documents for a user
     */
    @QueryMapping
    public List<VerificationDocument> getUserVerificationDocuments(@Argument Long userId) {
        return verificationDocumentService.getUserDocuments(userId);
    }
    
    /**
     * Get a specific verification document by type for a user
     */
    @QueryMapping
    public VerificationDocument getUserDocumentByType(@Argument Long userId, 
                                                     @Argument String documentType) {
        return verificationDocumentService.getUserDocumentByType(userId, 
                VerificationDocument.DocumentType.valueOf(documentType))
                .orElse(null);
    }
} 
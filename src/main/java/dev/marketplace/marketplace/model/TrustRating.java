package dev.marketplace.marketplace.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trust_rating")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrustRating {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private java.util.UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "overall_score", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal overallScore = BigDecimal.ZERO;
    
    @Column(name = "profile_score", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal profileScore = BigDecimal.ZERO;
    
    @Column(name = "review_score", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal reviewScore = BigDecimal.ZERO;
    
    @Column(name = "transaction_score", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal transactionScore = BigDecimal.ZERO;
    
    @Column(name = "verification_score", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal verificationScore = BigDecimal.ZERO;
    
    @Column(name = "total_reviews", nullable = false)
    @Builder.Default
    private Integer totalReviews = 0;
    
    @Column(name = "positive_reviews", nullable = false)
    @Builder.Default
    private Integer positiveReviews = 0;
    
    @Column(name = "total_transactions", nullable = false)
    @Builder.Default
    private Integer totalTransactions = 0;
    
    @Column(name = "successful_transactions", nullable = false)
    @Builder.Default
    private Integer successfulTransactions = 0;
    
    @Column(name = "last_calculated")
    private LocalDateTime lastCalculated;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastCalculated = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getStarRating() {
        if (overallScore == null || overallScore.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        // Convert 0-100 score to 0-5 stars
        return overallScore.divide(BigDecimal.valueOf(20), 1, java.math.RoundingMode.HALF_UP);
    }

    public String getTrustLevel() {
        if (overallScore == null) return "UNKNOWN";
        
        int score = overallScore.intValue();
        if (score >= 90) return "EXCELLENT";
        if (score >= 80) return "VERY_GOOD";
        if (score >= 70) return "GOOD";
        if (score >= 60) return "FAIR";
        if (score >= 50) return "POOR";
        return "VERY_POOR";
    }
}

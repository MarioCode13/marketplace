package dev.marketplace.marketplace.model;

import jakarta.persistence.*;
        import lombok.*;
        import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "business_trust_rating")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessTrustRating {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "overall_score", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal overallScore = BigDecimal.ZERO;

    @Column(name = "verification_score", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal verificationScore = BigDecimal.ZERO;

    @Column(name = "profile_score", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal profileScore = BigDecimal.ZERO;

    @Column(name = "review_score", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal reviewScore = BigDecimal.ZERO;

    @Column(name = "transaction_score", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal transactionScore = BigDecimal.ZERO;

    @Column(name = "total_reviews", nullable = false)
    @Builder.Default
    private int totalReviews = 0;

    @Column(name = "positive_reviews", nullable = false)
    @Builder.Default
    private int positiveReviews = 0;

    @Column(name = "total_transactions", nullable = false)
    @Builder.Default
    private int totalTransactions = 0;

    @Column(name = "successful_transactions", nullable = false)
    @Builder.Default
    private int successfulTransactions = 0;

    @Column(name = "last_calculated")
    private LocalDateTime lastCalculated;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "verified_with_third_party", nullable = false)
    @Builder.Default
    private boolean verifiedWithThirdParty = false;

    @Transient
    @Builder.Default
    private Double averageRating = 0.0;

    @Transient
    @Builder.Default
    private Integer reviewCount = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}

package dev.marketplace.marketplace.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private java.util.UUID id;

    /**
     * Either user or business must be set, depending on subscription type.
     * For user-level subscriptions, set user and leave business null.
     * For business-level subscriptions, set business and leave user null (or set user as creator).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id")
    private Business business;

    @Column(name = "stripe_subscription_id", unique = true)
    private String stripeSubscriptionId;
    
    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;
    
    @Column(name = "plan_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private PlanType planType;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "USD";
    
    @Column(name = "billing_cycle", nullable = false)
    @Enumerated(EnumType.STRING)
    private BillingCycle billingCycle;
    
    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;
    
    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;
    
    @Column(name = "cancel_at_period_end")
    @Builder.Default
    private Boolean cancelAtPeriodEnd = false;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "payfast_profile_id")
    private String payfastProfileId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum PlanType {
        SELLER_PLUS("Seller+", new BigDecimal("1")),
        RESELLER("Reseller", new BigDecimal("1")),
        PRO_STORE("Pro Store", new BigDecimal("1"));

        private final String displayName;
        private final BigDecimal price;

        PlanType(String displayName, BigDecimal price) {
            this.displayName = displayName;
            this.price = price;
        }

        public String getDisplayName() {
            return displayName;
        }

        public BigDecimal getPrice() {
            return price;
        }
    }
    
    public enum SubscriptionStatus {
        ACTIVE,
        PAST_DUE,
        CANCELLED,
        UNPAID,
        TRIAL
    }
    
    public enum BillingCycle {
        MONTHLY,
        YEARLY
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIAL;
    }
    
    public boolean isExpired() {
        return currentPeriodEnd != null && LocalDateTime.now().isAfter(currentPeriodEnd);
    }
    
    public boolean willCancelAtPeriodEnd() {
        return cancelAtPeriodEnd != null && cancelAtPeriodEnd;
    }
}

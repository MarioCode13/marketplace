package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    // Find active subscription for a user
    Optional<Subscription> findByUserIdAndStatusIn(UUID userId, List<Subscription.SubscriptionStatus> statuses);

    // Find by Stripe subscription ID
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    
    // Find by Stripe customer ID
    List<Subscription> findByStripeCustomerId(String stripeCustomerId);
    
    // Find all active subscriptions
    List<Subscription> findByStatusIn(List<Subscription.SubscriptionStatus> statuses);
    
    // Find subscriptions expiring soon
    @Query("SELECT s FROM Subscription s WHERE s.currentPeriodEnd BETWEEN :startDate AND :endDate AND s.status = 'ACTIVE'")
    List<Subscription> findExpiringSubscriptions(@Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate);
    
    // Find subscriptions that need renewal
    @Query("SELECT s FROM Subscription s WHERE s.currentPeriodEnd <= :date AND s.status = 'ACTIVE'")
    List<Subscription> findSubscriptionsNeedingRenewal(@Param("date") LocalDateTime date);
    
    // Count active subscriptions
    long countByStatusIn(List<Subscription.SubscriptionStatus> statuses);
    
    // Find subscriptions by plan type
    List<Subscription> findByPlanType(Subscription.PlanType planType);
    
    // Find subscriptions by billing cycle
    List<Subscription> findByBillingCycle(Subscription.BillingCycle billingCycle);
    
    // Find all subscriptions for a user (including cancelled)
    List<Subscription> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Check if user has active subscription
    boolean existsByUserIdAndStatusIn(UUID userId, List<Subscription.SubscriptionStatus> statuses);

    // Business-level subscription queries
    Optional<Subscription> findByBusinessIdAndStatusIn(UUID businessId, List<Subscription.SubscriptionStatus> statuses);
    boolean existsByBusinessIdAndStatusIn(UUID businessId, List<Subscription.SubscriptionStatus> statuses);
    List<Subscription> findByBusinessIdOrderByCreatedAtDesc(UUID businessId);
}

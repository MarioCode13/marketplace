package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.enums.Role;
import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.model.Subscription;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.BusinessRepository;
import dev.marketplace.marketplace.repository.ListingRepository;
import dev.marketplace.marketplace.repository.SubscriptionRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final TrustRatingService trustRatingService;
    private final BusinessRepository businessRepository;
    private final ListingRepository listingRepository;

    /**
     * Check if user has active subscription
     */
    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(UUID userId) {
        return subscriptionRepository.existsByUserIdAndStatusIn(
                userId, 
                List.of(Subscription.SubscriptionStatus.ACTIVE, Subscription.SubscriptionStatus.TRIAL)
        );
    }
    
    /**
     * Get user's active subscription
     */
    @Transactional(readOnly = true)
    public Optional<Subscription> getActiveSubscription(UUID userId) {
        return subscriptionRepository.findByUserIdAndStatusIn(
                userId,
                List.of(Subscription.SubscriptionStatus.ACTIVE, Subscription.SubscriptionStatus.TRIAL)
        );
    }
    
    /**
     * Create a new subscription (called after successful Stripe payment)
     */
    @Transactional
    public Subscription createSubscription(UUID userId,
                                         String stripeSubscriptionId,
                                         String stripeCustomerId,
                                         Subscription.PlanType planType,
                                         BigDecimal amount,
                                         Subscription.BillingCycle billingCycle) {
        log.info("Creating subscription for user: {}, plan: {}", userId, planType);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        // Calculate period dates
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodEnd = billingCycle == Subscription.BillingCycle.MONTHLY 
                ? now.plusMonths(1) 
                : now.plusYears(1);
        
        Subscription subscription = Subscription.builder()
                .user(user)
                .stripeSubscriptionId(stripeSubscriptionId)
                .stripeCustomerId(stripeCustomerId)
                .planType(planType)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .amount(amount)
                .billingCycle(billingCycle)
                .currentPeriodStart(now)
                .currentPeriodEnd(periodEnd)
                .cancelAtPeriodEnd(false)
                .build();
        
        Subscription saved = subscriptionRepository.save(subscription);
        
        // Update user role to SUBSCRIBED
        user.setRole(Role.SUBSCRIBED);
        userRepository.save(user);
        
        // Update trust rating with subscription bonus
        trustRatingService.addSubscriptionBonus(userId);
        
        log.info("Subscription created successfully: {}", saved.getId());
        
        return saved;
    }
    
    /**
     * Update subscription status (called by Stripe webhook)
     */
    @Transactional
    public Subscription updateSubscriptionStatus(String stripeSubscriptionId, 
                                               Subscription.SubscriptionStatus status) {
        log.info("Updating subscription status: {}, status: {}", stripeSubscriptionId, status);
        
        Subscription subscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + stripeSubscriptionId));
        
        subscription.setStatus(status);
        
        if (status == Subscription.SubscriptionStatus.CANCELLED) {
            subscription.setCancelledAt(LocalDateTime.now());

            User user = subscription.getUser();
            boolean isInBusiness = !businessRepository.findByUser(user).isEmpty() || businessRepository.findByOwner(user).isPresent();
            if (!hasActiveSubscription(user.getId()) && !isInBusiness) {
                user.setRole(Role.HAS_ACCOUNT);
                userRepository.save(user);

                // Remove subscription bonus from trust rating
                trustRatingService.removeSubscriptionBonus(user.getId());
            }
        }
        
        Subscription saved = subscriptionRepository.save(subscription);
        
        log.info("Subscription status updated successfully: {}", stripeSubscriptionId);
        
        return saved;
    }
    
    /**
     * Cancel subscription at period end
     */
    @Transactional
    public Subscription cancelAtPeriodEnd(UUID userId) {
        log.info("Cancelling subscription at period end for user: {}", userId);
        
        Subscription subscription = getActiveSubscription(userId)
                .orElseThrow(() -> new IllegalArgumentException("No active subscription found for user: " + userId));
        
        subscription.setCancelAtPeriodEnd(true);
        Subscription saved = subscriptionRepository.save(subscription);
        
        log.info("Subscription marked for cancellation at period end: {}", subscription.getId());
        
        return saved;
    }
    
    /**
     * Reactivate cancelled subscription
     */
    @Transactional
    public Subscription reactivateSubscription(UUID userId) {
        log.info("Reactivating subscription for user: {}", userId);
        
        Subscription subscription = getActiveSubscription(userId)
                .orElseThrow(() -> new IllegalArgumentException("No active subscription found for user: " + userId));
        
        subscription.setCancelAtPeriodEnd(false);
        Subscription saved = subscriptionRepository.save(subscription);
        
        log.info("Subscription reactivated: {}", subscription.getId());
        
        return saved;
    }
    
    /**
     * Create or activate a PayFast subscription for the user and plan type
     */
    @Transactional
    public void createOrActivatePayFastSubscription(UUID userId, Subscription.PlanType planType) {
        if (!hasActiveSubscription(userId)) {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime periodEnd = now.plusMonths(1);
            Subscription subscription = Subscription.builder()
                .user(user)
                .planType(planType)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .amount(planType.getPrice())
                .currency("ZAR")
                .billingCycle(Subscription.BillingCycle.MONTHLY)
                .currentPeriodStart(now)
                .currentPeriodEnd(periodEnd)
                .cancelAtPeriodEnd(false)
                .build();
            subscriptionRepository.save(subscription);
            user.setRole(Role.SUBSCRIBED);
            userRepository.save(user);
            trustRatingService.addSubscriptionBonus(userId);
            log.info("PayFast subscription created for user: {} with plan: {}", userId, planType);
        }
    }
    
    /**
     * Get all subscriptions for a user
     */
    @Transactional(readOnly = true)
    public List<Subscription> getUserSubscriptions(UUID userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get all active subscriptions
     */
    @Transactional(readOnly = true)
    public List<Subscription> getActiveSubscriptions() {
        return subscriptionRepository.findByStatusIn(
                List.of(Subscription.SubscriptionStatus.ACTIVE, Subscription.SubscriptionStatus.TRIAL)
        );
    }
    
    /**
     * Get subscriptions expiring soon
     */
    @Transactional(readOnly = true)
    public List<Subscription> getExpiringSubscriptions(int daysAhead) {
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(daysAhead);
        return subscriptionRepository.findExpiringSubscriptions(startDate, endDate);
    }
    
    /**
     * Get subscription statistics
     */
    @Transactional(readOnly = true)
    public SubscriptionStats getSubscriptionStats() {
        long activeCount = subscriptionRepository.countByStatusIn(
                List.of(Subscription.SubscriptionStatus.ACTIVE, Subscription.SubscriptionStatus.TRIAL)
        );
        
        long totalCount = subscriptionRepository.count();
        
        return new SubscriptionStats(activeCount, totalCount);
    }
    
    /**
     * Check if user can contact sellers (requires active subscription)
     */
    @Transactional(readOnly = true)
    public boolean canContactSellers(UUID userId) {
        return hasActiveSubscription(userId);
    }
    
    /**
     * Get subscription plan details
     */
    public static Subscription.PlanType[] getAvailablePlans() {
        return Subscription.PlanType.values();
    }
    
    /**
     * Subscription statistics
     */
    public record SubscriptionStats(long activeSubscriptions, long totalSubscriptions) {}

    @Transactional
    public void handleBusinessSubscriptionExpiry(UUID businessId) {
        // Archive business and all its listings
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));
        business.setArchived(true);
        business.setBusinessType(null); // Set to null on expiry to fully disable business features
        List<Listing> listings = listingRepository.findByBusinessAndArchivedFalse(business);
        for (Listing listing : listings) {
            listing.setArchived(true);
            listingRepository.save(listing);
        }
        // Optionally, disable store URL and editing in other services
    }

    @Transactional
    public void handleBusinessSubscriptionReactivation(UUID businessId) {
        // Unarchive business and all its listings if within 14 days
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));
        if (business.isArchived()) {
            business.setArchived(false);
            businessRepository.save(business);
            List<Listing> listings = listingRepository.findByBusinessAndArchivedTrue(business);
            for (Listing listing : listings) {
                listing.setArchived(false);
                listingRepository.save(listing);
            }
            // Optionally, restore roles and permissions
        }
    }
}

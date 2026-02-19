package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.enums.BusinessType;
import dev.marketplace.marketplace.enums.BusinessUserRole;
import dev.marketplace.marketplace.enums.Role;
import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.BusinessTrustRating;
import dev.marketplace.marketplace.model.BusinessUser;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.model.Subscription;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.BusinessRepository;
import dev.marketplace.marketplace.repository.BusinessTrustRatingRepository;
import dev.marketplace.marketplace.repository.BusinessUserRepository;
import dev.marketplace.marketplace.repository.ListingRepository;
import dev.marketplace.marketplace.repository.SubscriptionRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.marketplace.marketplace.service.EmailService;

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
    private final BusinessUserRepository businessUserRepository;
    private final BusinessTrustRatingRepository businessTrustRatingRepository;
    private final EmailService emailService;
    private final BusinessService businessService;

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
     * Create a new subscription (supports both user-level and business-level subscriptions)
     * If userId is provided, creates a user-level subscription.
     * If businessId is provided, creates a business-level subscription.
     * At least one must be provided.
     */
    @Transactional
    public Subscription createSubscription(UUID userId,
                                           UUID businessId,
                                           String stripeSubscriptionId,
                                           String stripeCustomerId,
                                           Subscription.PlanType planType,
                                           BigDecimal amount,
                                           Subscription.BillingCycle billingCycle) {
        log.info("Creating subscription for user: {}, business: {}, plan: {}", userId, businessId, planType);

        if (userId == null && businessId == null) {
            throw new IllegalArgumentException("Either userId or businessId must be provided.");
        }
        if (userId != null && businessId != null) {
            throw new IllegalArgumentException("Only one of userId or businessId should be provided for a subscription.");
        }

        User user = null;
        Business business = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        }
        if (businessId != null) {
            business = businessRepository.findById(businessId)
                    .orElseThrow(() -> new IllegalArgumentException("Business not found with ID: " + businessId));
        }

        // Calculate period dates
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodEnd = billingCycle == Subscription.BillingCycle.MONTHLY
                ? now.plusMonths(1)
                : now.plusYears(1);

        Subscription subscription = Subscription.builder()
                .user(user)
                .business(business)
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

        if (user != null) {
            // Update user role to SUBSCRIBED
            user.setRole(Role.SUBSCRIBED);
            userRepository.save(user);
            // Update trust rating with subscription bonus
            trustRatingService.addSubscriptionBonus(user.getId());
        }
        if (business != null) {
            // Optionally, update business status/plan here if needed
            // e.g., business.setPlanType(planType.name());
            // businessRepository.save(business);
        }

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
        
        // Cancel PayFast recurring profile if present
        if (subscription.getPayfastProfileId() != null && !subscription.getPayfastProfileId().isBlank()) {
            cancelPayFastProfile(subscription.getPayfastProfileId());
        }
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
        log.info("[PayFast Sub] Called for userId={}, planType={}", userId, planType);

        // Load user early
        User user;
        try {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        } catch (Exception e) {
            log.error("[PayFast Sub] User lookup failed for userId={}", userId, e);
            throw e;
        }

        Optional<Subscription> existingOpt = getActiveSubscription(userId);
        if (existingOpt.isPresent()) {
            Subscription existing = existingOpt.get();
            // If same plan, nothing to do
            if (existing.getPlanType() == planType) {
                log.info("[PayFast Sub] User {} already has an active subscription with same plan {}, skipping createOrActivatePayFastSubscription", userId, planType);
                return;
            }

            // Upgrade path: update existing subscription to the new plan
            log.info("[PayFast Sub] Upgrading subscription for user {} from {} to {}", userId, existing.getPlanType(), planType);
            try {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime newPeriodEnd = now.plusMonths(1);

                existing.setPlanType(planType);
                existing.setAmount(planType.getPrice());
                existing.setBillingCycle(Subscription.BillingCycle.MONTHLY);
                existing.setCurrentPeriodStart(now);
                existing.setCurrentPeriodEnd(newPeriodEnd);
                existing.setStatus(Subscription.SubscriptionStatus.ACTIVE);
                existing.setCancelAtPeriodEnd(false);

                subscriptionRepository.save(existing);
                log.info("[PayFast Sub] Subscription upgraded and saved for user {} to plan {}", userId, planType);
            } catch (Exception e) {
                log.error("[PayFast Sub] Failed to upgrade subscription for userId={}", userId, e);
                throw e;
            }

            // For PRO_STORE and RESELLER plans, create a business (if not present) and link the user as owner
            if (planType == Subscription.PlanType.PRO_STORE || planType == Subscription.PlanType.RESELLER) {
                boolean hasOwnerBusiness = false;
                try {
                    hasOwnerBusiness = businessRepository.findByOwner(user).isPresent() ||
                                       !businessRepository.findByUser(user).isEmpty();
                } catch (Exception e) {
                    log.error("[PayFast Sub] Error checking business ownership for userId={}", userId, e);
                }
                if (!hasOwnerBusiness) {
                    try {
                        Business business = new Business();
                        business.setOwner(user);
                        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
                            business.setName(user.getFirstName() + "'s Store");
                        } else {
                            business.setName(user.getEmail() + "'s Store");
                        }
                        business.setEmail(user.getEmail());
                        if (planType == Subscription.PlanType.PRO_STORE) {
                            business.setBusinessType(BusinessType.PRO_STORE);
                        } else {
                            business.setBusinessType(BusinessType.RESELLER);
                        }
                        // Delegate to BusinessService so verification email, slug checks and listing migration run
                        Business created = businessService.createBusiness(business, false);
                        log.info("[PayFast Sub] Business created via BusinessService for user {} businessId {} (upgrade)", userId, created.getId());

                        BusinessTrustRating businessTrustRating = BusinessTrustRating.builder()
                                .business(created)
                                .overallScore(BigDecimal.ZERO)
                                .verificationScore(BigDecimal.ZERO)
                                .profileScore(BigDecimal.ZERO)
                                .reviewScore(BigDecimal.ZERO)
                                .transactionScore(BigDecimal.ZERO)
                                .totalReviews(0)
                                .positiveReviews(0)
                                .totalTransactions(0)
                                .successfulTransactions(0)
                                .build();
                        businessTrustRatingRepository.save(businessTrustRating);
                        log.info("[PayFast Sub] BusinessTrustRating created for businessId {} (upgrade)", created.getId());
                     } catch (Exception e) {
                         log.error("[PayFast Sub] Failed to create business for userId={}", userId, e);
                         throw e;
                     }
                 } else {
                     log.info("[PayFast Sub] User {} already has a business; skipping creation (upgrade)", userId);
                 }
             }

            // Add trust rating bonus if not already applied - attempt add (TrustRatingService should handle idempotency)
            try {
                trustRatingService.addSubscriptionBonus(userId);
                log.info("[PayFast Sub] Trust rating bonus ensured for userId={}", userId);
            } catch (Exception e) {
                log.error("[PayFast Sub] Failed to add trust rating bonus during upgrade for userId={}", userId, e);
            }

            // Send subscription upgrade confirmation email
            try {
                String userName = user.getFirstName() != null ? user.getFirstName() : user.getUsername();
                String planName = planType.getDisplayName();
                String amount = planType.getPrice().toString();
                String billingCycle = Subscription.BillingCycle.MONTHLY.toString();
                String nextBillingDate = LocalDateTime.now().plusMonths(1).toString();

                emailService.sendSubscriptionConfirmationEmail(
                        user.getEmail(),
                        userName,
                        planName,
                        amount,
                        billingCycle,
                        nextBillingDate
                );
                log.info("[PayFast Sub] Subscription upgrade confirmation email sent to {}", user.getEmail());
            } catch (Exception e) {
                log.error("[PayFast Sub] Failed to send subscription upgrade email to {}: {}", user.getEmail(), e.getMessage(), e);
                // Don't fail upgrade if email fails
            }

            return; // upgrade handled
        }

        // No active subscription -> create new one
        log.debug("[PayFast Sub] No active subscription, proceeding to create for user {} plan {}", userId, planType);
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
        try {
            subscriptionRepository.save(subscription);
            log.info("[PayFast Sub] Subscription saved for user {} subscriptionPlan {}", userId, planType);
        } catch (Exception e) {
            log.error("[PayFast Sub] Failed to save subscription for userId={}", userId, e);
            throw e;
        }
        // Update user role
        try {
            user.setRole(Role.SUBSCRIBED);
            userRepository.save(user);
            log.info("[PayFast Sub] User role updated for user {}", userId);
        } catch (Exception e) {
            log.error("[PayFast Sub] Failed to update user role for userId={}", userId, e);
            throw e;
        }
        // For PRO_STORE and RESELLER plans, create a business (if not present) and link the user as owner
        if (planType == Subscription.PlanType.PRO_STORE || planType == Subscription.PlanType.RESELLER) {
            boolean hasOwnerBusiness = false;
            try {
                hasOwnerBusiness = businessRepository.findByOwner(user).isPresent() ||
                                   !businessRepository.findByUser(user).isEmpty();
            } catch (Exception e) {
                log.error("[PayFast Sub] Error checking business ownership for userId={}", userId, e);
            }
            if (!hasOwnerBusiness) {
                try {
                    Business business = new Business();
                    business.setOwner(user);
                    // Set required fields for business entity
                    if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
                        business.setName(user.getFirstName() + "'s Store");
                    } else {
                        business.setName(user.getEmail() + "'s Store");
                    }
                    business.setEmail(user.getEmail()); // Set the email field
                    if (planType == Subscription.PlanType.PRO_STORE) {
                        business.setBusinessType(BusinessType.PRO_STORE);
                    } else {
                        business.setBusinessType(BusinessType.RESELLER);
                    }
                    Business created = businessService.createBusiness(business, false);
                    log.info("[PayFast Sub] Business created via BusinessService for user {} businessId {}", userId, created.getId());

                    BusinessTrustRating businessTrustRating = BusinessTrustRating.builder()
                            .business(created)
                            .overallScore(BigDecimal.ZERO)
                            .verificationScore(BigDecimal.ZERO)
                            .profileScore(BigDecimal.ZERO)
                            .reviewScore(BigDecimal.ZERO)
                            .transactionScore(BigDecimal.ZERO)
                            .totalReviews(0)
                            .positiveReviews(0)
                            .totalTransactions(0)
                            .successfulTransactions(0)
                            .build();
                    businessTrustRatingRepository.save(businessTrustRating);
                    log.info("[PayFast Sub] BusinessTrustRating created for businessId {}", created.getId());
                 } catch (Exception e) {
                     log.error("[PayFast Sub] Failed to create business for userId={}", userId, e);
                     throw e;
                 }
             } else {
                 log.info("[PayFast Sub] User {} already has a business; skipping creation", userId);
             }
         }
        try {
            trustRatingService.addSubscriptionBonus(userId);
            log.info("[PayFast Sub] Trust rating bonus added for userId={}", userId);
        } catch (Exception e) {
            log.error("[PayFast Sub] Failed to add trust rating bonus for userId={}", userId, e);
        }

        // Send subscription confirmation email
        try {
            String userName = user.getFirstName() != null ? user.getFirstName() : user.getUsername();
            String planName = planType.getDisplayName();
            String amount = planType.getPrice().toString();
            String billingCycle = Subscription.BillingCycle.MONTHLY.toString();
            String nextBillingDate = LocalDateTime.now().plusMonths(1).toString();

            emailService.sendSubscriptionConfirmationEmail(
                user.getEmail(),
                userName,
                planName,
                amount,
                billingCycle,
                nextBillingDate
            );
            log.info("[PayFast Sub] Subscription confirmation email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("[PayFast Sub] Failed to send subscription confirmation email to {}: {}", user.getEmail(), e.getMessage(), e);
            // Don't fail the subscription creation if email fails
        }

        log.info("[PayFast Sub] PayFast subscription created for user: {} with plan: {}", userId, planType);
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

    /**
     * Cancel PayFast recurring profile by profile ID
     */
    private void cancelPayFastProfile(String payfastProfileId) {
        // TODO: Implement actual API call to PayFast to cancel the recurring profile
        // For now, just log the action
        log.info("[PayFast] Cancelling recurring profile with ID: {}", payfastProfileId);
        // Example: Use RestTemplate or WebClient to call PayFast API
    }

    /**
     * Get business's active subscription
     */
    @Transactional(readOnly = true)
    public Optional<Subscription> getActiveBusinessSubscription(UUID businessId) {
        return subscriptionRepository.findByBusinessIdAndStatusIn(
                businessId,
                List.of(Subscription.SubscriptionStatus.ACTIVE, Subscription.SubscriptionStatus.TRIAL)
        );
    }

    /**
     * Cancel business subscription at period end
     */
    @Transactional
    public Subscription cancelBusinessSubscriptionAtPeriodEnd(UUID businessId) {
        log.info("Cancelling subscription at period end for business: {}", businessId);
        Subscription subscription = getActiveBusinessSubscription(businessId)
                .orElseThrow(() -> new IllegalArgumentException("No active subscription found for business: " + businessId));
        // Cancel PayFast recurring profile if present
        if (subscription.getPayfastProfileId() != null && !subscription.getPayfastProfileId().isBlank()) {
            cancelPayFastProfile(subscription.getPayfastProfileId());
        }
        subscription.setCancelAtPeriodEnd(true);
        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Business subscription marked for cancellation at period end: {}", subscription.getId());
        return saved;
    }

    /**
     * Reactivate cancelled business subscription
     */
    @Transactional
    public Subscription reactivateBusinessSubscription(UUID businessId) {
        log.info("Reactivating subscription for business: {}", businessId);
        Subscription subscription = getActiveBusinessSubscription(businessId)
                .orElseThrow(() -> new IllegalArgumentException("No active subscription found for business: " + businessId));
        subscription.setCancelAtPeriodEnd(false);
        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Business subscription reactivated: {}", subscription.getId());
        return saved;
    }

    /**
     * Get active subscription for a business
     */
    @Transactional(readOnly = true)
    public Optional<Subscription> getActiveSubscriptionForBusiness(UUID businessId) {
        return subscriptionRepository.findByBusinessIdAndStatusIn(
            businessId,
            List.of(Subscription.SubscriptionStatus.ACTIVE, Subscription.SubscriptionStatus.TRIAL)
        );
    }

    /**
     * Renew an existing PayFast subscription (called by PayFast renewal ITN callback)
     * Updates the subscription period dates and sends renewal confirmation email
     */
    @Transactional
    public Subscription renewPayFastSubscription(UUID userId,
                                               Subscription.PlanType planType,
                                               BigDecimal amount) {
        log.info("[PayFast Renewal] Called for userId={}, planType={}, amount={}", userId, planType, amount);

        // 1. Find user's current subscription
        Subscription subscription = getActiveSubscription(userId)
            .orElseThrow(() -> new IllegalArgumentException("No active subscription found for user: " + userId));

        log.info("[PayFast Renewal] Found existing subscription: id={}, currentPeriodEnd={}",
                 subscription.getId(), subscription.getCurrentPeriodEnd());

        // 2. Verify it's the correct plan type (log warning if mismatch but continue)
        if (!subscription.getPlanType().equals(planType)) {
            log.warn("[PayFast Renewal] Plan type mismatch. Expected: {}, Got: {}",
                     subscription.getPlanType(), planType);
        }

        // 3. Calculate new period dates
        LocalDateTime newPeriodStart = subscription.getCurrentPeriodEnd();
        LocalDateTime newPeriodEnd = subscription.getBillingCycle() == Subscription.BillingCycle.MONTHLY
            ? newPeriodStart.plusMonths(1)
            : newPeriodStart.plusYears(1);

        // 4. Update subscription record
        subscription.setCurrentPeriodStart(newPeriodStart);
        subscription.setCurrentPeriodEnd(newPeriodEnd);
        subscription.setAmount(amount);
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);

        Subscription renewed = subscriptionRepository.save(subscription);
        log.info("[PayFast Renewal] Subscription renewed for user {}. New period: {} to {}",
                 userId, newPeriodStart, newPeriodEnd);

        // 5. Send renewal confirmation email
        try {
            User user = renewed.getUser();
            String userName = user.getFirstName() != null ? user.getFirstName() : user.getUsername();
            String planName = planType.getDisplayName();
            String billingCycle = renewed.getBillingCycle().toString();
            String nextBillingDate = newPeriodEnd.toString();

            emailService.sendSubscriptionConfirmationEmail(
                user.getEmail(),
                userName,
                planName,
                amount.toString(),
                billingCycle,
                nextBillingDate
            );
            log.info("[PayFast Renewal] Renewal confirmation email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("[PayFast Renewal] Failed to send renewal confirmation email to {}: {}",
                      userId, e.getMessage(), e);
            // Don't fail renewal if email fails
        }

        log.info("[PayFast Renewal] PayFast subscription renewed for user: {} with plan: {}", userId, planType);
        return renewed;
    }

    /**
     * Renew user's subscription (convenience method for testing/development)
     * If no active subscription exists, this will throw an exception.
     */
    @Transactional
    public Subscription renewSubscriptionForUser(UUID userId) {
        log.info("[Sub Renewal] Renewing subscription for user: {}", userId);

        Subscription subscription = getActiveSubscription(userId)
            .orElseThrow(() -> new IllegalArgumentException("No active subscription found for user: " + userId));

        // Calculate new period dates
        LocalDateTime newPeriodStart = subscription.getCurrentPeriodEnd();
        LocalDateTime newPeriodEnd = subscription.getBillingCycle() == Subscription.BillingCycle.MONTHLY
            ? newPeriodStart.plusMonths(1)
            : newPeriodStart.plusYears(1);

        // Update subscription record
        subscription.setCurrentPeriodStart(newPeriodStart);
        subscription.setCurrentPeriodEnd(newPeriodEnd);

        Subscription renewed = subscriptionRepository.save(subscription);
        log.info("[Sub Renewal] Subscription renewed for user {}. New period: {} to {}",
                 userId, newPeriodStart, newPeriodEnd);

        return renewed;
    }

    /**
     * Cancel user's subscription immediately (not just at period end)
     */
    @Transactional
    public Subscription cancelSubscriptionNow(UUID userId) {
        log.info("[Sub Cancel] Immediately cancelling subscription for user: {}", userId);

        Subscription subscription = getActiveSubscription(userId)
            .orElseThrow(() -> new IllegalArgumentException("No active subscription found for user: " + userId));

        // Cancel PayFast recurring profile if present
        if (subscription.getPayfastProfileId() != null && !subscription.getPayfastProfileId().isBlank()) {
            cancelPayFastProfile(subscription.getPayfastProfileId());
        }

        subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.now());

        Subscription cancelled = subscriptionRepository.save(subscription);

        // Update user role if they don't have business
        User user = subscription.getUser();
        boolean isInBusiness = !businessRepository.findByUser(user).isEmpty() || businessRepository.findByOwner(user).isPresent();
        if (!hasActiveSubscription(user.getId()) && !isInBusiness) {
            user.setRole(Role.HAS_ACCOUNT);
            userRepository.save(user);
            trustRatingService.removeSubscriptionBonus(user.getId());
        }

        log.info("[Sub Cancel] Subscription cancelled for user {}", userId);
        return cancelled;
    }

    /**
     * Get list of all subscriptions for user (including inactive ones)
     */
    @Transactional(readOnly = true)
    public List<Subscription> getAllUserSubscriptions(UUID userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Send subscription confirmation email for an existing user and plan.
     * This is a helper used by dev flows to force-send the confirmation email
     * even when a subscription already exists.
     */
    @Transactional
    public void sendSubscriptionConfirmationEmailForUser(UUID userId, Subscription.PlanType planType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        try {
            String userName = user.getFirstName() != null ? user.getFirstName() : user.getUsername();
            String planName = planType.getDisplayName();
            String amount = planType.getPrice().toString();
            String billingCycle = Subscription.BillingCycle.MONTHLY.toString();
            String nextBillingDate = LocalDateTime.now().plusMonths(1).toString();

            emailService.sendSubscriptionConfirmationEmail(
                    user.getEmail(),
                    userName,
                    planName,
                    amount,
                    billingCycle,
                    nextBillingDate
            );
            log.info("[Sub Email] Subscription confirmation email sent to {} (forced)", user.getEmail());
        } catch (Exception e) {
            log.error("[Sub Email] Failed to send subscription confirmation email to {}: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send subscription confirmation email: " + e.getMessage(), e);
        }
    }

    /**
     * Forcefully set or update user's active subscription to the given plan.
     * Creates a subscription if none exists, or updates the existing one.
     * Sends confirmation email after operation.
     */
    @Transactional
    public Subscription forceSetSubscriptionPlan(UUID userId, Subscription.PlanType planType) {
        log.info("[PayFast Sub] forceSetSubscriptionPlan called for userId={}, planType={}", userId, planType);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        Optional<Subscription> existingOpt = getActiveSubscription(userId);
        Subscription subscription;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodEnd = now.plusMonths(1);
        if (existingOpt.isPresent()) {
            subscription = existingOpt.get();
            subscription.setPlanType(planType);
            subscription.setAmount(planType.getPrice());
            subscription.setBillingCycle(Subscription.BillingCycle.MONTHLY);
            subscription.setCurrentPeriodStart(now);
            subscription.setCurrentPeriodEnd(periodEnd);
            subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
            subscription.setCancelAtPeriodEnd(false);
            subscription = subscriptionRepository.save(subscription);
            log.info("[PayFast Sub] force-updated existing subscription {} for user {} to plan {}", subscription.getId(), userId, planType);
        } else {
            subscription = Subscription.builder()
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
            subscription = subscriptionRepository.save(subscription);
            log.info("[PayFast Sub] force-created new subscription {} for user {} plan {}", subscription.getId(), userId, planType);

            // Update user role
            try {
                user.setRole(Role.SUBSCRIBED);
                userRepository.save(user);
                log.info("[PayFast Sub] User role updated for user {} (force)", userId);
            } catch (Exception e) {
                log.error("[PayFast Sub] Failed to update user role for userId={} (force)", userId, e);
            }
        }

        // For PRO_STORE and RESELLER plans, create a business (if not present) and link the user as owner
        if (planType == Subscription.PlanType.PRO_STORE || planType == Subscription.PlanType.RESELLER) {
            boolean hasOwnerBusiness = false;
            try {
                hasOwnerBusiness = businessRepository.findByOwner(user).isPresent() ||
                                   !businessRepository.findByUser(user).isEmpty();
            } catch (Exception e) {
                log.error("[PayFast Sub] Error checking business ownership for userId={} (force)", userId, e);
            }
            if (!hasOwnerBusiness) {
                try {
                    Business business = new Business();
                    business.setOwner(user);
                    if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
                        business.setName(user.getFirstName() + "'s Store");
                    } else {
                        business.setName(user.getEmail() + "'s Store");
                    }
                    business.setEmail(user.getEmail());
                    if (planType == Subscription.PlanType.PRO_STORE) {
                        business.setBusinessType(BusinessType.PRO_STORE);
                    } else {
                        business.setBusinessType(BusinessType.RESELLER);
                    }
                    Business created = businessService.createBusiness(business, false);
                    log.info("[PayFast Sub] Business created via BusinessService for user {} businessId {} (force)", userId, created.getId());

                    BusinessTrustRating businessTrustRating = BusinessTrustRating.builder()
                            .business(created)
                            .overallScore(BigDecimal.ZERO)
                            .verificationScore(BigDecimal.ZERO)
                            .profileScore(BigDecimal.ZERO)
                            .reviewScore(BigDecimal.ZERO)
                            .transactionScore(BigDecimal.ZERO)
                            .totalReviews(0)
                            .positiveReviews(0)
                            .totalTransactions(0)
                            .successfulTransactions(0)
                            .build();
                    businessTrustRatingRepository.save(businessTrustRating);
                    log.info("[PayFast Sub] BusinessTrustRating created for businessId {} (force)", created.getId());
                 } catch (Exception e) {
                     log.error("[PayFast Sub] Failed to create business for userId={} (force)", userId, e);
                 }
             }
        }

        // Ensure trust rating bonus
        try {
            trustRatingService.addSubscriptionBonus(userId);
            log.info("[PayFast Sub] Trust rating bonus ensured for userId={} (force)", userId);
        } catch (Exception e) {
            log.error("[PayFast Sub] Failed to add trust rating bonus for userId={} (force)", userId, e);
        }

        // Send confirmation email
        try {
            String userName = user.getFirstName() != null ? user.getFirstName() : user.getUsername();
            String planName = planType.getDisplayName();
            String amount = planType.getPrice().toString();
            String billingCycle = Subscription.BillingCycle.MONTHLY.toString();
            String nextBillingDate = LocalDateTime.now().plusMonths(1).toString();

            emailService.sendSubscriptionConfirmationEmail(
                    user.getEmail(),
                    userName,
                    planName,
                    amount,
                    billingCycle,
                    nextBillingDate
            );
            log.info("[PayFast Sub] Force subscription confirmation email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("[PayFast Sub] Failed to send force subscription confirmation email to {}: {}", user.getEmail(), e.getMessage(), e);
        }

        return subscription;
    }
}

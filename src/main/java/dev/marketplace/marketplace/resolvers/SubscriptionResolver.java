package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.Subscription;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.service.SubscriptionService;
import dev.marketplace.marketplace.service.SubscriptionService.SubscriptionStats;
import dev.marketplace.marketplace.service.StripeService;
import dev.marketplace.marketplace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SubscriptionResolver {
    
    private final SubscriptionService subscriptionService;
    private final StripeService stripeService;
    private final UserService userService;
    
    /**
     * Create checkout session for subscription
     */
    @MutationMapping
    public String createCheckoutSession(@Argument String planType,
                                      @Argument String billingCycle,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = userService.getUserIdByUsername(userDetails.getUsername());
            User user = userService.getUserById(userId);
            
            Subscription.PlanType plan = Subscription.PlanType.valueOf(planType);
            Subscription.BillingCycle cycle = Subscription.BillingCycle.valueOf(billingCycle);
            
            var session = stripeService.createCheckoutSession(
                    userId,
                    user.getEmail(),
                    user.getUsername() != null ? user.getUsername() : user.getEmail(),
                    plan,
                    cycle
            );
            
            return session.getUrl();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create checkout session: " + e.getMessage());
        }
    }
    
    /**
     * Cancel subscription at period end
     */
    @MutationMapping
    public Subscription cancelSubscription(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return subscriptionService.cancelAtPeriodEnd(userId);
    }
    
    /**
     * Reactivate subscription
     */
    @MutationMapping
    public Subscription reactivateSubscription(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return subscriptionService.reactivateSubscription(userId);
    }
    
    /**
     * Get current user's subscription
     */
    @QueryMapping
    public Subscription mySubscription(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return subscriptionService.getActiveSubscription(userId).orElse(null);
    }
    
    /**
     * Get all user's subscriptions (including cancelled)
     */
    @QueryMapping
    public List<Subscription> mySubscriptionHistory(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return subscriptionService.getUserSubscriptions(userId);
    }
    
    /**
     * Check if user has active subscription
     */
    @QueryMapping
    public boolean hasActiveSubscription(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return subscriptionService.hasActiveSubscription(userId);
    }
    
    /**
     * Check if user can contact sellers
     */
    @QueryMapping
    public boolean canContactSellers(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return subscriptionService.canContactSellers(userId);
    }
    
    /**
     * Get available subscription plans
     */
    @QueryMapping
    public List<String> getAvailablePlans() {
        return List.of(
                "BASIC_MONTHLY",
                "BASIC_YEARLY", 
                "PREMIUM_MONTHLY",
                "PREMIUM_YEARLY"
        );
    }
    
    /**
     * Get subscription statistics (admin only)
     */
    @QueryMapping
    public SubscriptionStats getSubscriptionStats() {
        return subscriptionService.getSubscriptionStats();
    }
    
    /**
     * Get expiring subscriptions (admin only)
     */
    @QueryMapping
    public List<Subscription> getExpiringSubscriptions(@Argument Integer daysAhead) {
        return subscriptionService.getExpiringSubscriptions(daysAhead != null ? daysAhead : 7);
    }
} 
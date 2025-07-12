package dev.marketplace.marketplace.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import dev.marketplace.marketplace.model.Subscription.PlanType;
import dev.marketplace.marketplace.model.Subscription.BillingCycle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {
    
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    
    @Value("${app.base.url}")
    private String baseUrl;
    
    private final SubscriptionService subscriptionService;
    
    /**
     * Create a Stripe customer
     */
    public Customer createCustomer(String email, String name) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name)
                .build();
        
        return Customer.create(params);
    }
    
    /**
     * Create a checkout session for subscription
     */
    public Session createCheckoutSession(Long userId, String userEmail, String userName, 
                                       PlanType planType, BillingCycle billingCycle) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        
        // Get price ID based on plan type and billing cycle
        String priceId = getPriceId(planType, billingCycle);
        
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomerEmail(userEmail)
                .setSuccessUrl(baseUrl + "/subscription/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(baseUrl + "/subscription/cancel")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build())
                .putMetadata("userId", userId.toString())
                .putMetadata("planType", planType.name())
                .putMetadata("billingCycle", billingCycle.name())
                .build();
        
        return Session.create(params);
    }
    
    /**
     * Create a subscription directly (for testing or admin use)
     */
    public Subscription createSubscription(String customerId, PlanType planType, 
                                         BillingCycle billingCycle) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        
        String priceId = getPriceId(planType, billingCycle);
        
        SubscriptionCreateParams params = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice(priceId)
                        .build())
                .build();
        
        return Subscription.create(params);
    }
    
    /**
     * Cancel a subscription
     */
    public Subscription cancelSubscription(String stripeSubscriptionId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        
        Subscription subscription = Subscription.retrieve(stripeSubscriptionId);
        return subscription.cancel();
    }
    
    /**
     * Get subscription details
     */
    public Subscription getSubscription(String stripeSubscriptionId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        return Subscription.retrieve(stripeSubscriptionId);
    }
    
    /**
     * Get customer details
     */
    public Customer getCustomer(String customerId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        return Customer.retrieve(customerId);
    }
    
    /**
     * Get price ID for plan type and billing cycle
     * These would be configured in your Stripe dashboard
     */
    private String getPriceId(PlanType planType, BillingCycle billingCycle) {
        // These are example price IDs - you'll need to create these in your Stripe dashboard
        Map<String, String> priceIds = new HashMap<>();
        
        // Basic plans
        priceIds.put("BASIC_MONTHLY", "price_basic_monthly");
        priceIds.put("BASIC_YEARLY", "price_basic_yearly");
        
        // Premium plans
        priceIds.put("PREMIUM_MONTHLY", "price_premium_monthly");
        priceIds.put("PREMIUM_YEARLY", "price_premium_yearly");
        
        String key = planType.name() + "_" + billingCycle.name();
        String priceId = priceIds.get(key);
        
        if (priceId == null) {
            throw new IllegalArgumentException("Price ID not found for plan: " + key);
        }
        
        return priceId;
    }
    
    /**
     * Verify webhook signature
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            com.stripe.net.Webhook.Signature.verifyHeader(payload, signature, webhookSecret, 300);
            return true;
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }
    
    /**
     * Handle subscription created webhook
     */
    public void handleSubscriptionCreated(Subscription stripeSubscription) {
        try {
            String customerId = stripeSubscription.getCustomer();
            String subscriptionId = stripeSubscription.getId();
            
            // Get customer details
            Customer customer = getCustomer(customerId);
            
            // Extract metadata
            Map<String, String> metadata = stripeSubscription.getMetadata();
            Long userId = Long.parseLong(metadata.get("userId"));
            PlanType planType = PlanType.valueOf(metadata.get("planType"));
            BillingCycle billingCycle = BillingCycle.valueOf(metadata.get("billingCycle"));
            
            // Create subscription in our database
            subscriptionService.createSubscription(
                    userId,
                    subscriptionId,
                    customerId,
                    planType,
                    planType.getPrice(),
                    billingCycle
            );
            
            log.info("Subscription created successfully: {}", subscriptionId);
            
        } catch (Exception e) {
            log.error("Error handling subscription created webhook", e);
        }
    }
    
    /**
     * Handle subscription updated webhook
     */
    public void handleSubscriptionUpdated(Subscription stripeSubscription) {
        try {
            String subscriptionId = stripeSubscription.getId();
            String status = stripeSubscription.getStatus();
            
            // Map Stripe status to our enum
            dev.marketplace.marketplace.model.Subscription.SubscriptionStatus ourStatus;
            switch (status) {
                case "active":
                    ourStatus = dev.marketplace.marketplace.model.Subscription.SubscriptionStatus.ACTIVE;
                    break;
                case "past_due":
                    ourStatus = dev.marketplace.marketplace.model.Subscription.SubscriptionStatus.PAST_DUE;
                    break;
                case "canceled":
                    ourStatus = dev.marketplace.marketplace.model.Subscription.SubscriptionStatus.CANCELLED;
                    break;
                case "unpaid":
                    ourStatus = dev.marketplace.marketplace.model.Subscription.SubscriptionStatus.UNPAID;
                    break;
                default:
                    ourStatus = dev.marketplace.marketplace.model.Subscription.SubscriptionStatus.ACTIVE;
            }
            
            // Update subscription in our database
            subscriptionService.updateSubscriptionStatus(subscriptionId, ourStatus);
            
            log.info("Subscription updated: {} - {}", subscriptionId, status);
            
        } catch (Exception e) {
            log.error("Error handling subscription updated webhook", e);
        }
    }
} 
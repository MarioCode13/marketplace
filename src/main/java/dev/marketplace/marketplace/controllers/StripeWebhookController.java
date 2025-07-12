package dev.marketplace.marketplace.controllers;

import com.stripe.model.Event;
import com.stripe.model.Subscription;
import dev.marketplace.marketplace.service.StripeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/webhooks/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {
    
    private final StripeService stripeService;
    
    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                               @RequestHeader("Stripe-Signature") String signature) {
        log.info("Received Stripe webhook");
        
        try {
            // Verify webhook signature
            if (!stripeService.verifyWebhookSignature(payload, signature)) {
                log.error("Invalid webhook signature");
                return ResponseEntity.badRequest().body("Invalid signature");
            }
            
            // Parse the event
            Event event = Event.GSON.fromJson(payload, Event.class);
            
            // Handle the event
            switch (event.getType()) {
                case "customer.subscription.created":
                    handleSubscriptionCreated(event);
                    break;
                case "customer.subscription.updated":
                    handleSubscriptionUpdated(event);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event);
                    break;
                case "invoice.payment_succeeded":
                    handlePaymentSucceeded(event);
                    break;
                case "invoice.payment_failed":
                    handlePaymentFailed(event);
                    break;
                default:
                    log.info("Unhandled event type: {}", event.getType());
            }
            
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.badRequest().body("Webhook processing failed");
        }
    }
    
    private void handleSubscriptionCreated(Event event) {
        Subscription subscription = (Subscription) event.getData().getObject();
        log.info("Handling subscription created: {}", subscription.getId());
        stripeService.handleSubscriptionCreated(subscription);
    }
    
    private void handleSubscriptionUpdated(Event event) {
        Subscription subscription = (Subscription) event.getData().getObject();
        log.info("Handling subscription updated: {}", subscription.getId());
        stripeService.handleSubscriptionUpdated(subscription);
    }
    
    private void handleSubscriptionDeleted(Event event) {
        Subscription subscription = (Subscription) event.getData().getObject();
        log.info("Handling subscription deleted: {}", subscription.getId());
        stripeService.handleSubscriptionUpdated(subscription); // Use same handler
    }
    
    private void handlePaymentSucceeded(Event event) {
        log.info("Handling payment succeeded: {}", event.getId());
        // You can add additional logic here if needed
    }
    
    private void handlePaymentFailed(Event event) {
        log.info("Handling payment failed: {}", event.getId());
        // You can add additional logic here if needed
    }
} 
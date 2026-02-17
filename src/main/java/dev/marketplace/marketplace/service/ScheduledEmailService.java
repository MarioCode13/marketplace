package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.Subscription;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.SubscriptionRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ScheduledEmailService handles background email sending tasks.
 * This service uses Spring's @Scheduled annotation to run periodic checks
 * and send emails for events like listing/subscription expiration warnings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledEmailService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Check for listings expiring in the next 7 days and send warning emails.
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void checkListingExpirations() {
        log.info("Starting scheduled task: checkListingExpirations");
        try {
            // TODO: Implement listing expiration check logic
            // 1. Find all active listings expiring within 7 days
            // 2. Filter those that haven't received a warning email yet
            // 3. Send warning emails
            // 4. Update expirationWarningEmailSentAt timestamp
            log.debug("Completed listing expiration check");
        } catch (Exception e) {
            log.error("Error in checkListingExpirations scheduled task", e);
        }
    }

    /**
     * Check for subscriptions expiring in the next 7 days and send warning emails.
     * Runs daily at 2:30 AM.
     */
    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void checkSubscriptionExpirations() {
        log.info("[Scheduled] ========== Starting subscription expiration check ==========");
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime sevenDaysFromNow = now.plusDays(7);

            // Phase 1: Find subscriptions expiring in next 7 days
            log.info("[Scheduled] Looking for subscriptions expiring between {} and {}", now, sevenDaysFromNow);
            List<Subscription> expiringSubscriptions = subscriptionRepository
                .findExpiringSubscriptions(now, sevenDaysFromNow);

            log.info("[Scheduled] Found {} subscriptions expiring in next 7 days", expiringSubscriptions.size());

            for (Subscription subscription : expiringSubscriptions) {
                try {
                    User user = subscription.getUser();

                    if (subscription.willCancelAtPeriodEnd()) {
                        // User has marked subscription for cancellation
                        log.info("[Scheduled] Subscription {} will be cancelled at period end (user initiated)",
                                subscription.getId());
                    } else {
                        // Send renewal warning email (auto-renewal enabled)
                        sendRenewalWarningEmail(user, subscription);
                    }
                } catch (Exception e) {
                    log.error("[Scheduled] Error processing subscription {}: {}",
                             subscription.getId(), e.getMessage(), e);
                }
            }

            // Phase 2: Check subscriptions that expired in the last 24 hours
            log.info("[Scheduled] Looking for subscriptions that expired in the last 24 hours");
            LocalDateTime yesterday = now.minusDays(1);
            List<Subscription> recentlyExpired = subscriptionRepository
                .findExpiringSubscriptions(yesterday, now);

            log.info("[Scheduled] Found {} subscriptions that expired recently", recentlyExpired.size());

            for (Subscription subscription : recentlyExpired) {
                try {
                    if (subscription.getStatus() == Subscription.SubscriptionStatus.ACTIVE) {
                        // Renewal ITN was successfully processed
                        log.info("[Scheduled] Subscription {} was successfully renewed", subscription.getId());
                    } else if (subscription.getStatus() == Subscription.SubscriptionStatus.PAST_DUE) {
                        // Renewal failed - subscription is past due
                        log.warn("[Scheduled] Subscription {} failed to renew (status: PAST_DUE)", subscription.getId());
                        handleFailedRenewal(subscription);
                    } else {
                        // Subscription was cancelled
                        log.info("[Scheduled] Subscription {} has status: {}",
                                subscription.getId(), subscription.getStatus());
                    }
                } catch (Exception e) {
                    log.error("[Scheduled] Error handling expired subscription {}: {}",
                             subscription.getId(), e.getMessage(), e);
                }
            }

            log.info("[Scheduled] ========== Completed subscription expiration check ==========");
        } catch (Exception e) {
            log.error("[Scheduled] Error in checkSubscriptionExpirations scheduled task", e);
        }
    }

    /**
     * Send renewal warning email to user
     */
    private void sendRenewalWarningEmail(User user, Subscription subscription) {
        try {
            if (user == null || user.getEmail() == null) {
                log.warn("[Scheduled] Cannot send warning email - user or email is null");
                return;
            }

            String planName = subscription.getPlanType().getDisplayName();
            String expiryDate = subscription.getCurrentPeriodEnd().toString();
            String renewUrl = "https://www.dealio.org.za/dashboard";
            boolean isAutoRenewal = !subscription.willCancelAtPeriodEnd();

            emailService.sendSubscriptionExpirationWarningEmail(
                user.getEmail(),
                user.getFirstName() != null ? user.getFirstName() : user.getUsername(),
                planName,
                expiryDate,
                renewUrl,
                isAutoRenewal
            );
            log.info("[Scheduled] Renewal warning email sent to {} for subscription {}",
                    user.getEmail(), subscription.getId());
        } catch (Exception e) {
            log.error("[Scheduled] Failed to send renewal warning email to user {}: {}",
                     user.getId(), e.getMessage(), e);
        }
    }

    /**
     * Handle failed subscription renewal
     */
    private void handleFailedRenewal(Subscription subscription) {
        try {
            User user = subscription.getUser();
            if (user == null || user.getEmail() == null) {
                log.warn("[Scheduled] Cannot notify about failed renewal - user or email is null");
                return;
            }

            // Update subscription status to PAST_DUE
            subscription.setStatus(Subscription.SubscriptionStatus.PAST_DUE);
            subscriptionRepository.save(subscription);
            log.info("[Scheduled] Subscription {} marked as PAST_DUE due to failed renewal",
                    subscription.getId());

            // Send failure notification email
            String planName = subscription.getPlanType().getDisplayName();
            String expiryDate = subscription.getCurrentPeriodEnd().toString();
            String renewUrl = "https://www.dealio.org.za/dashboard/subscription/renew";

            emailService.sendSubscriptionExpirationWarningEmail(
                user.getEmail(),
                user.getFirstName() != null ? user.getFirstName() : user.getUsername(),
                planName,
                expiryDate,
                renewUrl,
                false  // Not auto-renewal since it failed
            );
            log.info("[Scheduled] Failed renewal notification sent to {} for subscription {}",
                    user.getEmail(), subscription.getId());
        } catch (Exception e) {
            log.error("[Scheduled] Error handling failed renewal for subscription {}: {}",
                     subscription.getId(), e.getMessage(), e);
        }
    }
}


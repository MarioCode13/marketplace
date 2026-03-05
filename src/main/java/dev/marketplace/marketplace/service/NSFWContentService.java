package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.enums.ContentApprovalStatus;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

/**
 * Service for NSFW content visibility and filtering
 * Determines whether users should see NSFW content based on age and preferences
 */
@Service
@Transactional(readOnly = true)
public class NSFWContentService {

    private static final int ADULT_AGE = 18;

    /**
     * Check if a user is eligible to view NSFW content
     * Must be 18+, age verified, and have enabled explicit content preference
     */
    public boolean canUserViewNSFW(User user) {
        if (user == null) {
            return false;
        }

        // User must have enabled explicit content viewing
        if (!user.getAllowsExplicitContent()) {
            return false;
        }

        // User must be age verified
        if (!user.getAgeVerified()) {
            return false;
        }

        // User must be 18+
        return isUserAdult(user);
    }

    /**
     * Check if user should be shown a specific listing
     * Filters based on NSFW approval status and user eligibility
     */
    public boolean canUserViewListing(Listing listing, User user) {
        // If listing is not NSFW flagged
        if (!listing.getNsfwFlagged()) {
            // If seller marked it as 18+ but it wasn't flagged by system,
            // check if it's been approved by admin
            if (listing.isSellerMarked18Plus()) {
                // Seller-marked 18+ content requires admin approval
                if (listing.getNsfwApprovalStatus() == ContentApprovalStatus.APPROVED) {
                    return canUserViewNSFW(user);
                }
                // Not approved yet, so hide it
                return false;
            }
            // Normal content, not marked 18+ - visible to all
            return true;
        }

        // If flagged by system, only show approved NSFW content to eligible users
        if (listing.getNsfwApprovalStatus() == ContentApprovalStatus.APPROVED) {
            // Approved NSFW content - only show to eligible users
            return canUserViewNSFW(user);
        }

        // Declined or pending NSFW content - hide from everyone
        return false;
    }

    /**
     * Filter listings to only show those the user can view
     */
    public List<Listing> filterListingsByUserEligibility(List<Listing> listings, User user) {
        return listings.stream()
                .filter(listing -> canUserViewListing(listing, user))
                .toList();
    }

    /**
     * Check if a listing is approved NSFW content
     */
    public boolean isApprovedNSFWContent(Listing listing) {
        return listing.getNsfwFlagged() &&
               listing.getNsfwApprovalStatus() == ContentApprovalStatus.APPROVED;
    }

    /**
     * Check if a listing is pending NSFW approval
     */
    public boolean isPendingNSFWApproval(Listing listing) {
        return listing.getNsfwFlagged() &&
               listing.getNsfwApprovalStatus() == ContentApprovalStatus.PENDING;
    }

    /**
     * Check if a listing is declined (should not be shown)
     */
    public boolean isDeclinedListing(Listing listing) {
        return listing.getNsfwFlagged() &&
               listing.getNsfwApprovalStatus() == ContentApprovalStatus.DECLINED;
    }

    /**
     * Determine if user is 18+ based on date of birth
     */
    public boolean isUserAdult(User user) {
        if (user.getDateOfBirth() == null) {
            return false;
        }

        LocalDate now = LocalDate.now();
        Period age = Period.between(user.getDateOfBirth(), now);
        return age.getYears() >= ADULT_AGE;
    }

    /**
     * Check user age and update ageVerified flag if they're 18+
     */
    @Transactional
    public boolean verifyUserAge(User user) {
        if (isUserAdult(user)) {
            user.setAgeVerified(true);
            return true;
        }
        user.setAgeVerified(false);
        return false;
    }

    /**
     * Get NSFW content visibility recommendation for business
     * Returns message about NSFW flag and approval status
     */
    public String getNSFWStatusMessage(Listing listing) {
        if (!listing.getNsfwFlagged() && !listing.isSellerMarked18Plus()) {
            return "Content is not flagged - visible to all users";
        }

        if (listing.isSellerMarked18Plus()) {
            ContentApprovalStatus status = listing.getNsfwApprovalStatus();
            if (status == ContentApprovalStatus.PENDING) {
                return "Seller marked as 18+ - pending admin review";
            } else if (status == ContentApprovalStatus.APPROVED) {
                return "Seller marked as 18+ and approved - visible to adults 18+ only";
            } else if (status == ContentApprovalStatus.DECLINED) {
                return "Seller marked as 18+ but declined by admin - not visible";
            }
        }

        if (listing.getNsfwFlagged()) {
            ContentApprovalStatus status = listing.getNsfwApprovalStatus();
            if (status == ContentApprovalStatus.PENDING) {
                return "Content is pending admin review (flagged by system)";
            } else if (status == ContentApprovalStatus.APPROVED) {
                return "Content approved but restricted to adults 18+";
            } else if (status == ContentApprovalStatus.DECLINED) {
                return "Content declined and will not be visible";
            }
        }

        return "Content flagged - status unknown";
    }

    /**
     * Rules for NSFW content visibility
     * Document the business rules
     */
    public record NSFWVisibilityRules(
            boolean requiresAgeVerification,
            boolean requiresExplicitContentOpt,
            String visibilityLevel,
            String description
    ) {
        public static NSFWVisibilityRules getRule(Listing listing, User user) {
            // Not flagged and not marked 18+ = public content
            if (!listing.getNsfwFlagged() && !listing.isSellerMarked18Plus()) {
                return new NSFWVisibilityRules(false, false, "PUBLIC", "No restrictions");
            }

            // Seller marked as 18+ but not flagged by system
            if (!listing.getNsfwFlagged() && listing.isSellerMarked18Plus()) {
                if (listing.getNsfwApprovalStatus() == ContentApprovalStatus.APPROVED) {
                    return new NSFWVisibilityRules(
                            true, true, "ADULT_ONLY",
                            "Seller marked as 18+ and approved by admin - visible only to users 18+ with explicit content enabled"
                    );
                } else {
                    return new NSFWVisibilityRules(
                            true, true, "HIDDEN",
                            "Seller marked as 18+ but pending admin approval"
                    );
                }
            }

            // Flagged by system
            ContentApprovalStatus status = listing.getNsfwApprovalStatus();
            if (status == ContentApprovalStatus.APPROVED) {
                return new NSFWVisibilityRules(
                        true, true, "ADULT_ONLY",
                        "Visible only to users 18+ with explicit content enabled"
                );
            } else if (status == ContentApprovalStatus.DECLINED || status == ContentApprovalStatus.PENDING) {
                return new NSFWVisibilityRules(
                        true, true, "HIDDEN",
                        "Not visible to any users"
                );
            } else {
                return new NSFWVisibilityRules(
                        true, true, "HIDDEN",
                        "Visibility unknown - treating as hidden"
                );
            }
        }
    }
}




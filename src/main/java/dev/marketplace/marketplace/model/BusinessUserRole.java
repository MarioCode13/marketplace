package dev.marketplace.marketplace.model;

public enum BusinessUserRole {
    OWNER,      // Can edit business details, branding, manage team
    MANAGER,    // Can manage team, create listings, cannot edit business details
    CONTRIBUTOR // Can create listings only
}

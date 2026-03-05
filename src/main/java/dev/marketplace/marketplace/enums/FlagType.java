package dev.marketplace.marketplace.enums;

public enum FlagType {
    NSFW_IMAGE("NSFW Content Detected"),
    PROBLEMATIC_SLUG("Problematic or Reserved Slug");

    private final String displayName;

    FlagType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}


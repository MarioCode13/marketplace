package dev.marketplace.marketplace.model;

import dev.marketplace.marketplace.enums.Condition;
import dev.marketplace.marketplace.enums.ContentApprovalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "listing")
@NoArgsConstructor
public class Listing {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "listing_image", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "image")
    private List<String> images;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private double price;
    private boolean sold = false;
    private int quantity = 1;
    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;

    @Column(name = "custom_city")
    private String customCity;

    @Enumerated(EnumType.STRING)
    private Condition condition;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id")
    private Business business;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy; // Tracks who created the listing (for audit)

    private boolean archived = false; // Archived state for expiry/deletion
    private LocalDateTime soldAt; // Timestamp when listing was sold (nullable)
    private java.math.BigDecimal soldPrice; // Price at which it was sold (nullable)

    // NSFW Content Tracking
    @Column(name = "nsfw_flagged")
    private boolean nsfwFlagged = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "nsfw_approval_status")
    private ContentApprovalStatus nsfwApprovalStatus;

    @Column(name = "nsfw_review_notes", columnDefinition = "TEXT")
    private String nsfwReviewNotes;

    @Column(name = "nsfw_reviewed_at")
    private LocalDateTime nsfwReviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nsfw_reviewed_by")
    private User nsfwReviewedBy;

    @PrePersist
    public void setExpiration() {
        this.expiresAt = this.createdAt.plusDays(30);
    }

    // NSFW Getter Methods
    public boolean getNsfwFlagged() {
        return nsfwFlagged;
    }

    public ContentApprovalStatus getNsfwApprovalStatus() {
        return nsfwApprovalStatus;
    }

    private Listing(Builder builder) {
        this.id = builder.id;
        this.user = builder.user;
        this.title = builder.title;
        this.description = builder.description;
        this.images = builder.images;
        this.category = builder.category;
        this.price = builder.price;
        this.sold = builder.sold;
        this.quantity = builder.quantity;
        this.city = builder.city;
        this.customCity = builder.customCity;
        this.condition = builder.condition;
        this.createdAt = builder.createdAt;
        this.expiresAt = builder.expiresAt;
        this.business = builder.business;
        this.createdBy = builder.createdBy;
        this.archived = builder.archived;
        this.soldAt = builder.soldAt;
        this.soldPrice = builder.soldPrice;
        this.nsfwFlagged = builder.nsfwFlagged;
        this.nsfwApprovalStatus = builder.nsfwApprovalStatus;
        this.nsfwReviewNotes = builder.nsfwReviewNotes;
        this.nsfwReviewedAt = builder.nsfwReviewedAt;
        this.nsfwReviewedBy = builder.nsfwReviewedBy;
    }

    public static class Builder {
        private UUID id;
        private User user;
        private String title;
        private String description;
        private List<String> images;
        private Category category;
        private double price;
        private boolean sold = false;
        private City city;
        private String customCity;
        private Condition condition;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime expiresAt;
        private Business business;
        private User createdBy; // Tracks who created the listing (for audit)
        private boolean archived = false; // Archived state for expiry/deletion
        private int quantity = 1;
        private LocalDateTime soldAt;
        private java.math.BigDecimal soldPrice;
        private boolean nsfwFlagged = false;
        private ContentApprovalStatus nsfwApprovalStatus;
        private String nsfwReviewNotes;
        private LocalDateTime nsfwReviewedAt;
        private User nsfwReviewedBy;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder images(List<String> images) {
            this.images = images;
            return this;
        }

        public Builder category(Category category) {
            this.category = category;
            return this;
        }

        public Builder price(double price) {
            this.price = price;
            return this;
        }

        public Builder sold(boolean sold) {
            this.sold = sold;
            return this;
        }

        public Builder city(City city) {
            this.city = city;
            return this;
        }

        public Builder customCity(String customCity) {
            this.customCity = customCity;
            return this;
        }

        public Builder condition(Condition condition) {
            this.condition = condition;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder business(Business business) {
            this.business = business;
            return this;
        }

        public Builder createdBy(User createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder archived(boolean archived) {
            this.archived = archived;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder soldAt(LocalDateTime soldAt) {
            this.soldAt = soldAt;
            return this;
        }

        public Builder soldPrice(java.math.BigDecimal soldPrice) {
            this.soldPrice = soldPrice;
            return this;
        }

        public Builder nsfwFlagged(boolean nsfwFlagged) {
            this.nsfwFlagged = nsfwFlagged;
            return this;
        }

        public Builder nsfwApprovalStatus(ContentApprovalStatus nsfwApprovalStatus) {
            this.nsfwApprovalStatus = nsfwApprovalStatus;
            return this;
        }

        public Builder nsfwReviewNotes(String nsfwReviewNotes) {
            this.nsfwReviewNotes = nsfwReviewNotes;
            return this;
        }

        public Builder nsfwReviewedAt(LocalDateTime nsfwReviewedAt) {
            this.nsfwReviewedAt = nsfwReviewedAt;
            return this;
        }

        public Builder nsfwReviewedBy(User nsfwReviewedBy) {
            this.nsfwReviewedBy = nsfwReviewedBy;
            return this;
        }

        public Listing build() {
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException("Title is required");
            }
            if (user == null) {
                throw new IllegalArgumentException("User is required");
            }
            if (category == null) {
                throw new IllegalArgumentException("Category is required");
            }
            if (price <= 0) {
                throw new IllegalArgumentException("Price must be positive");
            }
            if (this.quantity < 0) {
                throw new IllegalArgumentException("Quantity cannot be negative");
            }

            if (this.expiresAt == null) {
                this.expiresAt = this.createdAt.plusDays(30);
            }
            
            return new Listing(this);
        }
    }
}

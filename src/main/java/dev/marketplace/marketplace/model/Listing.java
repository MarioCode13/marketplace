package dev.marketplace.marketplace.model;

import dev.marketplace.marketplace.enums.Condition;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "listing")
@NoArgsConstructor
public class Listing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
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
    @Column(columnDefinition = "TEXT")
    private String location;

    @Enumerated(EnumType.STRING)
    private Condition condition;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime expiresAt;

    @PrePersist
    public void setExpiration() {
        this.expiresAt = this.createdAt.plusDays(30);
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
        this.location = builder.location;
        this.condition = builder.condition;
        this.createdAt = builder.createdAt;
        this.expiresAt = builder.expiresAt;
    }

    public static class Builder {
        private Long id;
        private User user;
        private String title;
        private String description;
        private List<String> images;
        private Category category;
        private double price;
        private boolean sold = false;
        private String location;
        private Condition condition;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime expiresAt;

        public Builder id(Long id) {
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

        public Builder location(String location) {
            this.location = location;
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
            
            if (this.expiresAt == null) {
                this.expiresAt = this.createdAt.plusDays(30);
            }
            
            return new Listing(this);
        }
    }
}

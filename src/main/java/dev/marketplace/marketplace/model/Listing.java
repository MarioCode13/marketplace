package dev.marketplace.marketplace.model;

import dev.marketplace.marketplace.enums.Condition;
import jakarta.persistence.*;
import jdk.jshell.Snippet;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "listing")
@Getter
@Setter
@AllArgsConstructor
public class Listing {
    public Listing() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String title;
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
    private String location;

    @Enumerated(EnumType.STRING)
    private Condition condition;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime expiresAt;



    @PrePersist
    public void setExpiration() {
        this.expiresAt = this.createdAt.plusDays(30);
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isSold() {
        return sold;
    }

    public void setSold(boolean sold) {
        this.sold = sold;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    // Private constructor to enforce the use of the builder
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
            if (this.expiresAt == null) {
                this.expiresAt = this.createdAt.plusDays(30);
            }
            return new Listing(this);
        }
    }


}

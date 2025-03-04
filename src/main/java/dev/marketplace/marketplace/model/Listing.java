package dev.marketplace.marketplace.model;

import dev.marketplace.marketplace.enums.Condition;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String title;
    private String description;

    @ElementCollection
    private List<String> images;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToMany
    @JoinTable(
            name = "listing_subcategories",
            joinColumns = @JoinColumn(name = "listing_id"),
            inverseJoinColumns = @JoinColumn(name = "subcategory_id")
    )
    private List<Subcategory> subcategories;

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

    public Listing(Long id, User user, String title, String description, List<String> images,
                   Category category, List<Subcategory> subcategories, double price,
                   boolean sold, String location, Condition condition,
                   LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.user = user;
        this.title = title;
        this.description = description;
        this.images = images;
        this.category = category;
        this.subcategories = subcategories;
        this.price = price;
        this.sold = sold;
        this.location = location;
        this.condition = condition;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    // ✅ Manually Implemented Builder Pattern
    public static ListingBuilder builder() {
        return new ListingBuilder();
    }

    public static class ListingBuilder {
        private Long id;
        private User user;
        private String title;
        private String description;
        private List<String> images;
        private Category category;
        private List<Subcategory> subcategories;
        private double price;
        private boolean sold;
        private String location;
        private Condition condition;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime expiresAt;

        public ListingBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ListingBuilder user(User user) {
            this.user = user;
            return this;
        }

        public ListingBuilder title(String title) {
            this.title = title;
            return this;
        }

        public ListingBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ListingBuilder images(List<String> images) {
            this.images = images;
            return this;
        }

        public ListingBuilder category(Category category) {
            this.category = category;
            return this;
        }

        public ListingBuilder subcategories(List<Subcategory> subcategories) {
            this.subcategories = subcategories;
            return this;
        }

        public ListingBuilder price(double price) {
            this.price = price;
            return this;
        }

        public ListingBuilder sold(boolean sold) {
            this.sold = sold;
            return this;
        }

        public ListingBuilder location(String location) {
            this.location = location;
            return this;
        }

        public ListingBuilder condition(Condition condition) {
            this.condition = condition;
            return this;
        }

        public ListingBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ListingBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Listing build() {
            return new Listing(id, user, title, description, images, category, subcategories, price,
                    sold, location, condition, createdAt, expiresAt);
        }
    }
}

package dev.marketplace.marketplace.model;

import dev.marketplace.marketplace.enums.Condition;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // The user who posted the listing

    private String title;
    private String description;

    @ElementCollection
    private List<String> images; // Store Base64 images as strings for now

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category; // Primary category

    @ManyToMany
    @JoinTable(
            name = "listing_subcategories",
            joinColumns = @JoinColumn(name = "listing_id"),
            inverseJoinColumns = @JoinColumn(name = "subcategory_id")
    )
    private List<Subcategory> subcategories; // Multiple subcategories

    private double price;
    private boolean sold = false;

    private String location; // Free-text location field

    @Enumerated(EnumType.STRING)
    private Condition condition; // Enum for NEW/USED

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime expiresAt; // Auto-expire field

    @PrePersist
    public void setExpiration() {
        this.expiresAt = this.createdAt.plusDays(30); // Listings expire in 30 days
    }
}

package dev.marketplace.marketplace.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "review")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_user_id", nullable = false)
    private User reviewedUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;
    
    @Column(name = "rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;
    
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "is_positive", nullable = false)
    private Boolean isPositive;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Automatically set isPositive based on rating
        if (rating != null) {
            isPositive = rating.compareTo(BigDecimal.valueOf(3.5)) >= 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Update isPositive if rating changed
        if (rating != null) {
            isPositive = rating.compareTo(BigDecimal.valueOf(3.5)) >= 0;
        }
    }
    
    // Helper method to get star display
    public String getStarDisplay() {
        if (rating == null) return "0.0";
        return rating.toString();
    }
    
    // Helper method to check if rating is valid
    public boolean isValidRating() {
        return rating != null && 
               rating.compareTo(BigDecimal.valueOf(0.5)) >= 0 && 
               rating.compareTo(BigDecimal.valueOf(5.0)) <= 0;
    }
} 
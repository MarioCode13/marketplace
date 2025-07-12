package dev.marketplace.marketplace.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "profile_completion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileCompletion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(name = "has_profile_photo", nullable = false)
    @Builder.Default
    private Boolean hasProfilePhoto = false;
    
    @Column(name = "has_bio", nullable = false)
    @Builder.Default
    private Boolean hasBio = false;
    
    @Column(name = "has_contact_number", nullable = false)
    @Builder.Default
    private Boolean hasContactNumber = false;
    
    @Column(name = "has_location", nullable = false)
    @Builder.Default
    private Boolean hasLocation = false;
    
    @Column(name = "has_verified_email", nullable = false)
    @Builder.Default
    private Boolean hasVerifiedEmail = false;
    
    @Column(name = "has_verified_phone", nullable = false)
    @Builder.Default
    private Boolean hasVerifiedPhone = false;
    
    @Column(name = "has_id_verification", nullable = false)
    @Builder.Default
    private Boolean hasIdVerification = false;
    
    @Column(name = "has_address_verification", nullable = false)
    @Builder.Default
    private Boolean hasAddressVerification = false;
    
    @Column(name = "completion_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal completionPercentage = BigDecimal.ZERO;
    
    @Column(name = "last_calculated")
    private LocalDateTime lastCalculated;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastCalculated = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Helper method to calculate completion percentage
    public void calculateCompletionPercentage() {
        int totalFields = 8; // Total number of completion fields
        int completedFields = 0;
        
        if (Boolean.TRUE.equals(hasProfilePhoto)) completedFields++;
        if (Boolean.TRUE.equals(hasBio)) completedFields++;
        if (Boolean.TRUE.equals(hasContactNumber)) completedFields++;
        if (Boolean.TRUE.equals(hasLocation)) completedFields++;
        if (Boolean.TRUE.equals(hasVerifiedEmail)) completedFields++;
        if (Boolean.TRUE.equals(hasVerifiedPhone)) completedFields++;
        if (Boolean.TRUE.equals(hasIdVerification)) completedFields++;
        if (Boolean.TRUE.equals(hasAddressVerification)) completedFields++;
        
        this.completionPercentage = BigDecimal.valueOf(completedFields)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalFields), 2, BigDecimal.ROUND_HALF_UP);
        
        this.lastCalculated = LocalDateTime.now();
    }
    
    // Helper method to get completion level
    public String getCompletionLevel() {
        if (completionPercentage == null) return "UNKNOWN";
        
        int percentage = completionPercentage.intValue();
        if (percentage >= 90) return "EXCELLENT";
        if (percentage >= 80) return "VERY_GOOD";
        if (percentage >= 70) return "GOOD";
        if (percentage >= 60) return "FAIR";
        if (percentage >= 50) return "POOR";
        return "VERY_POOR";
    }
} 
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
    
    @Column(name = "has_id_document", nullable = false)
    @Builder.Default
    private Boolean hasIdDocument = false;

    @Column(name = "has_drivers_license", nullable = false)
    @Builder.Default
    private Boolean hasDriversLicense = false;

    @Column(name = "has_proof_of_address", nullable = false)
    @Builder.Default
    private Boolean hasProofOfAddress = false;
    
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
        int totalFields = 4; // user info fields: photo, bio, contact, location
        int completedFields = 0;
        if (Boolean.TRUE.equals(hasProfilePhoto)) completedFields++;
        if (Boolean.TRUE.equals(hasBio)) completedFields++;
        if (Boolean.TRUE.equals(hasContactNumber)) completedFields++;
        if (Boolean.TRUE.equals(hasLocation)) completedFields++;
        // Uploaded docs (ID, driver's license, proof of address)
        int totalDocs = 3;
        int uploadedDocs = 0;
        if (Boolean.TRUE.equals(hasIdDocument)) uploadedDocs++;
        if (Boolean.TRUE.equals(hasDriversLicense)) uploadedDocs++;
        if (Boolean.TRUE.equals(hasProofOfAddress)) uploadedDocs++;
        int total = totalFields + totalDocs;
        int complete = completedFields + uploadedDocs;
        this.completionPercentage = BigDecimal.valueOf(complete)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, BigDecimal.ROUND_HALF_UP);
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
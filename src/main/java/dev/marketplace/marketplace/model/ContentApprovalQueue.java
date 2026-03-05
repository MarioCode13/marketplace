package dev.marketplace.marketplace.model;

import dev.marketplace.marketplace.enums.ContentApprovalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Admin approval queue for NSFW or problematic content
 * Tracks flagged listings that need admin review
 */
@Entity
@Table(name = "content_approval_queue")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ContentApprovalQueue {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing; // The listing being reviewed

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlagType flagType; // What type of flag is this

    @Column(name = "flagged_data", columnDefinition = "TEXT")
    private String flaggedData; // JSON: images that triggered NSFW, or slug details

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentApprovalStatus status = ContentApprovalStatus.PENDING;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes; // Admin notes on decision

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt; // When admin made decision

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy; // Which admin approved/declined

    /**
     * Type of flag
     */
    public enum FlagType {
        NSFW_IMAGE("Flagged by NSFW detection (images)"),
        PROBLEMATIC_SLUG("Problematic slug (similar to reserved brand)");

        private final String description;

        FlagType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}


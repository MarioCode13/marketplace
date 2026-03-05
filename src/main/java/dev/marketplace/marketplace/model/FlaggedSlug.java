package dev.marketplace.marketplace.model;

import dev.marketplace.marketplace.enums.FlaggedSlugStatus;
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
 * Tracks slugs that have been flagged for review (e.g., too similar to known brands)
 */
@Entity
@Table(name = "flagged_slug")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class FlaggedSlug {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String slug; // The problematic slug value

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business; // The business that tried to use this slug

    @Column(columnDefinition = "TEXT")
    private String reason; // Why it was flagged (e.g., "Similar to reserved brand name: Nike")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlaggedSlugStatus status = FlaggedSlugStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy; // Which admin reviewed it

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes; // Admin notes

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt; // When admin made decision
}


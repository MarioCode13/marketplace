package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.VerificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationDocumentRepository extends JpaRepository<VerificationDocument, UUID> {

    List<VerificationDocument> findByUserId(UUID userId);

    List<VerificationDocument> findByUserIdAndStatus(UUID userId, VerificationDocument.VerificationStatus status);

    @Query("SELECT vd FROM VerificationDocument vd WHERE vd.status = :status")
    List<VerificationDocument> findByStatus(@Param("status") VerificationDocument.VerificationStatus status);
    
    @Query("SELECT vd FROM VerificationDocument vd WHERE vd.user.id = :userId AND vd.documentType = :documentType")
    Optional<VerificationDocument> findByUserIdAndDocumentType(@Param("userId") UUID userId,
                                                              @Param("documentType") VerificationDocument.DocumentType documentType);
    
    @Query("SELECT COUNT(vd) FROM VerificationDocument vd WHERE vd.user.id = :userId AND vd.status = 'APPROVED'")
    Long countApprovedDocumentsByUserId(@Param("userId") UUID userId);

    @Query("SELECT vd FROM VerificationDocument vd WHERE vd.status = 'PENDING' ORDER BY vd.createdAt ASC")
    List<VerificationDocument> findPendingDocuments();
    
    boolean existsByUserIdAndDocumentType(UUID userId, VerificationDocument.DocumentType documentType);

    List<VerificationDocument> findByBusinessId(UUID businessId);

    List<VerificationDocument> findByBusinessIdAndStatus(UUID businessId, VerificationDocument.VerificationStatus status);

    @Query("SELECT vd FROM VerificationDocument vd WHERE vd.business.id = :businessId AND vd.documentType = :documentType")
    Optional<VerificationDocument> findByBusinessIdAndDocumentType(@Param("businessId") UUID businessId,
                                                                  @Param("documentType") VerificationDocument.DocumentType documentType);

    @Query("SELECT COUNT(vd) FROM VerificationDocument vd WHERE vd.business.id = :businessId AND vd.status = 'APPROVED'")
    Long countApprovedDocumentsByBusinessId(@Param("businessId") UUID businessId);

    boolean existsByBusinessIdAndDocumentType(UUID businessId, VerificationDocument.DocumentType documentType);
}

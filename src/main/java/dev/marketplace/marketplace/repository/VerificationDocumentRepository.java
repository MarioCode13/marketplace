package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.VerificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationDocumentRepository extends JpaRepository<VerificationDocument, Long> {
    
    List<VerificationDocument> findByUserId(Long userId);
    
    List<VerificationDocument> findByUserIdAndStatus(Long userId, VerificationDocument.VerificationStatus status);
    
    @Query("SELECT vd FROM VerificationDocument vd WHERE vd.status = :status")
    List<VerificationDocument> findByStatus(@Param("status") VerificationDocument.VerificationStatus status);
    
    @Query("SELECT vd FROM VerificationDocument vd WHERE vd.user.id = :userId AND vd.documentType = :documentType")
    Optional<VerificationDocument> findByUserIdAndDocumentType(@Param("userId") Long userId, 
                                                              @Param("documentType") VerificationDocument.DocumentType documentType);
    
    @Query("SELECT COUNT(vd) FROM VerificationDocument vd WHERE vd.user.id = :userId AND vd.status = 'APPROVED'")
    Long countApprovedDocumentsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT vd FROM VerificationDocument vd WHERE vd.status = 'PENDING' ORDER BY vd.createdAt ASC")
    List<VerificationDocument> findPendingDocuments();
    
    boolean existsByUserIdAndDocumentType(Long userId, VerificationDocument.DocumentType documentType);
} 
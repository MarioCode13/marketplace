package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.VerificationDocument;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.BusinessRepository;
import dev.marketplace.marketplace.repository.VerificationDocumentRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationDocumentService {
    
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final UserRepository userRepository;
    private final TrustRatingService trustRatingService;
    private final B2StorageService b2StorageService;
    private final BusinessRepository businessRepository;

    @Transactional
    public VerificationDocument uploadDocument(UUID userId,
                                             VerificationDocument.DocumentType documentType,
                                             byte[] documentData, 
                                             String fileName) {
        log.info("Uploading document for user: {}, type: {}", userId, documentType);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        // Check if document of this type already exists
        Optional<VerificationDocument> existingDoc = verificationDocumentRepository
                .findByUserIdAndDocumentType(userId, documentType);
        
        if (existingDoc.isPresent()) {
            throw new IllegalArgumentException("Document of type " + documentType + " already exists for user " + userId);
        }
        
        String documentUrl;
        try {
            // Upload to B2 storage
            documentUrl = b2StorageService.uploadImage(fileName, documentData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document to storage", e);
        }
        
        // Create verification document
        VerificationDocument document = VerificationDocument.builder()
                .user(user)
                .documentType(documentType)
                .documentUrl(documentUrl)
                .status(VerificationDocument.VerificationStatus.PENDING)
                .build();
        
        VerificationDocument saved = verificationDocumentRepository.save(document);
        
        // Update trust rating (upload gives immediate points)
        trustRatingService.calculateAndUpdateTrustRating(userId);
        
        log.info("Document uploaded successfully for user: {}, type: {}, status: {}", 
                userId, documentType, saved.getStatus());
        
        return saved;
    }
    
    @Transactional
    public VerificationDocument verifyDocument(UUID documentId,
                                             UUID adminUserId,
                                             VerificationDocument.VerificationStatus status,
                                             String rejectionReason) {
        log.info("Verifying document: {}, by admin: {}, status: {}", documentId, adminUserId, status);
        
        VerificationDocument document = verificationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));
        
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found with ID: " + adminUserId));
        
        // Update document status
        document.setStatus(status);
        document.setVerifiedBy(admin);
        
        if (status == VerificationDocument.VerificationStatus.APPROVED) {
            document.setVerifiedAt(java.time.LocalDateTime.now());
            document.setRejectionReason(null);
        } else if (status == VerificationDocument.VerificationStatus.REJECTED) {
            document.setRejectionReason(rejectionReason);
        }
        
        VerificationDocument saved = verificationDocumentRepository.save(document);
        
        // Update trust rating (verification gives bonus points)
        trustRatingService.calculateAndUpdateTrustRating(document.getUser().getId());
        
        log.info("Document verification completed: {}, status: {}", documentId, status);
        
        return saved;
    }
    
    @Transactional(readOnly = true)
    public List<VerificationDocument> getUserDocuments(UUID userId) {
        return verificationDocumentRepository.findByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public List<VerificationDocument> getPendingDocuments() {
        return verificationDocumentRepository.findPendingDocuments();
    }
    
    @Transactional(readOnly = true)
    public Optional<VerificationDocument> getDocument(UUID documentId) {
        return verificationDocumentRepository.findById(documentId);
    }
    
    @Transactional(readOnly = true)
    public Optional<VerificationDocument> getUserDocumentByType(UUID userId, VerificationDocument.DocumentType documentType) {
        return verificationDocumentRepository.findByUserIdAndDocumentType(userId, documentType);
    }
    
    @Transactional
    public void deleteDocument(UUID documentId, UUID userId) {
        VerificationDocument document = verificationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));
        
        // Check if user owns the document
        if (!document.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("User not authorized to delete this document");
        }
        
        try {
            // Delete from B2 storage
            b2StorageService.deleteImage(document.getDocumentUrl());
        } catch (Exception e) {
            // Log and continue
            log.warn("Failed to delete document from storage: {}", document.getDocumentUrl(), e);
        }
        
        verificationDocumentRepository.delete(document);
        
        // Update trust rating (removing document reduces points)
        trustRatingService.calculateAndUpdateTrustRating(userId);
        
        log.info("Document deleted: {}", documentId);
    }
    
    @Transactional(readOnly = true)
    public Long getApprovedDocumentsCount(UUID userId) {
        return verificationDocumentRepository.countApprovedDocumentsByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public boolean hasDocumentOfType(UUID userId, VerificationDocument.DocumentType documentType) {
        return verificationDocumentRepository.existsByUserIdAndDocumentType(userId, documentType);
    }
    
    @Transactional(readOnly = true)
    public boolean hasApprovedDocumentOfType(UUID userId, VerificationDocument.DocumentType documentType) {
        Optional<VerificationDocument> doc = verificationDocumentRepository
                .findByUserIdAndDocumentType(userId, documentType);
        return doc.isPresent() && doc.get().getStatus() == VerificationDocument.VerificationStatus.APPROVED;
    }

    @Transactional
    public VerificationDocument uploadBusinessDocument(UUID businessId,
                                                      VerificationDocument.DocumentType documentType,
                                                      byte[] documentData,
                                                      String fileName) {
        log.info("Uploading document for business: {}, type: {}", businessId, documentType);
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found with ID: " + businessId));
        Optional<VerificationDocument> existingDoc = verificationDocumentRepository
                .findByBusinessIdAndDocumentType(businessId, documentType);
        if (existingDoc.isPresent()) {
            throw new IllegalArgumentException("Document of type " + documentType + " already exists for business " + businessId);
        }
        String documentUrl;
        try {
            documentUrl = b2StorageService.uploadImage(fileName, documentData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document to storage", e);
        }
        VerificationDocument document = VerificationDocument.builder()
                .business(business)
                .documentType(documentType)
                .documentUrl(documentUrl)
                .status(VerificationDocument.VerificationStatus.PENDING)
                .build();
        VerificationDocument saved = verificationDocumentRepository.save(document);
        trustRatingService.calculateAndUpdateBusinessTrustRating(businessId);
        log.info("Document uploaded successfully for business: {}, type: {}, status: {}", businessId, documentType, saved.getStatus());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<VerificationDocument> getBusinessDocuments(UUID businessId) {
        return verificationDocumentRepository.findByBusinessId(businessId);
    }

    @Transactional(readOnly = true)
    public Optional<VerificationDocument> getBusinessDocumentByType(UUID businessId, VerificationDocument.DocumentType documentType) {
        return verificationDocumentRepository.findByBusinessIdAndDocumentType(businessId, documentType);
    }

    @Transactional
    public void deleteBusinessDocument(UUID documentId, UUID businessId) {
        VerificationDocument document = verificationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));
        if (document.getBusiness() == null || !document.getBusiness().getId().equals(businessId)) {
            throw new IllegalArgumentException("Business not authorized to delete this document");
        }
        try {
            b2StorageService.deleteImage(document.getDocumentUrl());
        } catch (Exception e) {
            log.warn("Failed to delete document from storage: {}", document.getDocumentUrl(), e);
        }
        verificationDocumentRepository.delete(document);
        trustRatingService.calculateAndUpdateBusinessTrustRating(businessId);
        log.info("Business document deleted: {}", documentId);
    }

    @Transactional(readOnly = true)
    public Long getApprovedDocumentsCountForBusiness(UUID businessId) {
        return verificationDocumentRepository.countApprovedDocumentsByBusinessId(businessId);
    }

    @Transactional(readOnly = true)
    public boolean hasBusinessDocumentOfType(UUID businessId, VerificationDocument.DocumentType documentType) {
        return verificationDocumentRepository.existsByBusinessIdAndDocumentType(businessId, documentType);
    }

    @Transactional(readOnly = true)
    public boolean hasApprovedBusinessDocumentOfType(UUID businessId, VerificationDocument.DocumentType documentType) {
        Optional<VerificationDocument> doc = verificationDocumentRepository
                .findByBusinessIdAndDocumentType(businessId, documentType);
        return doc.isPresent() && doc.get().getStatus() == VerificationDocument.VerificationStatus.APPROVED;
    }
}

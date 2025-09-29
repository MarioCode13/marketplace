package dev.marketplace.marketplace.service;
import dev.marketplace.marketplace.model.VerificationDocument;
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
    
    @Transactional(readOnly = true)
    public List<VerificationDocument> getBusinessDocuments(UUID businessId) {
        return verificationDocumentRepository.findByBusinessId(businessId);
    }

    @Transactional(readOnly = true)
    public Optional<VerificationDocument> getBusinessDocumentByType(UUID businessId, VerificationDocument.DocumentType documentType) {
        return verificationDocumentRepository.findByBusinessIdAndDocumentType(businessId, documentType);
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

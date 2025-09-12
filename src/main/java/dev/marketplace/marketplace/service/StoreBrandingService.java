package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.StoreBranding;
import dev.marketplace.marketplace.repository.StoreBrandingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreBrandingService {
    
    private final StoreBrandingRepository storeBrandingRepository;
    
    public Optional<StoreBranding> findByBusiness(Business business) {
        return storeBrandingRepository.findByBusiness(business);
    }
    
    public Optional<StoreBranding> findBySlug(String slug) {
        return storeBrandingRepository.findBySlug(slug);
    }
    
    @Transactional
    public StoreBranding save(StoreBranding storeBranding) {
        log.info("Saving store branding for business: {}", storeBranding.getBusiness().getId());
        return storeBrandingRepository.save(storeBranding);
    }
    
    @Transactional
    public void delete(StoreBranding storeBranding) {
        log.info("Deleting store branding for business: {}", storeBranding.getBusiness().getId());
        storeBrandingRepository.delete(storeBranding);
    }
}

package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.enums.BusinessUserRole;
import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.StoreBranding;
import dev.marketplace.marketplace.repository.BusinessRepository;
import dev.marketplace.marketplace.repository.StoreBrandingRepository;
import dev.marketplace.marketplace.repository.BusinessUserRepository;
import dev.marketplace.marketplace.model.BusinessUser;
import dev.marketplace.marketplace.repository.UserRepository;
import dev.marketplace.marketplace.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreBrandingService {
    
    private final StoreBrandingRepository storeBrandingRepository;
    private final BusinessRepository businessRepository;
    private final BusinessUserRepository businessUserRepository;
    private final UserRepository userRepository;

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

    @Transactional
    public StoreBranding updateStoreBranding(UUID userId, UUID businessId, String slug, String logoUrl, String bannerUrl, String themeColor, String primaryColor, String secondaryColor, String lightOrDark, String about, String storeName, String backgroundColor, String textColor, String cardTextColor) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new IllegalArgumentException("Business not found"));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // Authorization check: must be OWNER or MANAGER
        BusinessUser businessUser = businessUserRepository.findByBusinessAndUser(business, user)
            .orElseThrow(() -> new AccessDeniedException("User is not associated with this business"));
        if (!(businessUser.getRole() == BusinessUserRole.OWNER || businessUser.getRole() == BusinessUserRole.MANAGER)) {
            throw new AccessDeniedException("User does not have permission to update branding for this business");
        }
        StoreBranding storeBranding = findByBusiness(business)
            .orElseGet(() -> {
                StoreBranding sb = new StoreBranding();
                sb.setBusiness(business);
                sb.setBusinessId(business.getId());
                return sb;
            });
        storeBranding.setSlug(slug);
        storeBranding.setLogoUrl(logoUrl);
        storeBranding.setBannerUrl(bannerUrl);
        storeBranding.setThemeColor(themeColor);
        storeBranding.setPrimaryColor(primaryColor);
        storeBranding.setSecondaryColor(secondaryColor);
        storeBranding.setLightOrDark(lightOrDark);
        storeBranding.setAbout(about);
        storeBranding.setStoreName(storeName);
        storeBranding.setBackgroundColor(backgroundColor);
        storeBranding.setTextColor(textColor);
        storeBranding.setCardTextColor(cardTextColor);
        return save(storeBranding);
    }
}

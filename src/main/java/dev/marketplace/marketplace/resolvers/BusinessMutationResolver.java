package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.dto.UpdateBusinessInput;
import dev.marketplace.marketplace.dto.UpdateStoreBrandingInput;
import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.BusinessUser;
import dev.marketplace.marketplace.model.BusinessUserRole;
import dev.marketplace.marketplace.model.StoreBranding;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.service.BusinessService;
import dev.marketplace.marketplace.service.CityService;
import dev.marketplace.marketplace.service.StoreBrandingService;
import dev.marketplace.marketplace.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BusinessMutationResolver {
    
    private final BusinessService businessService;
    private final StoreBrandingService storeBrandingService;
    private final UserService userService;
    private final CityService cityService;
    
    @MutationMapping
    @Transactional
    public Business createBusiness(@Argument String name, 
                                 @Argument String email, 
                                 @Argument String contactNumber,
                                 @Argument String addressLine1,
                                 @Argument String addressLine2,
                                 @Argument Long cityId,
                                 @Argument String postalCode) {
        User currentUser = getCurrentUser();
        log.info("Creating business: {} for user: {}", name, currentUser.getId());
        
        Business business = new Business();
        business.setName(name);
        business.setEmail(email);
        business.setContactNumber(contactNumber);
        business.setAddressLine1(addressLine1);
        business.setAddressLine2(addressLine2);
        business.setPostalCode(postalCode);
        business.setOwner(currentUser);
        
        if (cityId != null) {
            business.setCity(cityService.getCityById(cityId));
        }
        
        return businessService.createBusiness(business);
    }
    
    @MutationMapping
    @Transactional
    public Business updateBusinessAndBranding(@Argument UpdateBusinessInput business, 
                                            @Argument UpdateStoreBrandingInput branding) {
        User currentUser = getCurrentUser();
        log.info("Updating business and branding for business: {}", business.getBusinessId());
        
        // Update business details
        Business existingBusiness = businessService.findById(business.getBusinessId())
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + business.getBusinessId()));
        
        // Update business fields
        if (business.getEmail() != null) existingBusiness.setEmail(business.getEmail());
        if (business.getContactNumber() != null) existingBusiness.setContactNumber(business.getContactNumber());
        if (business.getAddressLine1() != null) existingBusiness.setAddressLine1(business.getAddressLine1());
        if (business.getAddressLine2() != null) existingBusiness.setAddressLine2(business.getAddressLine2());
        if (business.getPostalCode() != null) existingBusiness.setPostalCode(business.getPostalCode());
        if (business.getCityId() != null) {
            existingBusiness.setCity(cityService.getCityById(business.getCityId()));
        }
        
        Business updatedBusiness = businessService.updateBusiness(existingBusiness, currentUser);
        
        // Update branding
        if (branding != null) {
            StoreBranding existingBranding = storeBrandingService.findByBusiness(updatedBusiness)
                    .orElse(new StoreBranding());
            
            existingBranding.setBusiness(updatedBusiness);
            if (branding.getSlug() != null) existingBranding.setSlug(branding.getSlug());
            if (branding.getLogoUrl() != null) existingBranding.setLogoUrl(branding.getLogoUrl());
            if (branding.getBannerUrl() != null) existingBranding.setBannerUrl(branding.getBannerUrl());
            if (branding.getThemeColor() != null) existingBranding.setThemeColor(branding.getThemeColor());
            if (branding.getPrimaryColor() != null) existingBranding.setPrimaryColor(branding.getPrimaryColor());
            if (branding.getSecondaryColor() != null) existingBranding.setSecondaryColor(branding.getSecondaryColor());
            if (branding.getLightOrDark() != null) existingBranding.setLightOrDark(branding.getLightOrDark());
            if (branding.getAbout() != null) existingBranding.setAbout(branding.getAbout());
            if (branding.getStoreName() != null) existingBranding.setStoreName(branding.getStoreName());
            
            storeBrandingService.save(existingBranding);
        }
        
        return updatedBusiness;
    }
    
    @MutationMapping
    @Transactional
    public BusinessUser linkUserToBusiness(@Argument Long businessId, 
                                         @Argument Long userId, 
                                         @Argument String role) {
        User currentUser = getCurrentUser();
        User targetUser = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        BusinessUserRole businessRole = BusinessUserRole.valueOf(role.toUpperCase());
        
        return businessService.linkUserToBusiness(businessId, targetUser, businessRole, currentUser);
    }
    
    @MutationMapping
    @Transactional
    public Boolean unlinkUserFromBusiness(@Argument Long businessId, @Argument Long userId) {
        User currentUser = getCurrentUser();
        User targetUser = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        businessService.unlinkUserFromBusiness(businessId, targetUser, currentUser);
        return true;
    }
    
    @MutationMapping
    @Transactional
    public Boolean transferBusinessOwnership(@Argument Long businessId, @Argument Long newOwnerId) {
        User currentUser = getCurrentUser();
        User newOwner = userService.findById(newOwnerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + newOwnerId));
        
        businessService.transferOwnership(businessId, newOwner, currentUser);
        return true;
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("User not authenticated");
        }
        
        String email = authentication.getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }
}

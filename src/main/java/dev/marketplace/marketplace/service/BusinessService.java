package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.*;
import dev.marketplace.marketplace.repository.BusinessRepository;
import dev.marketplace.marketplace.repository.BusinessUserRepository;
import dev.marketplace.marketplace.repository.StoreBrandingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {
    
    private final BusinessRepository businessRepository;
    private final BusinessUserRepository businessUserRepository;
    private final StoreBrandingRepository storeBrandingRepository;
    
    public Optional<Business> findById(Long id) {
        return businessRepository.findById(id);
    }
    
    public Optional<Business> findByOwner(User owner) {
        return businessRepository.findByOwner(owner);
    }
    
    public List<Business> findByUser(User user) {
        return businessRepository.findByUser(user);
    }
    
    public Optional<Business> findOwnedByUser(User user) {
        return businessRepository.findOwnedByUser(user);
    }
    
    @Transactional
    public Business createBusiness(Business business) {
        log.info("Creating business: {}", business.getName());
        
        // Validate email uniqueness
        if (businessRepository.existsByEmail(business.getEmail())) {
            throw new IllegalArgumentException("Business email already exists: " + business.getEmail());
        }
        
        Business savedBusiness = businessRepository.save(business);
        
        // Create owner relationship
        BusinessUser ownerRelation = new BusinessUser();
        ownerRelation.setBusiness(savedBusiness);
        ownerRelation.setUser(business.getOwner());
        ownerRelation.setRole(BusinessUserRole.OWNER);
        businessUserRepository.save(ownerRelation);
        
        log.info("Created business with ID: {}", savedBusiness.getId());
        return savedBusiness;
    }
    
    @Transactional
    public Business updateBusiness(Business business, User requestingUser) {
        log.info("Updating business: {}", business.getId());
        
        Business existingBusiness = businessRepository.findById(business.getId())
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + business.getId()));
        
        // Check permissions
        if (!existingBusiness.canUserEditBusiness(requestingUser)) {
            throw new IllegalArgumentException("User does not have permission to edit this business");
        }
        
        // Validate email uniqueness if changed
        if (!existingBusiness.getEmail().equals(business.getEmail()) && 
            businessRepository.existsByEmailAndIdNot(business.getEmail(), business.getId())) {
            throw new IllegalArgumentException("Business email already exists: " + business.getEmail());
        }
        
        // Update fields
        existingBusiness.setName(business.getName());
        existingBusiness.setEmail(business.getEmail());
        existingBusiness.setContactNumber(business.getContactNumber());
        existingBusiness.setAddressLine1(business.getAddressLine1());
        existingBusiness.setAddressLine2(business.getAddressLine2());
        existingBusiness.setCity(business.getCity());
        existingBusiness.setPostalCode(business.getPostalCode());
        
        return businessRepository.save(existingBusiness);
    }
    
    @Transactional
    public BusinessUser linkUserToBusiness(Long businessId, User user, BusinessUserRole role, User requestingUser) {
        log.info("Linking user {} to business {} with role {}", user.getId(), businessId, role);
        
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));
        
        // Check permissions (only owner or manager can link users)
        if (!business.canUserEditBusiness(requestingUser)) {
            throw new IllegalArgumentException("User does not have permission to manage this business");
        }
        
        // Check if user is already linked
        if (businessUserRepository.existsByBusinessAndUser(business, user)) {
            throw new IllegalArgumentException("User is already linked to this business");
        }
        
        BusinessUser businessUser = new BusinessUser();
        businessUser.setBusiness(business);
        businessUser.setUser(user);
        businessUser.setRole(role);
        
        return businessUserRepository.save(businessUser);
    }
    
    @Transactional
    public void unlinkUserFromBusiness(Long businessId, User user, User requestingUser) {
        log.info("Unlinking user {} from business {}", user.getId(), businessId);
        
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));
        
        // Check permissions
        if (!business.canUserEditBusiness(requestingUser)) {
            throw new IllegalArgumentException("User does not have permission to manage this business");
        }
        
        // Prevent removing the last owner
        if (business.getOwner().getId().equals(user.getId())) {
            long ownerCount = businessUserRepository.countOwnersByBusiness(business);
            if (ownerCount <= 1) {
                throw new IllegalArgumentException("Cannot remove the last owner from business");
            }
        }
        
        businessUserRepository.deleteByBusinessAndUser(business, user);
    }
    
    @Transactional
    public void transferOwnership(Long businessId, User newOwner, User requestingUser) {
        log.info("Transferring ownership of business {} to user {}", businessId, newOwner.getId());
        
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));
        
        // Only current owner can transfer ownership
        if (!business.isOwner(requestingUser)) {
            throw new IllegalArgumentException("Only the current owner can transfer ownership");
        }
        
        // Check if new owner is already linked to business
        Optional<BusinessUser> existingRelation = businessUserRepository.findByBusinessAndUser(business, newOwner);
        if (existingRelation.isPresent()) {
            // Update existing relation to owner
            existingRelation.get().setRole(BusinessUserRole.OWNER);
            businessUserRepository.save(existingRelation.get());
        } else {
            // Create new owner relation
            BusinessUser newOwnerRelation = new BusinessUser();
            newOwnerRelation.setBusiness(business);
            newOwnerRelation.setUser(newOwner);
            newOwnerRelation.setRole(BusinessUserRole.OWNER);
            businessUserRepository.save(newOwnerRelation);
        }
        
        // Update business owner
        business.setOwner(newOwner);
        businessRepository.save(business);
    }
    
    public List<BusinessUser> getBusinessUsers(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));
        return businessUserRepository.findByBusiness(business);
    }
    
    public boolean canUserCreateListingsForBusiness(User user, Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));
        return business.canUserCreateListings(user);
    }
}

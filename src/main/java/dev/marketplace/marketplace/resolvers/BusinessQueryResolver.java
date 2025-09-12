package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.BusinessUser;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.service.BusinessService;
import dev.marketplace.marketplace.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BusinessQueryResolver {
    
    private final BusinessService businessService;
    private final UserService userService;
    
    @QueryMapping
    public Business business(@Argument Long id) {
        log.info("Fetching business with ID: {}", id);
        return businessService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + id));
    }
    
    @QueryMapping
    public Business myBusiness() {
        User currentUser = getCurrentUser();
        log.info("Fetching business for user: {}", currentUser.getId());
        return businessService.findOwnedByUser(currentUser)
                .orElseThrow(() -> new IllegalArgumentException("No business found for user"));
    }
    
    @QueryMapping
    public List<Business> myBusinesses() {
        User currentUser = getCurrentUser();
        log.info("Fetching all businesses for user: {}", currentUser.getId());
        return businessService.findByUser(currentUser);
    }
    
    @QueryMapping
    public List<BusinessUser> businessUsers(@Argument Long businessId) {
        log.info("Fetching users for business: {}", businessId);
        return businessService.getBusinessUsers(businessId);
    }
    
    @SchemaMapping
    public User owner(Business business) {
        return business.getOwner();
    }
    
    @SchemaMapping
    public List<BusinessUser> businessUsers(Business business) {
        return business.getBusinessUsers();
    }
    
    @SchemaMapping
    public User user(BusinessUser businessUser) {
        return businessUser.getUser();
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

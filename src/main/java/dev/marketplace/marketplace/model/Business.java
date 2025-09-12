package dev.marketplace.marketplace.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "business")
@Data
@EqualsAndHashCode(exclude = {"owner", "city", "businessUsers", "storeBranding"})
@ToString(exclude = {"owner", "city", "businessUsers", "storeBranding"})
@EntityListeners(AuditingEntityListener.class)
public class Business {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String email;
    
    @Column(name = "contact_number")
    private String contactNumber;
    
    @Column(name = "address_line1")
    private String addressLine1;
    
    @Column(name = "address_line2")
    private String addressLine2;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;
    
    @Column(name = "postal_code")
    private String postalCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BusinessUser> businessUsers = new ArrayList<>();
    
    @OneToOne(mappedBy = "business", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private StoreBranding storeBranding;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Helper methods
    public boolean isOwner(User user) {
        return owner != null && owner.getId().equals(user.getId());
    }
    
    public boolean hasUser(User user) {
        return businessUsers.stream()
                .anyMatch(bu -> bu.getUser().getId().equals(user.getId()));
    }
    
    public BusinessUserRole getUserRole(User user) {
        return businessUsers.stream()
                .filter(bu -> bu.getUser().getId().equals(user.getId()))
                .map(BusinessUser::getRole)
                .findFirst()
                .orElse(null);
    }
    
    public boolean canUserEditBusiness(User user) {
        return isOwner(user) || getUserRole(user) == BusinessUserRole.MANAGER;
    }
    
    public boolean canUserCreateListings(User user) {
        BusinessUserRole role = getUserRole(user);
        return role != null && (role == BusinessUserRole.OWNER || 
                               role == BusinessUserRole.MANAGER || 
                               role == BusinessUserRole.CONTRIBUTOR);
    }
}

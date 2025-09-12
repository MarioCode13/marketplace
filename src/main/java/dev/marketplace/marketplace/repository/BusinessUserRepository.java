package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.BusinessUser;
import dev.marketplace.marketplace.model.BusinessUserRole;
import dev.marketplace.marketplace.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessUserRepository extends JpaRepository<BusinessUser, Long> {
    
    Optional<BusinessUser> findByBusinessAndUser(Business business, User user);
    
    List<BusinessUser> findByBusiness(Business business);
    
    List<BusinessUser> findByUser(User user);
    
    @Query("SELECT bu FROM BusinessUser bu WHERE bu.business = :business AND bu.role = :role")
    List<BusinessUser> findByBusinessAndRole(@Param("business") Business business, @Param("role") BusinessUserRole role);
    
    boolean existsByBusinessAndUser(Business business, User user);
    
    void deleteByBusinessAndUser(Business business, User user);
    
    @Query("SELECT COUNT(bu) FROM BusinessUser bu WHERE bu.business = :business AND bu.role = 'OWNER'")
    long countOwnersByBusiness(@Param("business") Business business);
}

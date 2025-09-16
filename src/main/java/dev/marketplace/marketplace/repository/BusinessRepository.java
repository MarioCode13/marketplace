package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.Business;
import dev.marketplace.marketplace.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findByOwner(User owner);
    
    @Query("SELECT b FROM Business b JOIN b.businessUsers bu WHERE bu.user = :user")
    List<Business> findByUser(@Param("user") User user);
    
    @Query("SELECT b FROM Business b JOIN b.businessUsers bu WHERE bu.user = :user AND bu.role = 'OWNER'")
    Optional<Business> findOwnedByUser(@Param("user") User user);
    
    boolean existsByEmail(String email);
    
    boolean existsByEmailAndIdNot(String email, UUID id);
}

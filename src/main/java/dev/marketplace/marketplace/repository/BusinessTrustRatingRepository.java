package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.BusinessTrustRating;
import dev.marketplace.marketplace.model.Business;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessTrustRatingRepository extends JpaRepository<BusinessTrustRating, UUID> {
    Optional<BusinessTrustRating> findByBusiness(Business business);
    Optional<BusinessTrustRating> findByBusinessId(UUID businessId);
}
package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.ProfileCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileCompletionRepository extends JpaRepository<ProfileCompletion, Long> {
    
    Optional<ProfileCompletion> findByUserId(Long userId);
    
    @Query("SELECT pc FROM ProfileCompletion pc WHERE pc.completionPercentage >= :minPercentage")
    List<ProfileCompletion> findByMinimumCompletionPercentage(@Param("minPercentage") BigDecimal minPercentage);
    
    @Query("SELECT AVG(pc.completionPercentage) FROM ProfileCompletion pc")
    BigDecimal getAverageCompletionPercentage();
    
    @Query("SELECT pc FROM ProfileCompletion pc ORDER BY pc.completionPercentage DESC LIMIT :limit")
    List<ProfileCompletion> findTopCompletedProfiles(@Param("limit") int limit);
    
    @Query("SELECT COUNT(pc) FROM ProfileCompletion pc WHERE pc.completionPercentage >= :minPercentage")
    Long countByMinimumCompletionPercentage(@Param("minPercentage") BigDecimal minPercentage);
    
    boolean existsByUserId(Long userId);
} 
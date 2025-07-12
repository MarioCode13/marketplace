package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.TrustRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrustRatingRepository extends JpaRepository<TrustRating, Long> {
    
    Optional<TrustRating> findByUserId(Long userId);
    
    @Query("SELECT tr FROM TrustRating tr WHERE tr.overallScore >= :minScore ORDER BY tr.overallScore DESC")
    List<TrustRating> findByMinimumScore(@Param("minScore") BigDecimal minScore);
    
    @Query("SELECT AVG(tr.overallScore) FROM TrustRating tr")
    BigDecimal getAverageTrustScore();
    
    @Query("SELECT COUNT(tr) FROM TrustRating tr WHERE tr.overallScore >= :minScore")
    Long countByMinimumScore(@Param("minScore") BigDecimal minScore);
    
    @Query("SELECT tr FROM TrustRating tr ORDER BY tr.overallScore DESC LIMIT :limit")
    List<TrustRating> findTopTrustedUsers(@Param("limit") int limit);
    
    boolean existsByUserId(Long userId);
} 
package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findByUserId(Long userId);
    List<Listing> findByCategoryId(Long categoryId);
    List<Listing> findBySoldFalse(); // Get only active listings
}


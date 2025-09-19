package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // Find transactions by listing
    List<Transaction> findByListingId(UUID listingId);
    
    // Find transactions where user is the buyer
    List<Transaction> findByBuyerId(UUID buyerId);

    // Find transactions where user is the seller
    List<Transaction> findBySellerId(UUID sellerId);

    // Find completed transactions by listing
    List<Transaction> findByListingIdAndStatus(UUID listingId, Transaction.TransactionStatus status);
    
    // Find the most recent completed transaction for a listing
    Optional<Transaction> findFirstByListingIdAndStatusOrderBySaleDateDesc(UUID listingId, Transaction.TransactionStatus status);
    
    // Check if a user has bought a specific listing
    boolean existsByListingIdAndBuyerIdAndStatus(UUID listingId, UUID buyerId, Transaction.TransactionStatus status);

    // Get transaction count for a user as buyer
    long countByBuyerIdAndStatus(UUID buyerId, Transaction.TransactionStatus status);

    // Get transaction count for a user as seller
    long countBySellerIdAndStatus(UUID sellerId, Transaction.TransactionStatus status);

    // Find transactions by status
    List<Transaction> findByStatus(Transaction.TransactionStatus status);
    
    // Find transactions by buyer and status
    List<Transaction> findByBuyerIdAndStatus(UUID buyerId, Transaction.TransactionStatus status);

    // Find transactions by seller and status
    List<Transaction> findBySellerIdAndStatus(UUID sellerId, Transaction.TransactionStatus status);

    // Find transactions within a date range
    @Query("SELECT t FROM Transaction t WHERE t.saleDate BETWEEN :startDate AND :endDate")
    List<Transaction> findBySaleDateBetween(@Param("startDate") java.time.LocalDateTime startDate, 
                                           @Param("endDate") java.time.LocalDateTime endDate);

    // Get transaction count for a user as seller (all statuses)
    long countBySellerId(UUID sellerId);
    // Get transaction count for a user as buyer (all statuses)
    long countByBuyerId(UUID buyerId);

    // Find transactions by multiple listings
    List<Transaction> findByListingIdIn(List<UUID> listingIds);
}

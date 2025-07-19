package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // Find transactions by listing
    List<Transaction> findByListingId(Long listingId);
    
    // Find transactions where user is the buyer
    List<Transaction> findByBuyerId(Long buyerId);
    
    // Find transactions where user is the seller
    List<Transaction> findBySellerId(Long sellerId);
    
    // Find completed transactions by listing
    List<Transaction> findByListingIdAndStatus(Long listingId, Transaction.TransactionStatus status);
    
    // Find the most recent completed transaction for a listing
    Optional<Transaction> findFirstByListingIdAndStatusOrderBySaleDateDesc(Long listingId, Transaction.TransactionStatus status);
    
    // Check if a user has bought a specific listing
    boolean existsByListingIdAndBuyerIdAndStatus(Long listingId, Long buyerId, Transaction.TransactionStatus status);
    
    // Get transaction count for a user as buyer
    long countByBuyerIdAndStatus(Long buyerId, Transaction.TransactionStatus status);
    
    // Get transaction count for a user as seller
    long countBySellerIdAndStatus(Long sellerId, Transaction.TransactionStatus status);
    
    // Find transactions by status
    List<Transaction> findByStatus(Transaction.TransactionStatus status);
    
    // Find transactions by buyer and status
    List<Transaction> findByBuyerIdAndStatus(Long buyerId, Transaction.TransactionStatus status);
    
    // Find transactions by seller and status
    List<Transaction> findBySellerIdAndStatus(Long sellerId, Transaction.TransactionStatus status);
    
    // Find transactions within a date range
    @Query("SELECT t FROM Transaction t WHERE t.saleDate BETWEEN :startDate AND :endDate")
    List<Transaction> findBySaleDateBetween(@Param("startDate") java.time.LocalDateTime startDate, 
                                           @Param("endDate") java.time.LocalDateTime endDate);

    // Get transaction count for a user as seller (all statuses)
    long countBySellerId(Long sellerId);
    // Get transaction count for a user as buyer (all statuses)
    long countByBuyerId(Long buyerId);
} 
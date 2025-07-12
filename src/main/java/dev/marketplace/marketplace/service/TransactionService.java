package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.Transaction;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.TransactionRepository;
import dev.marketplace.marketplace.repository.ListingRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    
    /**
     * Create a transaction when a listing is sold to a specific buyer
     */
    @Transactional
    public Transaction createTransaction(Long listingId, Long buyerId, BigDecimal salePrice, 
                                       String paymentMethod, String notes) {
        log.info("Creating transaction: listing={}, buyer={}, price={}", listingId, buyerId, salePrice);
        
        // Validate listing exists and is not already sold
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found with ID: " + listingId));
        
        if (listing.isSold()) {
            throw new IllegalArgumentException("Listing is already sold");
        }
        
        // Validate buyer exists and is not the seller
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found with ID: " + buyerId));
        
        if (buyer.getId().equals(listing.getUser().getId())) {
            throw new IllegalArgumentException("Buyer cannot be the same as seller");
        }
        
        // Validate sale price
        if (salePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Sale price must be greater than zero");
        }
        
        // Create transaction
        Transaction transaction = Transaction.builder()
                .listing(listing)
                .seller(listing.getUser())
                .buyer(buyer)
                .salePrice(salePrice)
                .saleDate(LocalDateTime.now())
                .status(Transaction.TransactionStatus.PENDING)
                .paymentMethod(paymentMethod)
                .notes(notes)
                .build();
        
        Transaction saved = transactionRepository.save(transaction);
        
        // Mark listing as sold
        listing.setSold(true);
        listingRepository.save(listing);
        
        log.info("Transaction created successfully: {}", saved.getId());
        
        return saved;
    }
    
    /**
     * Complete a transaction (confirm the sale)
     */
    @Transactional
    public Transaction completeTransaction(Long transactionId, Long sellerId) {
        log.info("Completing transaction: {}, by seller: {}", transactionId, sellerId);
        
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));
        
        // Verify seller owns the transaction
        if (!transaction.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("User not authorized to complete this transaction");
        }
        
        // Verify transaction is pending
        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new IllegalArgumentException("Transaction is not in pending status");
        }
        
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        Transaction saved = transactionRepository.save(transaction);
        
        log.info("Transaction completed successfully: {}", transactionId);
        
        return saved;
    }
    
    /**
     * Cancel a transaction
     */
    @Transactional
    public Transaction cancelTransaction(Long transactionId, Long sellerId, String reason) {
        log.info("Cancelling transaction: {}, by seller: {}", transactionId, sellerId);
        
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));
        
        // Verify seller owns the transaction
        if (!transaction.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("User not authorized to cancel this transaction");
        }
        
        // Verify transaction is pending
        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new IllegalArgumentException("Transaction is not in pending status");
        }
        
        transaction.setStatus(Transaction.TransactionStatus.CANCELLED);
        transaction.setNotes(transaction.getNotes() + "\nCancelled: " + reason);
        Transaction saved = transactionRepository.save(transaction);
        
        // Mark listing as not sold
        Listing listing = transaction.getListing();
        listing.setSold(false);
        listingRepository.save(listing);
        
        log.info("Transaction cancelled successfully: {}", transactionId);
        
        return saved;
    }
    
    /**
     * Check if a user has bought a specific listing
     */
    @Transactional(readOnly = true)
    public boolean hasUserBoughtListing(Long userId, Long listingId) {
        return transactionRepository.existsByListingIdAndBuyerIdAndStatus(
                listingId, userId, Transaction.TransactionStatus.COMPLETED);
    }
    
    /**
     * Get the buyer for a completed transaction on a listing
     */
    @Transactional(readOnly = true)
    public Optional<User> getBuyerForListing(Long listingId) {
        return transactionRepository.findFirstByListingIdAndStatusOrderBySaleDateDesc(
                listingId, Transaction.TransactionStatus.COMPLETED)
                .map(Transaction::getBuyer);
    }
    
    /**
     * Get all transactions for a user as buyer
     */
    @Transactional(readOnly = true)
    public List<Transaction> getUserBuyingHistory(Long userId) {
        return transactionRepository.findByBuyerId(userId);
    }
    
    /**
     * Get all transactions for a user as seller
     */
    @Transactional(readOnly = true)
    public List<Transaction> getUserSellingHistory(Long userId) {
        return transactionRepository.findBySellerId(userId);
    }
    
    /**
     * Get completed transactions for a user as buyer
     */
    @Transactional(readOnly = true)
    public List<Transaction> getUserCompletedPurchases(Long userId) {
        return transactionRepository.findByBuyerIdAndStatus(userId, Transaction.TransactionStatus.COMPLETED);
    }
    
    /**
     * Get completed transactions for a user as seller
     */
    @Transactional(readOnly = true)
    public List<Transaction> getUserCompletedSales(Long userId) {
        return transactionRepository.findBySellerIdAndStatus(userId, Transaction.TransactionStatus.COMPLETED);
    }
    
    /**
     * Get transaction by ID
     */
    @Transactional(readOnly = true)
    public Optional<Transaction> getTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId);
    }
    
    /**
     * Get all transactions for a listing
     */
    @Transactional(readOnly = true)
    public List<Transaction> getListingTransactions(Long listingId) {
        return transactionRepository.findByListingId(listingId);
    }
    
    /**
     * Get user's transaction count as buyer
     */
    @Transactional(readOnly = true)
    public long getUserPurchaseCount(Long userId) {
        return transactionRepository.countByBuyerIdAndStatus(userId, Transaction.TransactionStatus.COMPLETED);
    }
    
    /**
     * Get user's transaction count as seller
     */
    @Transactional(readOnly = true)
    public long getUserSaleCount(Long userId) {
        return transactionRepository.countBySellerIdAndStatus(userId, Transaction.TransactionStatus.COMPLETED);
    }
} 
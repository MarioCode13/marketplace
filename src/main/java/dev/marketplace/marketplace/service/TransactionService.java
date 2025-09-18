package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.Transaction;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.TransactionRepository;
import dev.marketplace.marketplace.repository.ListingRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import dev.marketplace.marketplace.dto.TransactionDTO;
import dev.marketplace.marketplace.dto.ListingDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ListingImageService imageService;
    
    /**
     * Create a transaction when a listing is sold to a specific buyer
     */
    @Transactional
    public Transaction createTransaction(UUID listingId, UUID buyerId, BigDecimal salePrice,
                                       String paymentMethod, String notes) {
        log.info("Creating transaction: listing={}, buyer={}, price={}", listingId, buyerId, salePrice);

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

        listing.setSold(true);
        listingRepository.save(listing);
        
        log.info("Transaction created successfully: {}", saved.getId());
        
        return saved;
    }
    
    /**
     * Complete a transaction (confirm the sale)
     */
    @Transactional
    public Transaction completeTransaction(UUID transactionId, UUID sellerId) {
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
    public Transaction cancelTransaction(UUID transactionId, UUID sellerId, String reason) {
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
    public boolean hasUserBoughtListing(UUID userId, UUID listingId) {
        return transactionRepository.existsByListingIdAndBuyerIdAndStatus(
                listingId, userId, Transaction.TransactionStatus.COMPLETED);
    }
    
    /**
     * Get the buyer for a completed transaction on a listing
     */
    @Transactional(readOnly = true)
    public Optional<User> getBuyerForListing(UUID listingId) {
        return transactionRepository.findFirstByListingIdAndStatusOrderBySaleDateDesc(
                listingId, Transaction.TransactionStatus.COMPLETED)
                .map(Transaction::getBuyer);
    }
    
    /**
     * Get all transactions for a user as buyer
     */
    @Transactional(readOnly = true)
    public List<Transaction> getUserBuyingHistory(UUID userId) {
        return transactionRepository.findByBuyerId(userId);
    }
    
    /**
     * Get all transactions for a user as seller
     */
    @Transactional(readOnly = true)
    public List<Transaction> getUserSellingHistory(UUID userId) {
        return transactionRepository.findBySellerId(userId);
    }
    
    /**
     * Get completed transactions for a user as buyer
     */
    @Transactional(readOnly = true)
    public List<Transaction> getUserCompletedPurchases(UUID userId) {
        return transactionRepository.findByBuyerIdAndStatus(userId, Transaction.TransactionStatus.COMPLETED);
    }
    
    /**
     * Get completed transactions for a user as seller
     */
    @Transactional(readOnly = true)
    public List<Transaction> getUserCompletedSales(UUID userId) {
        return transactionRepository.findBySellerIdAndStatus(userId, Transaction.TransactionStatus.COMPLETED);
    }
    
    /**
     * Get transaction by ID
     */
    @Transactional(readOnly = true)
    public Optional<Transaction> getTransaction(UUID transactionId) {
        return transactionRepository.findById(transactionId);
    }
    
    /**
     * Get all transactions for a listing
     */
    @Transactional(readOnly = true)
    public List<Transaction> getListingTransactions(UUID listingId) {
        return transactionRepository.findByListingId(listingId);
    }
    
    /**
     * Get user's transaction count as buyer
     */
    @Transactional(readOnly = true)
    public long getUserPurchaseCount(UUID userId) {
        return transactionRepository.countByBuyerIdAndStatus(userId, Transaction.TransactionStatus.COMPLETED);
    }
    
    /**
     * Get user's transaction count as seller
     */
    @Transactional(readOnly = true)
    public long getUserSaleCount(UUID userId) {
        return transactionRepository.countBySellerIdAndStatus(userId, Transaction.TransactionStatus.COMPLETED);
    }
    
    /**
     * Convert a transaction to DTO with proper image URLs
     */
    private TransactionDTO convertToDTO(Transaction transaction) {
        Listing listing = transaction.getListing();
        List<String> preSignedUrls = imageService.generatePreSignedUrls(listing.getImages());
        
        ListingDTO listingDTO = new ListingDTO(
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                preSignedUrls,
                listing.getCategory(),
                listing.getPrice(),
                listing.getCity(),
                listing.getCustomCity(),
                listing.getCondition().name(),
                listing.getUser(),
                listing.getBusiness(),
                listing.getCreatedAt(),
                listing.isSold(),
                listing.getExpiresAt() != null ? listing.getExpiresAt().toString() : null,
                listing.isArchived() // Pass archived field
        );
        
        return TransactionDTO.fromTransaction(transaction, listingDTO);
    }
    
    /**
     * Get all transactions for a user as buyer (with DTOs)
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> getUserBuyingHistoryDTO(UUID userId) {
        return transactionRepository.findByBuyerId(userId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }
    
    /**
     * Get all transactions for a user as seller (with DTOs)
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> getUserSellingHistoryDTO(UUID userId) {
        return transactionRepository.findBySellerId(userId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }
    
    /**
     * Get completed transactions for a user as buyer (with DTOs)
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> getUserCompletedPurchasesDTO(UUID userId) {
        return transactionRepository.findByBuyerIdAndStatus(userId, Transaction.TransactionStatus.COMPLETED)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }
    
    /**
     * Get completed transactions for a user as seller (with DTOs)
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> getUserCompletedSalesDTO(UUID userId) {
        return transactionRepository.findBySellerIdAndStatus(userId, Transaction.TransactionStatus.COMPLETED)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }
    
    /**
     * Get transaction by ID (with DTO)
     */
    @Transactional(readOnly = true)
    public Optional<TransactionDTO> getTransactionDTO(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .map(this::convertToDTO);
    }
}

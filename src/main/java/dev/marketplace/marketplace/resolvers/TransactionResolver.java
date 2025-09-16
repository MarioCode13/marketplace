package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.Transaction;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.service.TransactionService;
import dev.marketplace.marketplace.service.UserService;
import dev.marketplace.marketplace.dto.TransactionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TransactionResolver {
    
    private final TransactionService transactionService;
    private final UserService userService;
    
    /**
     * Create a transaction when a listing is sold to a specific buyer
     */
    @MutationMapping
    public Transaction createTransaction(@Argument UUID listingId,
                                       @Argument UUID buyerId,
                                       @Argument BigDecimal salePrice,
                                       @Argument String paymentMethod,
                                       @Argument String notes,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        UUID sellerId = userService.getUserIdByUsername(userDetails.getUsername());
        return transactionService.createTransaction(listingId, buyerId, salePrice, paymentMethod, notes);
    }
    
    /**
     * Complete a transaction (confirm the sale)
     */
    @MutationMapping
    public Transaction completeTransaction(@Argument UUID transactionId,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        UUID sellerId = userService.getUserIdByUsername(userDetails.getUsername());
        return transactionService.completeTransaction(transactionId, sellerId);
    }
    
    /**
     * Cancel a transaction
     */
    @MutationMapping
    public Transaction cancelTransaction(@Argument UUID transactionId,
                                       @Argument String reason,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        UUID sellerId = userService.getUserIdByUsername(userDetails.getUsername());
        return transactionService.cancelTransaction(transactionId, sellerId, reason);
    }
    
    /**
     * Get user's buying history
     */
    @QueryMapping
    public List<TransactionDTO> myPurchases(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userService.getUserIdByUsername(userDetails.getUsername());
        return transactionService.getUserBuyingHistoryDTO(userId);
    }
    
    /**
     * Get user's selling history
     */
    @QueryMapping
    public List<TransactionDTO> mySales(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userService.getUserIdByUsername(userDetails.getUsername());
        return transactionService.getUserSellingHistoryDTO(userId);
    }
    
    /**
     * Get completed purchases for a user
     */
    @QueryMapping
    public List<TransactionDTO> myCompletedPurchases(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userService.getUserIdByUsername(userDetails.getUsername());
        return transactionService.getUserCompletedPurchasesDTO(userId);
    }
    
    /**
     * Get completed sales for a user
     */
    @QueryMapping
    public List<TransactionDTO> myCompletedSales(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userService.getUserIdByUsername(userDetails.getUsername());
        return transactionService.getUserCompletedSalesDTO(userId);
    }
    
    /**
     * Get transaction by ID
     */
    @QueryMapping
    public TransactionDTO getTransaction(@Argument UUID transactionId) {
        return transactionService.getTransactionDTO(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));
    }
    
    /**
     * Get all transactions for a listing
     */
    @QueryMapping
    public List<Transaction> getListingTransactions(@Argument UUID listingId) {
        return transactionService.getListingTransactions(listingId);
    }
    
    /**
     * Check if current user has bought a specific listing
     */
    @QueryMapping
    public boolean hasBoughtListing(@Argument UUID listingId,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userService.getUserIdByUsername(userDetails.getUsername());
        return transactionService.hasUserBoughtListing(userId, listingId);
    }
    
    /**
     * Get the buyer for a completed transaction on a listing
     */
    @QueryMapping
    public String getBuyerForListing(@Argument UUID listingId) {
        return transactionService.getBuyerForListing(listingId)
                .map(user -> user.getUsername() != null ? user.getUsername() : user.getEmail())
                .orElse(null);
    }
}

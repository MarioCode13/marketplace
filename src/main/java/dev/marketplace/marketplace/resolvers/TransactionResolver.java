package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.Transaction;
import dev.marketplace.marketplace.model.Listing;
import dev.marketplace.marketplace.service.TransactionService;
import dev.marketplace.marketplace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class TransactionResolver {
    
    private final TransactionService transactionService;
    private final UserService userService;
    
    /**
     * Create a transaction when a listing is sold to a specific buyer
     */
    @MutationMapping
    public Transaction createTransaction(@Argument Long listingId,
                                       @Argument Long buyerId,
                                       @Argument BigDecimal salePrice,
                                       @Argument String paymentMethod,
                                       @Argument String notes,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        Long sellerId = userService.getUserIdByUsername(userDetails.getUsername());
        return transactionService.createTransaction(listingId, buyerId, salePrice, paymentMethod, notes);
    }
    
    /**
     * Complete a transaction (confirm the sale)
     */
    @MutationMapping
    public Transaction completeTransaction(@Argument Long transactionId,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        Long sellerId = userService.getUserIdByUsername(userDetails.getUsername());
        return transactionService.completeTransaction(transactionId, sellerId);
    }
    
    /**
     * Cancel a transaction
     */
    @MutationMapping
    public Transaction cancelTransaction(@Argument Long transactionId,
                                       @Argument String reason,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        Long sellerId = userService.getUserIdByUsername(userDetails.getUsername());
        return transactionService.cancelTransaction(transactionId, sellerId, reason);
    }
    
    /**
     * Get user's buying history
     */
    @QueryMapping
    public List<Transaction> myPurchases(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return transactionService.getUserBuyingHistory(userId);
    }
    
    /**
     * Get user's selling history
     */
    @QueryMapping
    public List<Transaction> mySales(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return transactionService.getUserSellingHistory(userId);
    }
    
    /**
     * Get completed purchases for a user
     */
    @QueryMapping
    public List<Transaction> myCompletedPurchases(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return transactionService.getUserCompletedPurchases(userId);
    }
    
    /**
     * Get completed sales for a user
     */
    @QueryMapping
    public List<Transaction> myCompletedSales(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return transactionService.getUserCompletedSales(userId);
    }
    
    /**
     * Get transaction by ID
     */
    @QueryMapping
    public Transaction getTransaction(@Argument Long transactionId) {
        return transactionService.getTransaction(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));
    }
    
    /**
     * Get all transactions for a listing
     */
    @QueryMapping
    public List<Transaction> getListingTransactions(@Argument Long listingId) {
        return transactionService.getListingTransactions(listingId);
    }
    
    /**
     * Check if current user has bought a specific listing
     */
    @QueryMapping
    public boolean hasBoughtListing(@Argument Long listingId,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return transactionService.hasUserBoughtListing(userId, listingId);
    }
    
    /**
     * Get the buyer for a completed transaction on a listing
     */
    @QueryMapping
    public String getBuyerForListing(@Argument Long listingId) {
        return transactionService.getBuyerForListing(listingId)
                .map(user -> user.getUsername() != null ? user.getUsername() : user.getEmail())
                .orElse(null);
    }
} 
package dev.marketplace.marketplace.dto;

import dev.marketplace.marketplace.model.Transaction;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.model.Transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TransactionDTO(
        Long id,
        ListingDTO listing,
        User seller,
        User buyer,
        BigDecimal salePrice,
        LocalDateTime saleDate,
        TransactionStatus status,
        String paymentMethod,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TransactionDTO fromTransaction(Transaction transaction, ListingDTO listingDTO) {
        return new TransactionDTO(
                transaction.getId(),
                listingDTO,
                transaction.getSeller(),
                transaction.getBuyer(),
                transaction.getSalePrice(),
                transaction.getSaleDate(),
                transaction.getStatus(),
                transaction.getPaymentMethod(),
                transaction.getNotes(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
} 
package dev.marketplace.marketplace.dto;

import dev.marketplace.marketplace.model.Transaction;
import dev.marketplace.marketplace.dto.UserDTO;
import dev.marketplace.marketplace.model.Transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TransactionDTO(
        UUID id,
        ListingDTO listing,
        UserDTO seller,
        UserDTO buyer,
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
                dev.marketplace.marketplace.mapper.UserMapper.toDto(transaction.getSeller()),
                dev.marketplace.marketplace.mapper.UserMapper.toDto(transaction.getBuyer()),
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

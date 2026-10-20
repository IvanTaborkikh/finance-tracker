package com.padadak.finance_tracker.transaction.dto;

import com.padadak.finance_tracker.category.CategoryType;
import com.padadak.finance_tracker.transaction.CurrencyCode;
import com.padadak.finance_tracker.transaction.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        BigDecimal amount,
        CurrencyCode currency,
        CategoryType type,
        Long categoryId,
        String categoryName,
        String description,
        LocalDate transactionDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAmount(),
                t.getCurrency(),
                t.getType(),
                t.getCategory().getId(),
                t.getCategory().getName(),   // без @EntityGraph тут був би окремий SELECT на кожну транзакцію
                t.getDescription(),
                t.getTransactionDate(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}

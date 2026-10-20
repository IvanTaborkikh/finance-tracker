package com.padadak.finance_tracker.transaction.dto;

import com.padadak.finance_tracker.category.CategoryType;
import com.padadak.finance_tracker.transaction.CurrencyCode;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
        CurrencyCode currency,                 // необов'язкова: за замовчуванням PLN
        @NotNull CategoryType type,
        @NotNull Long categoryId,
        @Size(max = 255) String description,
        @NotNull LocalDate transactionDate     // JSON: "2026-10-15"
) {
    // Compact constructor: виконується при кожному створенні record
    public TransactionRequest {
        if (currency == null) {
            currency = CurrencyCode.PLN;
        }
    }
}

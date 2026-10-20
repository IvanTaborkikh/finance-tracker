package com.padadak.finance_tracker.transaction.dto;

import com.padadak.finance_tracker.category.CategoryType;

import java.time.LocalDate;

// Усі поля необов'язкові: null означає "не фільтрувати"
public record TransactionFilter(LocalDate from, LocalDate to, Long categoryId, CategoryType type) {
}

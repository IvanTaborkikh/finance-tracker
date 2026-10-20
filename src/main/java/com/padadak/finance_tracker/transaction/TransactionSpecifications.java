package com.padadak.finance_tracker.transaction;

import com.padadak.finance_tracker.category.CategoryType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

// Шматки умови WHERE, які можна комбінувати. null-фільтр = "без умови".
final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    // WHERE user_id = ?   — завжди, це ізоляція даних
    static Specification<Transaction> belongsTo(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    // WHERE transaction_date >= ?
    static Specification<Transaction> dateFrom(LocalDate from) {
        if (from == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDate>get("transactionDate"), from);
    }

    // WHERE transaction_date <= ?
    static Specification<Transaction> dateTo(LocalDate to) {
        if (to == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<LocalDate>get("transactionDate"), to);
    }

    static Specification<Transaction> hasCategory(Long categoryId) {
        if (categoryId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    static Specification<Transaction> hasType(CategoryType type) {
        if (type == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }
}

package com.padadak.finance_tracker.transaction;

import com.padadak.finance_tracker.category.Category;
import com.padadak.finance_tracker.category.CategoryType;
import com.padadak.finance_tracker.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // optional = false: у транзакції ЗАВЖДИ є власник (на відміну від категорії)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // BigDecimal: точне десяткове число. precision/scale відповідають NUMERIC(19, 2)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryType type;

    @Column(length = 255)
    private String description;

    // LocalDate: календарний день без часу і без часової зони, відповідає DATE
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Transaction() {
    }

    public Transaction(User user, Category category, BigDecimal amount, CurrencyCode currency,
                       CategoryType type, String description, LocalDate transactionDate) {
        this.user = user;
        this.category = category;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.description = description;
        this.transactionDate = transactionDate;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(Category category, BigDecimal amount, CurrencyCode currency,
                       CategoryType type, String description, LocalDate transactionDate) {
        this.category = category;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.description = description;
        this.transactionDate = transactionDate;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Category getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public CurrencyCode getCurrency() { return currency; }
    public CategoryType getType() { return type; }
    public String getDescription() { return description; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

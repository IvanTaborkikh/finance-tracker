package com.padadak.finance_tracker.category;

import com.padadak.finance_tracker.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Багато категорій → один юзер. В БД це колонка user_id з FK.
    // LAZY: юзер не завантажується з БД, поки до нього явно не звернешся.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;   // null = дефолтна категорія для всіх

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)   // зберігати як текст 'INCOME', а не як число 0/1
    @Column(nullable = false, length = 20)
    private CategoryType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Category() {
    }

    public Category(User user, String name, CategoryType type) {
        this.user = user;
        this.name = name;
        this.type = type;
        this.createdAt = Instant.now();
    }

    public void update(String name, CategoryType type) {
        this.name = name;
        this.type = type;
    }

    public boolean isDefault() {
        return user == null;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public CategoryType getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
}

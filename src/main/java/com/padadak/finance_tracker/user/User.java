package com.padadak.finance_tracker.user;

import jakarta.persistence.*;

import java.time.Instant;

@Entity                       // цей клас — таблиця в БД
@Table(name = "users")        // яка саме таблиця
public class User {

    @Id                                                   // первинний ключ
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // id генерує БД (наш GENERATED ALWAYS AS IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;   // Instant — точка в часі в UTC, відповідає TIMESTAMPTZ

    protected User() {
        // Порожній конструктор потрібен Hibernate: він спершу створює об'єкт, потім заповнює поля.
        // protected — щоб у звичайному коді ніхто не створив "порожнього" юзера.
    }

    public User(String email, String displayName) {
        this.email = email;
        this.displayName = displayName;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public Instant getCreatedAt() { return createdAt; }
}

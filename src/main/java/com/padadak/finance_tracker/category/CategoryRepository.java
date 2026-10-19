package com.padadak.finance_tracker.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // JPQL: схожий на SQL, але оперує класами і полями (Category, c.user), а не таблицями.
    // Повертає дефолтні категорії + категорії цього юзера.
    @Query("""
            select c from Category c
            where c.user is null or c.user.id = :userId
            order by c.type, c.name
            """)
    List<Category> findAllVisibleTo(@Param("userId") Long userId);

    // Derived query: Spring генерує SQL з НАЗВИ методу:
    // WHERE id = ? AND user_id = ?
    // Optional — результату може не бути, і компілятор змушує це обробити.
    Optional<Category> findByIdAndUserId(Long id, Long userId);

    // WHERE user_id = ? AND name = ? AND type = ?  → true/false
    boolean existsByUserIdAndNameAndType(Long userId, String name, CategoryType type);
}

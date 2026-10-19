package com.padadak.finance_tracker.user;

import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository<Тип entity, тип id> — і ти безкоштовно отримуєш save, findById, existsById, deleteById, findAll...
// Реалізацію цього інтерфейсу Spring генерує сам під час старту.
public interface UserRepository extends JpaRepository<User, Long> {
}
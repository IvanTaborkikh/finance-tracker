package com.padadak.finance_tracker.category.dto;

import com.padadak.finance_tracker.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// record — незмінний клас-контейнер даних. Java сама генерує конструктор, гетери name() і type(), equals, hashCode, toString.
public record CategoryRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull CategoryType type
) {
}

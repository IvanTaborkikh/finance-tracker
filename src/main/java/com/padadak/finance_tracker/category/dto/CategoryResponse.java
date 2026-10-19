package com.padadak.finance_tracker.category.dto;

import com.padadak.finance_tracker.category.Category;
import com.padadak.finance_tracker.category.CategoryType;

public record CategoryResponse(Long id, String name, CategoryType type, boolean isDefault) {

    // Маппер: Entity → DTO. Static factory method.
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.isDefault()
        );
    }
}
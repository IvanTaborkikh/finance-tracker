package com.padadak.finance_tracker.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// Власна обгортка для сторінки результатів.
// Spring Data не гарантує стабільний JSON для Page, тому формат відповіді контролюємо самі.
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}

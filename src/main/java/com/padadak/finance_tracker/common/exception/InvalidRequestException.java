package com.padadak.finance_tracker.common.exception;

// 400: запит технічно валідний, але порушує бізнес-правило (чужа категорія, from > to тощо)
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}

package com.padadak.finance_tracker.common.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

// Ловить винятки з УСІХ контролерів і перетворює їх на HTTP-відповіді.
// Базовий клас уже вміє обробляти стандартні помилки Spring MVC (невалідний JSON, не той метод і т.д.).
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    ProblemDetail handleUnauthorized(UnauthorizedException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    // Страховка: якщо два запити одночасно пройшли перевірку exists і обидва вставляють,
    // другий впаде на UNIQUE в БД. Віддаємо 409, а не 500.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", "Request conflicts with existing data");
    }

    // Помилки @Valid: повертаємо, яке поле і чому невалідне.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Validation failed", "Request contains invalid fields");
        body.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
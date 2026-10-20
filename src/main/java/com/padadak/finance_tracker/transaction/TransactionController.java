package com.padadak.finance_tracker.transaction;

import com.padadak.finance_tracker.category.CategoryType;
import com.padadak.finance_tracker.common.currentuser.CurrentUserProvider;
import com.padadak.finance_tracker.common.dto.PageResponse;
import com.padadak.finance_tracker.transaction.dto.TransactionFilter;
import com.padadak.finance_tracker.transaction.dto.TransactionRequest;
import com.padadak.finance_tracker.transaction.dto.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final CurrentUserProvider currentUser;

    public TransactionController(TransactionService transactionService, CurrentUserProvider currentUser) {
        this.transactionService = transactionService;
        this.currentUser = currentUser;
    }

    // GET /api/v1/transactions?from=2026-10-01&to=2026-10-31&type=EXPENSE&page=0&size=20&sort=amount,desc
    @GetMapping
    public PageResponse<TransactionResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) CategoryType type,
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {

        TransactionFilter filter = new TransactionFilter(from, to, categoryId, type);
        return transactionService.list(currentUser.currentUserId(), filter, pageable);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable Long id) {
        return transactionService.get(currentUser.currentUserId(), id);
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse created = transactionService.create(currentUser.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        return transactionService.update(currentUser.currentUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        transactionService.delete(currentUser.currentUserId(), id);
    }
}

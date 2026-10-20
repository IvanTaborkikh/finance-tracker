package com.padadak.finance_tracker.transaction;

import com.padadak.finance_tracker.category.Category;
import com.padadak.finance_tracker.category.CategoryRepository;
import com.padadak.finance_tracker.category.CategoryType;
import com.padadak.finance_tracker.common.dto.PageResponse;
import com.padadak.finance_tracker.common.exception.InvalidRequestException;
import com.padadak.finance_tracker.common.exception.NotFoundException;
import com.padadak.finance_tracker.transaction.dto.TransactionFilter;
import com.padadak.finance_tracker.transaction.dto.TransactionRequest;
import com.padadak.finance_tracker.transaction.dto.TransactionResponse;
import com.padadak.finance_tracker.user.User;
import com.padadak.finance_tracker.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.padadak.finance_tracker.transaction.TransactionSpecifications.belongsTo;
import static com.padadak.finance_tracker.transaction.TransactionSpecifications.dateFrom;
import static com.padadak.finance_tracker.transaction.TransactionSpecifications.dateTo;
import static com.padadak.finance_tracker.transaction.TransactionSpecifications.hasCategory;
import static com.padadak.finance_tracker.transaction.TransactionSpecifications.hasType;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public PageResponse<TransactionResponse> list(Long userId, TransactionFilter filter, Pageable pageable) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new InvalidRequestException("'from' must not be after 'to'");
        }

        // allOf = усі умови через AND; unrestricted() серед них просто ігнорується
        Specification<Transaction> spec = Specification.allOf(
                belongsTo(userId),
                dateFrom(filter.from()),
                dateTo(filter.to()),
                hasCategory(filter.categoryId()),
                hasType(filter.type())
        );

        return PageResponse.from(
                transactionRepository.findAll(spec, pageable).map(TransactionResponse::from)
        );
    }

    public TransactionResponse get(Long userId, Long transactionId) {
        return TransactionResponse.from(findOwned(userId, transactionId));
    }

    @Transactional
    public TransactionResponse create(Long userId, TransactionRequest request) {
        Category category = resolveCategory(userId, request.categoryId(), request.type());
        User owner = userRepository.getReferenceById(userId);

        Transaction saved = transactionRepository.save(new Transaction(
                owner, category, request.amount(), request.currency(),
                request.type(), request.description(), request.transactionDate()
        ));
        return TransactionResponse.from(saved);
    }

    @Transactional
    public TransactionResponse update(Long userId, Long transactionId, TransactionRequest request) {
        // Спочатку перевіряємо власника транзакції, і лише потім категорію:
        // про чужу транзакцію не повинно "витікати" навіть те, чи валідна категорія в запиті
        Transaction transaction = findOwned(userId, transactionId);
        Category category = resolveCategory(userId, request.categoryId(), request.type());

        transaction.update(category, request.amount(), request.currency(),
                request.type(), request.description(), request.transactionDate());
        return TransactionResponse.from(transaction);   // save() не потрібен: dirty checking
    }

    @Transactional
    public void delete(Long userId, Long transactionId) {
        transactionRepository.delete(findOwned(userId, transactionId));
    }

    private Transaction findOwned(Long userId, Long transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
    }

    // Категорія має бути доступна юзеру (своя або дефолтна) і того ж типу, що й транзакція
    private Category resolveCategory(Long userId, Long categoryId, CategoryType type) {
        Category category = categoryRepository.findVisibleById(categoryId, userId)
                .orElseThrow(() -> new InvalidRequestException("Category not found"));

        if (category.getType() != type) {
            throw new InvalidRequestException("Category type does not match transaction type");
        }
        return category;
    }
}

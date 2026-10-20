package com.padadak.finance_tracker.transaction;

import com.padadak.finance_tracker.category.Category;
import com.padadak.finance_tracker.category.CategoryRepository;
import com.padadak.finance_tracker.category.CategoryType;
import com.padadak.finance_tracker.common.exception.InvalidRequestException;
import com.padadak.finance_tracker.common.exception.NotFoundException;
import com.padadak.finance_tracker.transaction.dto.TransactionFilter;
import com.padadak.finance_tracker.transaction.dto.TransactionRequest;
import com.padadak.finance_tracker.transaction.dto.TransactionResponse;
import com.padadak.finance_tracker.user.User;
import com.padadak.finance_tracker.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long CATEGORY_ID = 3L;
    private static final Long TRANSACTION_ID = 100L;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    TransactionService service;

    @Test
    void create_whenCategoryNotVisibleToUser_throwsInvalidRequestAndDoesNotSave() {
        when(categoryRepository.findVisibleById(CATEGORY_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(USER_ID, expense("45.50")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Category not found");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void create_whenCategoryTypeDiffers_throwsInvalidRequestAndDoesNotSave() {
        Category salary = new Category(null, "Salary", CategoryType.INCOME);
        when(categoryRepository.findVisibleById(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(salary));

        assertThatThrownBy(() -> service.create(USER_ID, expense("45.50")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Category type does not match transaction type");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void create_whenValid_savesTransactionWithDefaultCurrency() {
        Category food = new Category(null, "Food", CategoryType.EXPENSE);
        when(categoryRepository.findVisibleById(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(food));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(new User("alice@example.com", "Alice"));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(call -> call.getArgument(0));

        TransactionResponse response = service.create(USER_ID, expense("45.50"));

        // isEqualByComparingTo, а не isEqualTo: для BigDecimal equals() враховує scale, тобто 45.5 != 45.50
        assertThat(response.amount()).isEqualByComparingTo("45.50");
        assertThat(response.currency()).isEqualTo(CurrencyCode.PLN);   // валюту не передали -> PLN
        assertThat(response.type()).isEqualTo(CategoryType.EXPENSE);
        assertThat(response.categoryName()).isEqualTo("Food");
    }

    @Test
    void list_whenFromIsAfterTo_throwsInvalidRequestWithoutQueryingDatabase() {
        TransactionFilter filter = new TransactionFilter(
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1), null, null);

        assertThatThrownBy(() -> service.list(USER_ID, filter, PageRequest.of(0, 20)))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(transactionRepository);   // до БД справа не дійшла
    }

    @Test
    void update_whenTransactionNotOwned_throwsNotFoundAndDoesNotCheckCategory() {
        when(transactionRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(USER_ID, TRANSACTION_ID, expense("10.00")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Transaction not found");

        verifyNoInteractions(categoryRepository);
    }

    @Test
    void update_whenOwned_changesFieldsWithoutSave() {
        Category food = new Category(null, "Food", CategoryType.EXPENSE);
        Transaction transaction = new Transaction(new User("alice@example.com", "Alice"), food,
                new BigDecimal("45.50"), CurrencyCode.PLN, CategoryType.EXPENSE, "Lunch", LocalDate.of(2026, 10, 15));
        when(transactionRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID)).thenReturn(Optional.of(transaction));
        when(categoryRepository.findVisibleById(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(food));

        TransactionResponse response = service.update(USER_ID, TRANSACTION_ID, expense("50.00"));

        assertThat(response.amount()).isEqualByComparingTo("50.00");
        assertThat(transaction.getAmount()).isEqualByComparingTo("50.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void delete_whenTransactionNotOwned_throwsNotFoundAndDeletesNothing() {
        when(transactionRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER_ID, TRANSACTION_ID))
                .isInstanceOf(NotFoundException.class);

        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    private static TransactionRequest expense(String amount) {
        return new TransactionRequest(new BigDecimal(amount), null, CategoryType.EXPENSE,
                CATEGORY_ID, "Lunch", LocalDate.of(2026, 10, 15));
    }
}

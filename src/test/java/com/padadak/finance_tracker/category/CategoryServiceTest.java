package com.padadak.finance_tracker.category;

import com.padadak.finance_tracker.category.dto.CategoryRequest;
import com.padadak.finance_tracker.category.dto.CategoryResponse;
import com.padadak.finance_tracker.common.exception.ConflictException;
import com.padadak.finance_tracker.common.exception.NotFoundException;
import com.padadak.finance_tracker.user.User;
import com.padadak.finance_tracker.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Unit-тест: ні Spring, ні БД. Репозиторії підмінені mock-об'єктами, перевіряється лише логіка сервісу.
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long CATEGORY_ID = 10L;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    CategoryService service;   // Mockito сам передасть mock-и в конструктор

    @Test
    void create_whenCategoryAlreadyExists_throwsConflictAndDoesNotSave() {
        when(categoryRepository.existsByUserIdAndNameAndType(USER_ID, "Coffee", CategoryType.EXPENSE))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(USER_ID, new CategoryRequest("Coffee", CategoryType.EXPENSE)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Category already exists");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_whenNew_savesCategoryOwnedByUser() {
        when(categoryRepository.existsByUserIdAndNameAndType(USER_ID, "Coffee", CategoryType.EXPENSE))
                .thenReturn(false);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(new User("alice@example.com", "Alice"));
        // save повертає той самий об'єкт, який отримав
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        CategoryResponse response = service.create(USER_ID, new CategoryRequest("Coffee", CategoryType.EXPENSE));

        assertThat(response.name()).isEqualTo("Coffee");
        assertThat(response.type()).isEqualTo(CategoryType.EXPENSE);
        assertThat(response.isDefault()).isFalse();   // у категорії є власник, тож вона не дефолтна
    }

    @Test
    void update_whenCategoryNotOwnedByUser_throwsNotFound() {
        // чужа, дефолтна або неіснуюча категорія: репозиторій нічого не знаходить
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(USER_ID, CATEGORY_ID, new CategoryRequest("Tea", CategoryType.EXPENSE)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Category not found");

        // до перевірки на дублікат справа навіть не доходить
        verify(categoryRepository, never()).existsByUserIdAndNameAndType(any(), any(), any());
    }

    @Test
    void update_whenNewNameAlreadyTaken_throwsConflictAndKeepsOldName() {
        Category coffee = ownedCategory("Coffee");
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(coffee));
        when(categoryRepository.existsByUserIdAndNameAndType(USER_ID, "Tea", CategoryType.EXPENSE)).thenReturn(true);

        assertThatThrownBy(() -> service.update(USER_ID, CATEGORY_ID, new CategoryRequest("Tea", CategoryType.EXPENSE)))
                .isInstanceOf(ConflictException.class);

        assertThat(coffee.getName()).isEqualTo("Coffee");   // об'єкт не змінився
    }

    @Test
    void update_whenNameIsFree_updatesCategory() {
        Category coffee = ownedCategory("Coffee");
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(coffee));
        when(categoryRepository.existsByUserIdAndNameAndType(USER_ID, "Tea", CategoryType.EXPENSE)).thenReturn(false);

        CategoryResponse response = service.update(USER_ID, CATEGORY_ID, new CategoryRequest("Tea", CategoryType.EXPENSE));

        assertThat(response.name()).isEqualTo("Tea");
        assertThat(coffee.getName()).isEqualTo("Tea");      // змінено саме entity -> dirty checking зробить UPDATE
        verify(categoryRepository, never()).save(any());    // save() справді не потрібен
    }

    @Test
    void update_whenNothingChanges_doesNotCheckForConflict() {
        Category coffee = ownedCategory("Coffee");
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(coffee));

        CategoryResponse response = service.update(USER_ID, CATEGORY_ID, new CategoryRequest("Coffee", CategoryType.EXPENSE));

        assertThat(response.name()).isEqualTo("Coffee");
        // без умови `changed` у сервісі тут був би виклик existsBy... і хибний 409
        verify(categoryRepository, never()).existsByUserIdAndNameAndType(any(), any(), any());
    }

    @Test
    void delete_whenCategoryNotOwnedByUser_throwsNotFoundAndDeletesNothing() {
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER_ID, CATEGORY_ID))
                .isInstanceOf(NotFoundException.class);

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_whenOwned_deletesThatCategory() {
        Category coffee = ownedCategory("Coffee");
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(coffee));

        service.delete(USER_ID, CATEGORY_ID);

        verify(categoryRepository).delete(coffee);   // видалено саме цей об'єкт, рівно один раз
    }

    private static Category ownedCategory(String name) {
        return new Category(new User("alice@example.com", "Alice"), name, CategoryType.EXPENSE);
    }
}

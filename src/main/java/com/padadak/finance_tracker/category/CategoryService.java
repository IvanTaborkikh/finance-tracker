package com.padadak.finance_tracker.category;

import com.padadak.finance_tracker.category.dto.CategoryRequest;
import com.padadak.finance_tracker.category.dto.CategoryResponse;
import com.padadak.finance_tracker.common.exception.ConflictException;
import com.padadak.finance_tracker.common.exception.NotFoundException;
import com.padadak.finance_tracker.user.User;
import com.padadak.finance_tracker.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)   // за замовчуванням усі методи — читання
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public List<CategoryResponse> list(Long userId) {
        // TODO 1: візьми categoryRepository.findAllVisibleTo(userId)
        //         і перетвори кожну Category на CategoryResponse.
        //         Підказка: .stream().map(CategoryResponse::from).toList()

        return categoryRepository.findAllVisibleTo(userId).stream().map(CategoryResponse::from).toList();
    }

    @Transactional   // цей метод пише в БД
    public CategoryResponse create(Long userId, CategoryRequest request) {
        if (categoryRepository.existsByUserIdAndNameAndType(userId, request.name(), request.type())) {
            throw new ConflictException("Category already exists");
        }
        // getReferenceById не робить SELECT: повертає "посилання" на юзера, нам потрібен лише його id для FK
        User owner = userRepository.getReferenceById(userId);
        Category saved = categoryRepository.save(new Category(owner, request.name(), request.type()));
        return CategoryResponse.from(saved);
    }

    @Transactional
    public CategoryResponse update(Long userId, Long categoryId, CategoryRequest request) {
        Category category = findOwned(userId, categoryId);

        boolean changed = !category.getName().equals(request.name()) || category.getType() != request.type();
        if (changed && categoryRepository.existsByUserIdAndNameAndType(userId, request.name(), request.type())) {
            throw new ConflictException("Category already exists");
        }

        // save() не потрібен: category завантажена в цій транзакції (managed),
        // Hibernate сам помітить зміну полів (dirty checking) і зробить UPDATE під час commit
        category.update(request.name(), request.type());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long userId, Long categoryId) {
        categoryRepository.delete(findOwned(userId, categoryId));
        // flush: виконати DELETE зараз, а не під час commit. Якщо категорію використовують транзакції,
        // FK у БД кине помилку тут, всередині сервісу, і GlobalExceptionHandler поверне 409.
        categoryRepository.flush();
    }

    // Шукає категорію ТІЛЬКИ серед категорій цього юзера.
    // Чужа або дефолтна категорія (user_id IS NULL) не знайдеться, тому відповідь 404 в обох випадках.
    private Category findOwned(Long userId, Long categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }
}

package com.padadak.finance_tracker.category;

import com.padadak.finance_tracker.category.dto.CategoryRequest;
import com.padadak.finance_tracker.category.dto.CategoryResponse;
import com.padadak.finance_tracker.common.currentuser.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final CurrentUserProvider currentUser;   // залежимо від ІНТЕРФЕЙСУ, а не від реалізації

    public CategoryController(CategoryService categoryService, CurrentUserProvider currentUser) {
        this.categoryService = categoryService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.list(currentUser.currentUserId());
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        // @RequestBody — JSON з тіла запиту → CategoryRequest
        // @Valid — перевірити анотації @NotBlank, @Size... До сервісу невалідні дані не дійдуть.
        CategoryResponse created = categoryService.create(currentUser.currentUserId(), request);
        URI location = URI.create("/api/v1/categories/" + created.id());
        return ResponseEntity.created(location).body(created);   // 201 Created + заголовок Location
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(currentUser.currentUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)   // 204: успішно, тіла відповіді немає
    public void delete(@PathVariable Long id) {
        categoryService.delete(currentUser.currentUserId(), id);
    }
}

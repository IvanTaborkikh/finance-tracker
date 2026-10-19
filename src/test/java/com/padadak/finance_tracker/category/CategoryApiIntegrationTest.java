package com.padadak.finance_tracker.category;

import com.padadak.finance_tracker.AbstractIntegrationTest;
import com.padadak.finance_tracker.user.User;
import com.padadak.finance_tracker.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Інтеграційний тест: справжні HTTP-запити (через MockMvc) -> контролер -> сервіс -> справжній PostgreSQL.
class CategoryApiIntegrationTest extends AbstractIntegrationTest {

    private static final String USER_HEADER = "X-User-Id";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Test
    void list_forNewUser_returnsSevenDefaultCategories() throws Exception {
        Long alice = createUser("alice@example.com");

        mockMvc.perform(get("/api/v1/categories").header(USER_HEADER, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].isDefault").value(true));
    }

    @Test
    void create_returns201_andOtherUserDoesNotSeeIt() throws Exception {
        Long alice = createUser("alice@example.com");
        Long bob = createUser("bob@example.com");

        mockMvc.perform(post("/api/v1/categories")
                        .header(USER_HEADER, alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Coffee", "type": "EXPENSE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Coffee"))
                .andExpect(jsonPath("$.isDefault").value(false));

        // Alice бачить 7 дефолтних + свою
        mockMvc.perform(get("/api/v1/categories").header(USER_HEADER, alice))
                .andExpect(jsonPath("$.length()").value(8));

        // Bob бачить тільки 7 дефолтних
        mockMvc.perform(get("/api/v1/categories").header(USER_HEADER, bob))
                .andExpect(jsonPath("$.length()").value(7));
    }

    @Test
    void request_withoutUserHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Missing X-User-Id header"));
    }

    @Test
    void request_withUnknownUser_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/categories").header(USER_HEADER, 999_999))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Unknown user"));
    }

    @Test
    void create_withInvalidBody_returns400WithFieldErrors() throws Exception {
        Long alice = createUser("alice@example.com");

        mockMvc.perform(post("/api/v1/categories")
                        .header(USER_HEADER, alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "type": null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.type").exists());
    }

    @Test
    void create_duplicate_returns409() throws Exception {
        Long alice = createUser("alice@example.com");
        createCategory(alice, "Coffee");

        mockMvc.perform(post("/api/v1/categories")
                        .header(USER_HEADER, alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Coffee")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Category already exists"));
    }

    @Test
    void update_otherUsersCategory_returns404AndDoesNotChangeIt() throws Exception {
        Long alice = createUser("alice@example.com");
        Long bob = createUser("bob@example.com");
        Long aliceCategoryId = createCategory(alice, "Coffee");

        // Bob підставляє чужий id
        mockMvc.perform(put("/api/v1/categories/{id}", aliceCategoryId)
                        .header(USER_HEADER, bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Hacked")))
                .andExpect(status().isNotFound());

        assertThat(categoryRepository.findById(aliceCategoryId).orElseThrow().getName()).isEqualTo("Coffee");
    }

    @Test
    void update_defaultCategory_returns404() throws Exception {
        Long alice = createUser("alice@example.com");
        Long defaultCategoryId = categoryRepository.findAllVisibleTo(alice).get(0).getId();

        mockMvc.perform(put("/api/v1/categories/{id}", defaultCategoryId)
                        .header(USER_HEADER, alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Hacked")))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_ownCategory_returns200WithNewName() throws Exception {
        Long alice = createUser("alice@example.com");
        Long id = createCategory(alice, "Coffee");

        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .header(USER_HEADER, alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Cafe")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Cafe"));
    }

    @Test
    void delete_ownCategory_returns204_andItDisappearsFromList() throws Exception {
        Long alice = createUser("alice@example.com");
        Long id = createCategory(alice, "Coffee");

        mockMvc.perform(delete("/api/v1/categories/{id}", id).header(USER_HEADER, alice))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/categories").header(USER_HEADER, alice))
                .andExpect(jsonPath("$.length()").value(7));
    }

    // ===== Допоміжні методи =====

    private Long createUser(String email) {
        return userRepository.save(new User(email, "Test User")).getId();
    }

    // Створює категорію через API і повертає її id із заголовка Location ("/api/v1/categories/15" -> 15)
    private Long createCategory(Long userId, String name) throws Exception {
        String location = mockMvc.perform(post("/api/v1/categories")
                        .header(USER_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson(name)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }

    private static String categoryJson(String name) {
        return """
                {"name": "%s", "type": "EXPENSE"}
                """.formatted(name);
    }
}

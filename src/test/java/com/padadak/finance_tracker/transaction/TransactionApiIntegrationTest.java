package com.padadak.finance_tracker.transaction;

import com.padadak.finance_tracker.AbstractIntegrationTest;
import com.padadak.finance_tracker.category.CategoryRepository;
import com.padadak.finance_tracker.user.User;
import com.padadak.finance_tracker.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionApiIntegrationTest extends AbstractIntegrationTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String URL = "/api/v1/transactions";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    EntityManager entityManager;

    private Long alice;
    private Long bob;
    private Long food;     // дефолтна EXPENSE
    private Long salary;   // дефолтна INCOME

    // Виконується перед КОЖНИМ тестом, всередині тієї ж транзакції, що відкотиться
    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice@example.com", "Alice")).getId();
        bob = userRepository.save(new User("bob@example.com", "Bob")).getId();
        food = defaultCategoryId("Food");
        salary = defaultCategoryId("Salary");
    }

    // ===== Створення і валідація =====

    @Test
    void create_withoutCurrency_defaultsToPlnAndReturnsCategoryName() throws Exception {
        mockMvc.perform(post(URL).header(USER_HEADER, alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("45.50", "EXPENSE", food, "2026-10-15")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(45.5))
                .andExpect(jsonPath("$.currency").value("PLN"))
                .andExpect(jsonPath("$.categoryName").value("Food"))
                .andExpect(jsonPath("$.transactionDate").value("2026-10-15"));
    }

    // Один тест, три набори даних: 0, від'ємне і три знаки після коми
    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "10.555"})
    void create_withInvalidAmount_returns400(String amount) throws Exception {
        mockMvc.perform(post(URL).header(USER_HEADER, alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(amount, "EXPENSE", food, "2026-10-15")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void create_withCategoryOfDifferentType_returns400() throws Exception {
        mockMvc.perform(post(URL).header(USER_HEADER, alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("45.50", "EXPENSE", salary, "2026-10-15")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Category type does not match transaction type"));
    }

    @Test
    void create_withOtherUsersCategory_returns400() throws Exception {
        Long bobsCategory = idFromLocation(mockMvc.perform(post("/api/v1/categories").header(USER_HEADER, bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Bob only", "type": "EXPENSE"}
                                """))
                .andExpect(status().isCreated())
                .andReturn());

        // Alice підставляє id категорії Bob
        mockMvc.perform(post(URL).header(USER_HEADER, alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("45.50", "EXPENSE", bobsCategory, "2026-10-15")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Category not found"));
    }

    // ===== Ізоляція даних =====

    @Test
    void get_otherUsersTransaction_returns404() throws Exception {
        Long id = createTransaction(alice, "45.50", "EXPENSE", food, "2026-10-15");

        mockMvc.perform(get(URL + "/{id}", id).header(USER_HEADER, bob))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_returnsOnlyOwnTransactions_newestFirst() throws Exception {
        createTransaction(alice, "8000.00", "INCOME", salary, "2026-10-01");
        createTransaction(alice, "45.50", "EXPENSE", food, "2026-10-15");
        createTransaction(bob, "10.00", "EXPENSE", food, "2026-10-10");

        mockMvc.perform(get(URL).header(USER_HEADER, alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].transactionDate").value("2026-10-15"))
                .andExpect(jsonPath("$.content[1].transactionDate").value("2026-10-01"));

        mockMvc.perform(get(URL).header(USER_HEADER, bob))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ===== Фільтри, пагінація, сортування =====

    @Test
    void list_filtersByTypeCategoryAndInclusiveDateRange() throws Exception {
        createTransaction(alice, "8000.00", "INCOME", salary, "2026-10-01");
        createTransaction(alice, "45.50", "EXPENSE", food, "2026-10-15");
        createTransaction(alice, "20.00", "EXPENSE", food, "2026-09-30");

        mockMvc.perform(get(URL).header(USER_HEADER, alice).param("type", "EXPENSE"))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get(URL).header(USER_HEADER, alice).param("categoryId", salary.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));

        // Межі включні: і 01.10, і 15.10 потрапляють у діапазон, 30.09 ні
        mockMvc.perform(get(URL).header(USER_HEADER, alice)
                        .param("from", "2026-10-01").param("to", "2026-10-15"))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get(URL).header(USER_HEADER, alice)
                        .param("from", "2026-10-01").param("to", "2026-10-15").param("type", "EXPENSE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_paginates() throws Exception {
        createTransaction(alice, "1.00", "EXPENSE", food, "2026-10-01");
        createTransaction(alice, "2.00", "EXPENSE", food, "2026-10-02");
        createTransaction(alice, "3.00", "EXPENSE", food, "2026-10-03");

        mockMvc.perform(get(URL).header(USER_HEADER, alice).param("size", "2"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get(URL).header(USER_HEADER, alice).param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void list_sortsByAmountWhenRequested() throws Exception {
        createTransaction(alice, "300.00", "EXPENSE", food, "2026-10-01");
        createTransaction(alice, "5.00", "EXPENSE", food, "2026-10-02");

        mockMvc.perform(get(URL).header(USER_HEADER, alice).param("sort", "amount,asc"))
                .andExpect(jsonPath("$.content[0].amount").value(5.0))
                .andExpect(jsonPath("$.content[1].amount").value(300.0));
    }

    @Test
    void list_withUnknownSortProperty_returns400() throws Exception {
        mockMvc.perform(get(URL).header(USER_HEADER, alice).param("sort", "hacker"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown sort property: hacker"));
    }

    @Test
    void list_withFromAfterTo_returns400() throws Exception {
        mockMvc.perform(get(URL).header(USER_HEADER, alice)
                        .param("from", "2026-10-31").param("to", "2026-10-01"))
                .andExpect(status().isBadRequest());
    }

    // ===== Оновлення і видалення =====

    @Test
    void update_ownTransaction_returns200WithNewAmount() throws Exception {
        Long id = createTransaction(alice, "45.50", "EXPENSE", food, "2026-10-15");

        mockMvc.perform(put(URL + "/{id}", id).header(USER_HEADER, alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("50.00", "EXPENSE", food, "2026-10-15")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(50.0));
    }

    @Test
    void update_otherUsersTransaction_returns404() throws Exception {
        Long id = createTransaction(alice, "45.50", "EXPENSE", food, "2026-10-15");

        mockMvc.perform(put(URL + "/{id}", id).header(USER_HEADER, bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("1.00", "EXPENSE", food, "2026-10-15")))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_ownTransaction_returns204AndThenItIsGone() throws Exception {
        Long id = createTransaction(alice, "45.50", "EXPENSE", food, "2026-10-15");

        mockMvc.perform(delete(URL + "/{id}", id).header(USER_HEADER, alice))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(URL + "/{id}", id).header(USER_HEADER, alice))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCategory_usedByTransaction_returns409() throws Exception {
        Long coffee = idFromLocation(mockMvc.perform(post("/api/v1/categories").header(USER_HEADER, alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Coffee", "type": "EXPENSE"}
                                """))
                .andExpect(status().isCreated())
                .andReturn());
        createTransaction(alice, "12.00", "EXPENSE", coffee, "2026-10-16");

        // Імітуємо окремі HTTP-запити: записуємо все в БД і очищаємо кеш Hibernate,
        // щоб видалення категорії йшло "з нуля", як у справжньому застосунку
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/v1/categories/{id}", coffee).header(USER_HEADER, alice))
                .andExpect(status().isConflict());
    }

    // ===== Допоміжні методи =====

    private Long createTransaction(Long userId, String amount, String type, Long categoryId, String date) throws Exception {
        return idFromLocation(mockMvc.perform(post(URL).header(USER_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(amount, type, categoryId, date)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    // "/api/v1/transactions/15" -> 15
    private static Long idFromLocation(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }

    private Long defaultCategoryId(String name) {
        return categoryRepository.findAllVisibleTo(alice).stream()
                .filter(c -> c.isDefault() && c.getName().equals(name))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    // amount без лапок: у JSON це число, а не рядок
    private static String json(String amount, String type, Long categoryId, String date) {
        return """
                {"amount": %s, "type": "%s", "categoryId": %d, "description": "test", "transactionDate": "%s"}
                """.formatted(amount, type, categoryId, date);
    }
}

package com.padadak.finance_tracker;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

// Спільна основа для інтеграційних тестів: повний застосунок + справжня БД + MockMvc.
// DB_USERNAME / DB_PASSWORD: application.properties очікує ці змінні (локально їх дає .env, у CI його немає).
// Реальні дані для підключення все одно беруться з контейнера (@ServiceConnection).
@SpringBootTest(properties = {"DB_USERNAME=test", "DB_PASSWORD=test"})
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional   // кожен тест у транзакції, яка відкочується в кінці: тести не псують дані одне одному
public abstract class AbstractIntegrationTest {
}

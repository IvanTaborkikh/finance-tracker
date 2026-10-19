package com.padadak.finance_tracker;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

// Піднімає справжній PostgreSQL у Docker на час тестів.
// @ServiceConnection: Spring Boot сам бере url/логін/пароль цього контейнера
// і підставляє їх у DataSource та Flyway. Свій application.properties тестам для БД не потрібен.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18");   // та сама версія, що в compose.yaml
    }
}

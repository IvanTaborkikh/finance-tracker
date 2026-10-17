package com.padadak.finance_tracker.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController                      // клас обробляє HTTP-запити, результат методів серіалізується в JSON
@RequestMapping("/api/v1/ping")      // базовий шлях для всіх методів класу
public class PingController {

    @GetMapping                      // GET /api/v1/ping
    public Map<String, String> ping() {
        return Map.of("status", "ok");   // Jackson перетворить Map на {"status":"ok"}
    }
}

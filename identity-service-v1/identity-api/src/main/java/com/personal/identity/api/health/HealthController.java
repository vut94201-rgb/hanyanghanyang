package com.personal.identity.api.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoint xác minh app đã khởi động được. KHÔNG dùng cho production health check
 * thật (sau này sẽ thêm Spring Boot Actuator nếu cần kiểm tra DB / Redis).
 *
 * <p>Mục đích duy nhất: sau khi chạy app + Flyway apply migration + Spring scan
 * xong, gọi {@code GET /api/health} thấy 200 OK là biết "khung xương" đã chạy.
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Endpoint kiểm tra app sống")
public class HealthController {

    @GetMapping
    @Operation(summary = "Trạng thái cơ bản của service")
    public Map<String, Object> health() {
        return Map.of(
                "status", "OK",
                "service", "identity-service",
                "timestamp", Instant.now().toString()
        );
    }
}

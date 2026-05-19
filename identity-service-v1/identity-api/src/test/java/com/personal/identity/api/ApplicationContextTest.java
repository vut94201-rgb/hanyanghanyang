package com.personal.identity.api;




import com.personal.identity.api.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test - verify infrastructure setup OK trước khi test logic.
 *
 * <p>Nếu test này fail, MỌI test khác đều sẽ fail. Tách riêng giúp khi CI báo lỗi
 * có thể phân biệt được "infrastructure broken" vs "business logic broken".
 *
 * <h3>Những gì cần verify</h3>
 * <ol>
 *   <li>Spring context boot được (= datasource + Flyway + Redis connect OK).</li>
 *   <li>Schema đã migrate (= seed data admin có sẵn).</li>
 *   <li>Redis ping được.</li>
 * </ol>
 */
class ApplicationContextTest extends IntegrationTestBase {

    @Autowired
    ApplicationContext applicationContext;

    @Test
    @DisplayName("Spring context khởi động thành công")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getBeanDefinitionCount()).isPositive();
    }

    @Test
    @DisplayName("Flyway migrate xong - admin user seed có sẵn (id=1)")
    void adminUserSeeded() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = 'admin'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Roles seed: ADMIN, USER, MODERATOR đều có")
    void rolesSeeded() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM roles WHERE role_code IN ('ADMIN', 'USER', 'MODERATOR')",
                Integer.class);
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Redis ping OK")
    void redisPing() {
        String pong = redisConnectionFactory.getConnection().ping();
        assertThat(pong).isEqualTo("PONG");
    }
}
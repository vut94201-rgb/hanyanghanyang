package com.personal.identity.api.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton Redis 7 container - dùng chung cho mọi integration test.
 *
 * <h3>Vì sao Redis nhẹ hơn Oracle</h3>
 * Image {@code redis:7-alpine} ~30MB, khởi động <1s. Vẫn singleton để pattern đồng
 * nhất với Oracle. Cleanup bằng {@code FLUSHALL} ở {@code @BeforeEach}, không restart
 * container.
 *
 * <h3>Vì sao dùng GenericContainer thay vì RedisContainer module</h3>
 * Testcontainers chưa có module Redis chính thức trong package chuẩn. Dùng
 * {@code GenericContainer} với port 6379 là đủ - Redis không cần config phức tạp.
 */
public final class RedisTestContainer {

    private static final GenericContainer<?> CONTAINER;

    static {
        CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withReuse(true);
        CONTAINER.start();
    }

    private RedisTestContainer() {
        // static-only
    }

    public static GenericContainer<?> getInstance() {
        return CONTAINER;
    }

    public static String getHost() {
        return CONTAINER.getHost();
    }

    public static Integer getPort() {
        return CONTAINER.getMappedPort(6379);
    }
}
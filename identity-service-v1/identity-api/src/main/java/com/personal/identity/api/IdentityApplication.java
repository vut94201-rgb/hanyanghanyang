package com.personal.identity.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Điểm vào của Identity Service.
 *
 * <h3>Vì sao cần khai báo {@code scanBasePackages}, {@code EntityScan}, {@code EnableJpaRepositories}?</h3>
 *
 * <p>Mặc định {@code @SpringBootApplication} chỉ scan package CHỨA class này
 * và các sub-package - tức {@code com.personal.identity.api.*}.
 * Nhưng dự án multi-module có:
 * <ul>
 *   <li>{@code com.personal.identity.core} - service, port interfaces</li>
 *   <li>{@code com.personal.identity.infrastructure} - JPA entity, adapter, Redis config</li>
 *   <li>{@code com.personal.identity.api} - controller, security config</li>
 * </ul>
 *
 * <p>Nếu không khai báo, Spring sẽ KHÔNG thấy:
 * <ul>
 *   <li>Service trong core - {@code NoSuchBeanDefinitionException} khi controller @Autowired.</li>
 *   <li>JPA entity trong infrastructure - {@code IllegalArgumentException: Not a managed type}.</li>
 *   <li>Spring Data repository - không sinh implementation, app crash khi start.</li>
 * </ul>
 *
 * <p>Đây là chỗ rất nhiều fresher mắc lỗi khi lần đầu làm multi-module.
 *
 * <h3>Multiple datasource? Async?</h3>
 *
 * <p>Hiện tại 1 datasource Oracle nên không cần config thêm. Nếu sau này thêm
 * datasource thứ 2 sẽ phải khai báo riêng {@code @EnableJpaRepositories(entityManagerFactoryRef=...)}.
 */
@SpringBootApplication(
        // Scan tất cả 3 module - Spring sẽ tìm @Service, @Component, @Configuration, ...
        scanBasePackages = "com.personal.identity"
)
@EntityScan(
        // Trỏ tới package chứa @Entity classes (tất cả nằm ở infrastructure)
        basePackages = "com.personal.identity.infrastructure.persistence.entity"
)
@EnableJpaRepositories(
        // Trỏ tới package chứa Spring Data repository interfaces
        basePackages = "com.personal.identity.infrastructure.persistence"
)
public class IdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}

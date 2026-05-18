package com.personal.identity.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Điểm vào của Identity Service.
 *
 * <h3>Multi-module scanning</h3>
 * Mặc định {@code @SpringBootApplication} chỉ scan package CHỨA class này
 * và các sub-package - tức {@code com.personal.identity.api.*}. Dự án multi-module
 * có code rải ở 3 module nên phải khai báo thêm:
 *
 * <ul>
 *   <li>{@code scanBasePackages = "com.personal.identity"}: scan toàn bộ 3 module
 *       để Spring tìm thấy {@code @Service}/{@code @Component}/{@code @Configuration}.</li>
 *   <li>{@code @EntityScan}: trỏ tới package chứa {@code @Entity} - ở
 *       {@code persistence.entity} (và sub-package {@code persistence.entity.base}).</li>
 *   <li>{@code @EnableJpaRepositories}: trỏ tới package chứa Spring Data repository
 *       INTERFACES - ở {@code persistence.jpa}.</li>
 * </ul>
 *
 * <p><b>Lưu ý:</b> {@code @EnableJpaRepositories} basePackages phải trỏ chính xác
 * vào sub-package {@code .jpa} chứa repository interfaces. Nếu trỏ rộng hơn
 * (vd: {@code persistence}), Spring vẫn tìm được nhưng scan thừa nhiều class
 * không phải repository.
 */
@SpringBootApplication(
        scanBasePackages = "com.personal.identity"
)
@EntityScan(
        basePackages = "com.personal.identity.infrastructure.persistence.entity"
)
@EnableJpaRepositories(
        basePackages = "com.personal.identity.infrastructure.persistence.jpa"
)
public class IdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}

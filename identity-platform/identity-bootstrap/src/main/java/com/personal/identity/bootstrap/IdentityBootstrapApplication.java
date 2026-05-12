package com.personal.identity.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.personal")
@EntityScan(basePackages = "com.personal")
@EnableJpaRepositories(basePackages = "com.personal")
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class IdentityBootstrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityBootstrapApplication.class, args);
    }

}

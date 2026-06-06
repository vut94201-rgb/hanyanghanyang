package com.personal.identity.bootstrap;


import com.personal.identity.infrastructure.ratelimit.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.personal.identity")
@EntityScan(basePackages = "com.personal.identity.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "com.personal.identity.infrastructure.persistence.adapter")
@ConfigurationPropertiesScan(basePackageClasses = {DataRedisProperties.class, RateLimitProperties.class})
@EnableConfigurationProperties
public class IdentityBootstrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityBootstrapApplication.class, args);
    }

}

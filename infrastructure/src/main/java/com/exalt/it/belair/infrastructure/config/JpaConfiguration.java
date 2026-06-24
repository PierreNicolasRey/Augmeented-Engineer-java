package com.exalt.it.belair.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration for JPA and Hibernate.
 * Enables Spring Data JPA repository support and Hibernate as the ORM.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.exalt.it.belair.infrastructure.persistence")
public class JpaConfiguration {
    // JPA and Hibernate configuration is managed via application.yml
    // See infrastructure/src/main/resources/application.yml
}

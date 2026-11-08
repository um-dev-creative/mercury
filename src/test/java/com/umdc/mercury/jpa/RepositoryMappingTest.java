package com.umdc.mercury.jpa;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Builds every JPA repository against the real entities, as Spring does at startup: a derived query such as {@code findByApplication_Id...}
 * or a JPQL string that names a property an entity no longer has stops Mercury from starting, and no other unit test would notice. An
 * in-memory H2 database is only the connection Hibernate needs; no table is created and nothing is read.
 */
@DisplayName("repositories match the entities")
class RepositoryMappingTest {

    private static LocalContainerEntityManagerFactoryBean factory;

    @BeforeAll
    static void startHibernate() {
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(new DriverManagerDataSource("jdbc:h2:mem:mapping;DB_CLOSE_DELAY=-1", "sa", ""));
        factory.setPackagesToScan("com.umdc.mercury");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "none"));
        factory.afterPropertiesSet();
    }

    @AfterAll
    static void stopHibernate() {
        factory.destroy();
    }

    @Test
    @DisplayName("every derived query and every @Query of every repository is valid for its entity")
    void everyRepositoryCanBeBuilt() throws ClassNotFoundException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition definition) {
                return definition.getMetadata().isInterface() && definition.getMetadata().isIndependent();
            }
        };
        scanner.addIncludeFilter(new AssignableTypeFilter(org.springframework.data.jpa.repository.JpaRepository.class));
        EntityManager entityManager = SharedEntityManagerCreator.createSharedEntityManager(factory.getObject());
        var repositoryFactory = new JpaRepositoryFactory(entityManager);

        List<String> repositories = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("com.umdc.mercury")) {
            Class<?> type = Class.forName(definition.getBeanClassName());
            repositories.add(type.getSimpleName());
            try {
                repositoryFactory.getRepository(type);
            } catch (RuntimeException e) {
                problems.add(type.getSimpleName() + ": " + rootMessage(e));
            }
        }

        assertFalse(repositories.isEmpty(), "no repository found: the scan is out of date");
        assertTrue(problems.isEmpty(), "These repositories do not match their entities (Mercury would not start):\n  " + String.join("\n  ", problems));
    }

    private static String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}

package com.umdc.mercury.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A repository outside the packages named by {@code @EnableJpaRepositories} is never created, and Mercury then fails to start with "required a
 * bean of type ... that could not be found" in the first service that needs it. No unit test with mocks sees that, so this one lists every JPA
 * repository of the project and checks that its package is covered.
 */
@DisplayName("DatabaseConfig")
class DatabaseConfigTest {

    @Test
    @DisplayName("every JPA repository of the project is inside a package that @EnableJpaRepositories scans")
    void everyJpaRepositoryIsRegistered() {
        var scanned = Arrays.asList(DatabaseConfig.class.getAnnotation(EnableJpaRepositories.class).basePackages());
        var provider = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
                return definition.getMetadata().isInterface();
            }
        };
        provider.addIncludeFilter(new AssignableTypeFilter(JpaRepository.class));

        List<String> outside = provider.findCandidateComponents("com.umdc.mercury").stream()
                .map(BeanDefinition::getBeanClassName)
                .filter(name -> scanned.stream().noneMatch(name::startsWith))
                .collect(Collectors.toList());

        assertTrue(outside.isEmpty(), "JPA repositories outside the scanned packages " + scanned + ": " + outside);
    }
}

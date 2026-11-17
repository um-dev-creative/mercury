package com.umdc.mercury.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Spring bean with several constructors and none marked {@code @Autowired} cannot be created ("No default constructor found") and Mercury fails to
 * start. A unit test builds the class by hand and never sees it, so this one checks every component of the project.
 */
@DisplayName("Components with several constructors")
class ComponentConstructorsTest {

    @Test
    @DisplayName("each one marks the constructor Spring must use with @Autowired")
    void markTheConstructorSpringUses() throws ClassNotFoundException {
        var provider = new ClassPathScanningCandidateComponentProvider(true) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
                return definition.getMetadata().isConcrete() && definition.getMetadata().isIndependent();
            }
        };
        provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));

        var ambiguous = new java.util.ArrayList<String>();
        for (var definition : provider.findCandidateComponents("com.umdc.mercury")) {
            var type = Class.forName(definition.getBeanClassName());
            var constructors = type.getDeclaredConstructors();
            boolean marked = java.util.Arrays.stream(constructors).anyMatch(c -> c.isAnnotationPresent(Autowired.class));
            boolean hasDefault = java.util.Arrays.stream(constructors).anyMatch(c -> c.getParameterCount() == 0);
            if (constructors.length > 1 && !marked && !hasDefault) {
                ambiguous.add(type.getName());
            }
        }

        assertTrue(ambiguous.isEmpty(), "components with several constructors and no @Autowired: " + ambiguous.stream().collect(Collectors.joining(", ")));
    }
}

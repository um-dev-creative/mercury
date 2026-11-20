package com.umdc.mercury.monitoring.health;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Runs {@link ServiceOutageRepository#search} against a real database (H2 in PostgreSQL mode, with the table Hibernate derives from the entity):
 * the window overlap, the filters, the escaped search text, the newest-first order and the paging.
 */
@DisplayName("ServiceOutageRepository.search")
class ServiceOutageRepositorySearchTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 12, 0);
    private static final LocalDateTime FROM = NOW.minusHours(24);

    private static LocalContainerEntityManagerFactoryBean factory;
    private static ServiceOutageRepository repository;

    @BeforeAll
    static void startDatabase() {
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(new DriverManagerDataSource(
                "jdbc:h2:mem:outages;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS mercury", "sa", ""));
        factory.setPackagesToScan(ServiceOutageEntity.class.getPackageName());
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
        factory.afterPropertiesSet();
        var entityManager = SharedEntityManagerCreator.createSharedEntityManager(factory.getObject());
        repository = new JpaRepositoryFactory(entityManager).getRepository(ServiceOutageRepository.class);
        new TransactionTemplate(new JpaTransactionManager(factory.getObject())).executeWithoutResult(tx -> repository.saveAll(List.of(
                outage("SMTP", "DOWN", "TIMEOUT", "The service did not answer in time", NOW.minusHours(2), NOW.minusHours(1)),
                outage("SMTP", "DEGRADED", "SLOW", "Answers take 50% longer_than usual", NOW.minusHours(5), NOW.minusHours(4)),
                outage("KAFKA", "DOWN", "CONNECTION_REFUSED", "The broker refused the connection", NOW.minusMinutes(30), null),
                outage("MONGODB", "DOWN", "TIMEOUT", "The service did not answer in time", NOW.minusDays(3), NOW.minusDays(2)),
                outage("POSTGRESQL", "DOWN", "TIMEOUT", "Long one", NOW.minusDays(2), NOW.minusHours(23)))));
    }

    @AfterAll
    static void stopDatabase() {
        factory.destroy();
    }

    private static ServiceOutageEntity outage(String service, String status, String cause, String text, LocalDateTime start, LocalDateTime end) {
        var entity = new ServiceOutageEntity();
        entity.setId(UUID.randomUUID());
        entity.setService(service);
        entity.setStatus(status);
        entity.setCauseCode(cause);
        entity.setCauseText(text);
        entity.setStartedAt(start);
        entity.setEndedAt(end);
        return entity;
    }

    private static Page<ServiceOutageEntity> search(String service, String status, String text, int page, int size) {
        return repository.search(FROM, NOW, service, status, ServiceHealthService.likePattern(text), PageRequest.of(page, size));
    }

    private static List<String> causes(Page<ServiceOutageEntity> page) {
        return page.getContent().stream().map(ServiceOutageEntity::getCauseCode).toList();
    }

    @Test
    @DisplayName("keeps the outages that overlap the window, newest first: ongoing ones and one that began before but ended inside count")
    void window() {
        var page = search("", "", "", 0, 20);

        assertEquals(4, page.getTotalElements(), "the one that ended two days ago is out");
        assertEquals(List.of("CONNECTION_REFUSED", "TIMEOUT", "SLOW", "TIMEOUT"), causes(page));
        assertEquals("POSTGRESQL", page.getContent().get(3).getService());
    }

    @Test
    @DisplayName("pages keep the order and the total")
    void paging() {
        var first = search("", "", "", 0, 3);
        var second = search("", "", "", 1, 3);

        assertEquals(4, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
        assertEquals(3, first.getContent().size());
        assertEquals(List.of("POSTGRESQL"), second.getContent().stream().map(ServiceOutageEntity::getService).toList());
    }

    @Test
    @DisplayName("service and status narrow it")
    void filters() {
        assertEquals(List.of("TIMEOUT", "SLOW"), causes(search("SMTP", "", "", 0, 20)));
        assertEquals(List.of("SLOW"), causes(search("SMTP", "DEGRADED", "", 0, 20)));
        assertEquals(3, search("", "DOWN", "", 0, 20).getTotalElements());
    }

    @Test
    @DisplayName("the text is found in the service, the cause code or the sentence, in any case; % and _ are literal")
    void text() {
        assertEquals(List.of("CONNECTION_REFUSED"), causes(search("", "", "kaf", 0, 20)));
        assertEquals(List.of("CONNECTION_REFUSED"), causes(search("", "", "refused", 0, 20)));
        assertEquals(List.of("TIMEOUT", "TIMEOUT"), causes(search("", "", "timeout", 0, 20)));
        assertEquals(List.of("SLOW"), causes(search("", "", "50% longer_than", 0, 20)));
        assertEquals(0, search("", "", "5_%", 0, 20).getTotalElements(), "wildcards typed by the person match only themselves");
        assertEquals(0, search("", "", "did!not", 0, 20).getTotalElements());
    }
}

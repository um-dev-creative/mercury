package com.umdc.mercury.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {"com.umdc.mercury.jpa.sql.repository", "com.umdc.mercury.telegram"})
@EnableMongoRepositories(basePackages = {"com.umdc.mercury.jpa.nosql.repository"})
public class DatabaseConfig {
}

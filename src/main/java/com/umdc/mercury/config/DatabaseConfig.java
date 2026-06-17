package com.umdc.mercury.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {"com.umdc.mercury.jpa.sql.repository"})
@EnableMongoRepositories(basePackages = {"com.umdc.mercury.jpa.nosql.repository"})
public class DatabaseConfig {
}

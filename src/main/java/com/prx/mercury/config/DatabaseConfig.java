package com.prx.mercury.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EntityScan(basePackages = {"com.prx.mercury.jpa.sql.entity"})
@EnableJpaRepositories(basePackages = {"com.prx.mercury.jpa.sql.repository"})
@EnableMongoRepositories(basePackages = {"com.prx.mercury.jpa.nosql.repository"})
public class DatabaseConfig {
}

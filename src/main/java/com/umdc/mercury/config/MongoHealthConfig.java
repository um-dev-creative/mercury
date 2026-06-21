package com.umdc.mercury.config;

import org.bson.Document;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoHealthConfig {

    @Bean
    @Primary
    public MongoHealthIndicatorOverride mongoHealthIndicator(MongoTemplate mongoTemplate) {
        return new MongoHealthIndicatorOverride(mongoTemplate);
    }

    static class MongoHealthIndicatorOverride extends AbstractHealthIndicator {

        private final MongoTemplate mongoTemplate;

        MongoHealthIndicatorOverride(MongoTemplate mongoTemplate) {
            this.mongoTemplate = mongoTemplate;
        }

        @Override
        protected void doHealthCheck(Health.Builder builder) {
            try {
                Document result = mongoTemplate.getDb().runCommand(new Document("ping", 1));
                builder.up().withDetail("maxWireVersion", result.getInteger("ok", 0));
            } catch (IllegalStateException e) {
                // MongoDB cluster closed during context shutdown — not a real failure
                builder.unknown().withDetail("reason", "MongoDB cluster closed (shutdown in progress)");
            } catch (Exception e) {
                builder.down(e);
            }
        }
    }
}

package com.umdc.mercury.config;

import com.mongodb.MongoException;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoHealthConfigTest {

    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final MongoDatabase database = mock(MongoDatabase.class);

    private String status() {
        return new MongoHealthConfig().mongoHealthIndicator(mongoTemplate).health().getStatus().getCode();
    }

    @Test
    void successfulPingIsUp() {
        when(mongoTemplate.getDb()).thenReturn(database);
        when(database.runCommand(ArgumentMatchers.<Bson>any())).thenReturn(new Document("ok", 1));

        assertEquals("UP", status());
    }

    @Test
    void closedClusterDuringShutdownIsUnknown() {
        when(mongoTemplate.getDb()).thenThrow(new IllegalStateException("state should be: open"));

        assertEquals("UNKNOWN", status());
    }

    @Test
    void failedPingIsDown() {
        when(mongoTemplate.getDb()).thenThrow(new MongoException("unreachable"));

        assertEquals("DOWN", status());
    }
}

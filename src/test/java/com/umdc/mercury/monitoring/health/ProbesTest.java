package com.umdc.mercury.monitoring.health;

import com.mongodb.client.MongoDatabase;
import com.sun.net.httpserver.HttpServer;
import com.umdc.mercury.provider.TelegramMessageDispatcher;
import jakarta.mail.MessagingException;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Service probes")
class ProbesTest {

    private static final long NEVER_SLOW = 60_000;

    // ---- PostgreSQL ----

    private static PostgresProbe postgres(DataSource dataSource, long slowMs) {
        return new PostgresProbe(dataSource, 2, slowMs, 30);
    }

    @Test
    @DisplayName("PostgreSQL: a valid connection is UP and names itself")
    void postgresUp() throws SQLException {
        var connection = mock(Connection.class);
        when(connection.isValid(2)).thenReturn(true);
        var dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);

        var probe = postgres(dataSource, NEVER_SLOW);

        assertEquals("POSTGRESQL", probe.service());
        assertEquals(Duration.ofSeconds(30), probe.interval());
        assertEquals(ServiceStatus.UP, probe.check().status());
    }

    @Test
    @DisplayName("PostgreSQL: an invalid connection, a SQL error and a timeout are DOWN with their cause; a slow answer is DEGRADED")
    void postgresFailures() throws SQLException {
        var invalid = mock(Connection.class);
        when(invalid.isValid(2)).thenReturn(false);
        var dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(invalid);
        assertEquals(HealthCause.CONNECTION_FAILED, postgres(dataSource, NEVER_SLOW).check().cause());

        var broken = mock(DataSource.class);
        when(broken.getConnection()).thenThrow(new SQLException("password for host db1 refused"));
        var result = postgres(broken, NEVER_SLOW).check();
        assertEquals(ServiceStatus.DOWN, result.status());
        assertEquals(HealthCause.CONNECTION_FAILED, result.cause());

        var slowPool = mock(DataSource.class);
        when(slowPool.getConnection()).thenThrow(new SQLTimeoutException("t"));
        assertEquals(HealthCause.TIMEOUT, postgres(slowPool, NEVER_SLOW).check().cause());

        var valid = mock(Connection.class);
        when(valid.isValid(2)).thenReturn(true);
        var ok = mock(DataSource.class);
        when(ok.getConnection()).thenReturn(valid);
        var slow = postgres(ok, -1).check();
        assertEquals(ServiceStatus.DEGRADED, slow.status());
        assertEquals(HealthCause.SLOW, slow.cause());
    }

    // ---- MongoDB ----

    private static MongoTemplate mongo(RuntimeException failure) {
        var database = mock(MongoDatabase.class);
        if (failure == null) {
            when(database.runCommand(any(Document.class))).thenReturn(new Document("ok", 1));
        } else {
            when(database.runCommand(any(Document.class))).thenThrow(failure);
        }
        var template = mock(MongoTemplate.class);
        when(template.getDb()).thenReturn(database);
        return template;
    }

    @Test
    @DisplayName("MongoDB: a ping is UP; a failure is DOWN, a timeout is TIMEOUT, a slow ping is DEGRADED")
    void mongoProbe() {
        var up = new MongoProbe(mongo(null), NEVER_SLOW, 30);
        assertEquals("MONGODB", up.service());
        assertEquals(Duration.ofSeconds(30), up.interval());
        assertEquals(ServiceStatus.UP, up.check().status());

        assertEquals(HealthCause.CONNECTION_FAILED, new MongoProbe(mongo(new IllegalStateException("no server")), NEVER_SLOW, 30).check().cause());
        class MongoTimeoutException extends RuntimeException {
        }
        assertEquals(HealthCause.TIMEOUT, new MongoProbe(mongo(new MongoTimeoutException()), NEVER_SLOW, 30).check().cause());
        assertEquals(HealthCause.SLOW, new MongoProbe(mongo(null), -1, 30).check().cause());
    }

    // ---- SMTP ----

    @Test
    @DisplayName("SMTP: a connection that signs in is UP; a refusal is classified; another sender type is not configured")
    void smtpProbe() throws MessagingException {
        var sender = mock(JavaMailSenderImpl.class);
        var probe = new SmtpProbe(sender, 300);
        assertEquals("SMTP", probe.service());
        assertEquals(Duration.ofMinutes(5), probe.interval());
        assertEquals(ServiceStatus.UP, probe.check().status());

        doThrow(new MessagingException("535 Username and Password not accepted")).when(sender).testConnection();
        var refused = probe.check();
        assertEquals(ServiceStatus.DOWN, refused.status());
        assertEquals(HealthCause.AUTH_REFUSED, refused.cause());

        assertEquals(ServiceStatus.NOT_CONFIGURED, new SmtpProbe(mock(MailSender.class), 300).check().status());
    }

    // ---- Kafka ----

    private static KafkaProbe kafka(boolean enabled, boolean listeners, KafkaProbe.ClusterPing ping, long slowMs) {
        return new KafkaProbe(enabled, listeners, ping, 2, slowMs, 30);
    }

    @Test
    @DisplayName("Kafka: switched off or without listeners is NOT_CONFIGURED, never DOWN, and the broker is not asked")
    void kafkaNotConfigured() {
        KafkaProbe.ClusterPing mustNotRun = timeout -> {
            throw new AssertionError("the broker must not be asked");
        };

        var disabled = kafka(false, true, mustNotRun, NEVER_SLOW).check();
        assertEquals(ServiceStatus.NOT_CONFIGURED, disabled.status());
        assertEquals(HealthCause.NOT_CONFIGURED, disabled.cause());
        var off = kafka(true, false, mustNotRun, NEVER_SLOW).check();
        assertEquals(HealthCause.LISTENERS_OFF, off.cause());
    }

    @Test
    @DisplayName("Kafka: an answering cluster is UP; a refusal is DOWN, a timeout is TIMEOUT, a slow answer is DEGRADED")
    void kafkaChecks() {
        var up = kafka(true, true, timeout -> { }, NEVER_SLOW);
        assertEquals("KAFKA", up.service());
        assertEquals(Duration.ofSeconds(30), up.interval());
        assertEquals(ServiceStatus.UP, up.check().status());

        assertEquals(HealthCause.CONNECTION_FAILED, kafka(true, true, timeout -> {
            throw new java.util.concurrent.ExecutionException(new RuntimeException("no broker"));
        }, NEVER_SLOW).check().cause());
        assertEquals(HealthCause.TIMEOUT, kafka(true, true, timeout -> {
            throw new TimeoutException();
        }, NEVER_SLOW).check().cause());
        assertEquals(HealthCause.TIMEOUT, kafka(true, true, timeout -> {
            throw new java.util.concurrent.ExecutionException(new org.apache.kafka.common.errors.TimeoutException("t"));
        }, NEVER_SLOW).check().cause());
        assertEquals(HealthCause.SLOW, kafka(true, true, timeout -> { }, -1).check().cause());
    }

    @Test
    @DisplayName("Kafka: an interrupted check is reported as a failed check and keeps the interrupt flag")
    void kafkaInterrupted() {
        var result = kafka(true, true, timeout -> {
            throw new InterruptedException();
        }, NEVER_SLOW).check();

        assertEquals(HealthCause.CHECK_ERROR, result.cause());
        assertTrue(Thread.interrupted(), "the interrupt flag is restored");
    }

    @Test
    @DisplayName("Kafka: the Spring constructor with no KafkaAdmin available is DOWN, not an exception")
    void kafkaWithoutAdmin() {
        @SuppressWarnings("unchecked")
        org.springframework.beans.factory.ObjectProvider<org.springframework.kafka.core.KafkaAdmin> none = mock(org.springframework.beans.factory.ObjectProvider.class);

        var result = new KafkaProbe(none, true, true, 2, NEVER_SLOW, 30).check();

        assertEquals(ServiceStatus.DOWN, result.status());
    }

    // ---- Backbone (a real HTTP server) ----

    private static HttpServer server(int status, long delayMillis) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static BackboneProbe backbone(HttpServer server, long slowMs) {
        return new BackboneProbe("http://127.0.0.1:" + server.getAddress().getPort() + "/backbone/", "actuator/health", 1, slowMs, 30);
    }

    @Test
    @DisplayName("Backbone: any answer below 500 is UP (even a 404), a 5xx is DOWN, a slow one is DEGRADED")
    void backboneStatusCodes() throws Exception {
        for (int status : new int[]{200, 401, 404}) {
            var server = server(status, 0);
            try {
                var probe = backbone(server, NEVER_SLOW);
                assertEquals("BACKBONE", probe.service());
                assertEquals(Duration.ofSeconds(30), probe.interval());
                assertEquals(ServiceStatus.UP, probe.check().status(), "HTTP " + status);
            } finally {
                server.stop(0);
            }
        }
        var broken = server(503, 0);
        try {
            assertEquals(HealthCause.HTTP_ERROR, backbone(broken, NEVER_SLOW).check().cause());
            assertEquals(HealthCause.HTTP_ERROR, backbone(broken, NEVER_SLOW).check().cause());
        } finally {
            broken.stop(0);
        }
        var ok = server(200, 0);
        try {
            assertEquals(HealthCause.SLOW, backbone(ok, -1).check().cause());
        } finally {
            ok.stop(0);
        }
    }

    @Test
    @DisplayName("Backbone: no answer in time is TIMEOUT and a closed port is CONNECTION_FAILED")
    void backboneUnreachable() throws Exception {
        var slow = server(200, 3000);
        try {
            assertEquals(HealthCause.TIMEOUT, backbone(slow, NEVER_SLOW).check().cause());
        } finally {
            slow.stop(0);
        }
        var gone = server(200, 0);
        var probe = backbone(gone, NEVER_SLOW);
        gone.stop(0);
        assertEquals(HealthCause.CONNECTION_FAILED, probe.check().cause());
    }

    // ---- Telegram ----

    @Test
    @DisplayName("Telegram: no bot token is NOT_CONFIGURED; a working bot is UP; refusals are classified")
    void telegramProbe() throws TelegramApiException {
        var dispatcher = mock(TelegramMessageDispatcher.class);
        var probe = new TelegramProbe(dispatcher, NEVER_SLOW, 30);
        assertEquals("TELEGRAM", probe.service());
        assertEquals(Duration.ofSeconds(30), probe.interval());

        when(dispatcher.isEnabled()).thenReturn(false);
        assertEquals(ServiceStatus.NOT_CONFIGURED, probe.check().status());

        when(dispatcher.isEnabled()).thenReturn(true);
        assertEquals(ServiceStatus.UP, probe.check().status());

        doThrow(new TelegramApiException("Error executing GetMe: [401] Unauthorized")).when(dispatcher).checkReachable();
        assertEquals(HealthCause.AUTH_REFUSED, probe.check().cause());
        doThrow(new TelegramApiException("Read timed out")).when(dispatcher).checkReachable();
        assertEquals(HealthCause.TIMEOUT, probe.check().cause());
        doThrow(new TelegramApiException((String) null)).when(dispatcher).checkReachable();
        assertEquals(HealthCause.CONNECTION_FAILED, probe.check().cause());
    }

    @Test
    @DisplayName("Telegram: a slow answer is DEGRADED")
    void telegramSlow() {
        var dispatcher = mock(TelegramMessageDispatcher.class);
        when(dispatcher.isEnabled()).thenReturn(true);

        assertEquals(HealthCause.SLOW, new TelegramProbe(dispatcher, -1, 30).check().cause());
    }
}

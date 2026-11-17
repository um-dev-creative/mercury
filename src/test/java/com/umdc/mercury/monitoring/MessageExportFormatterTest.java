package com.umdc.mercury.monitoring;

import com.umdc.mercury.api.v1.to.MonitoringTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MessageExportFormatter")
class MessageExportFormatterTest {

    private static MonitoringTO.MessageItem item(String recipient, String cause) {
        return new MonitoringTO.MessageItem(UUID.fromString("00000000-0000-0000-0000-000000000001"), LocalDateTime.of(2026, 11, 17, 8, 30, 5), "EMAIL", "CAMPAIGN", "FAILED",
                null, null, null, recipient, cause);
    }

    private static String csv(MonitoringTO.MessageItem... items) {
        return new String(MessageExportFormatter.csv(List.of(items)), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("writes the header and one CRLF-terminated row per message, with empty cells for what is missing")
    void rows() {
        var lines = csv(item("j***@e***.com", "SEND_FAILED")).split("\r\n");

        assertEquals(MessageExportFormatter.HEADER.strip(), lines[0]);
        assertEquals("2026-11-17T08:30:05,EMAIL,CAMPAIGN,FAILED,,,,j***@e***.com,SEND_FAILED", lines[1]);
        assertEquals(2, lines.length);
    }

    @Test
    @DisplayName("no rows is just the header")
    void empty() {
        assertEquals(MessageExportFormatter.HEADER, csv());
    }

    @Test
    @DisplayName("a cell that a spreadsheet would run as a formula gets a leading apostrophe")
    void formulaInjection() {
        for (String start : List.of("=", "+", "-", "@", "\t", "\r")) {
            assertTrue(MessageExportFormatter.cell(start + "cmd").startsWith("'" + start.replace("\r", "").replace("\t", "")) || MessageExportFormatter.cell(start + "cmd").startsWith("\"'"),
                    "starts with " + start.strip());
        }
        assertEquals("'=SUM(A1)", MessageExportFormatter.cell("=SUM(A1)"));
        assertEquals("'-1", MessageExportFormatter.cell("-1"));
        assertEquals("a-b", MessageExportFormatter.cell("a-b"));
    }

    @Test
    @DisplayName("commas, quotes and line breaks are quoted RFC 4180 style")
    void quoting() {
        assertEquals("\"a,b\"", MessageExportFormatter.cell("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", MessageExportFormatter.cell("say \"hi\""));
        assertEquals("\"a\nb\"", MessageExportFormatter.cell("a\nb"));
        assertEquals("", MessageExportFormatter.cell(null));
        assertEquals("", MessageExportFormatter.cell(""));
    }
}

package com.umdc.mercury.monitoring;

import com.umdc.mercury.api.v1.to.MonitoringTO;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Writes the message table as CSV. Cells that a spreadsheet would run as a formula (they start with {@code =}, {@code +}, {@code -}, {@code @},
 * a tab or a carriage return) get a leading apostrophe, so an exported file is safe to open. The recipient is the masked one.
 */
public final class MessageExportFormatter {

    static final String HEADER = "occurredAt,channel,kind,status,messageId,campaignId,templateDefinedId,recipient,errorCause\r\n";

    private MessageExportFormatter() {
        // utility class
    }

    public static byte[] csv(List<MonitoringTO.MessageItem> items) {
        var out = new StringBuilder(HEADER);
        for (var item : items) {
            out.append(String.join(",", cell(item.occurredAt()), cell(item.channel()), cell(item.kind()), cell(item.status()), cell(item.messageId()),
                    cell(item.campaignId()), cell(item.templateDefinedId()), cell(item.recipient()), cell(item.errorCause()))).append("\r\n");
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String cell(Object value) {
        if (value == null) {
            return "";
        }
        var text = value.toString();
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0
                ? '"' + text.replace("\"", "\"\"") + '"' : text;
    }
}

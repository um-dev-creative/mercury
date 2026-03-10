package com.prx.mercury.jpa.nosql.document;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "messages")
public class TelegramMessageDocument extends MessageDocument {
    private Long chatId;
    private String message;
    private String parseMode;

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getParseMode() {
        return parseMode;
    }

    public void setParseMode(String parseMode) {
        this.parseMode = parseMode;
    }
}

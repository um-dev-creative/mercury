package com.prx.mercury.jpa.nosql.document;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "messages")
public class SmsMessageDocument extends MessageDocument {
    private String phoneNumber;
    private String message;
    private String senderId;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
}

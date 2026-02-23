package com.prx.mercury.jpa.nosql.document;

import com.prx.mercury.api.v1.to.EmailContact;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "messages")
public class EmailMessageDocument extends MessageDocument {
    private List<EmailContact> to;
    private List<EmailContact> cc;
    private String from;
    private String subject;
    private String body;

    public List<EmailContact> getTo() {
        return to;
    }

    public void setTo(List<EmailContact> to) {
        this.to = to;
    }

    public List<EmailContact> getCc() {
        return cc;
    }

    public void setCc(List<EmailContact> cc) {
        this.cc = cc;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}


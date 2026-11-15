package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.to.EmailContact;
import com.umdc.mercury.api.v1.to.TemplateDefinedTO;
import com.umdc.mercury.api.v1.to.TemplateTO;
import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.EmailMessageDocument;
import com.umdc.mercury.mail.MailRecipientPolicy;
import com.umdc.mercury.mail.MailRecipientProperties;
import freemarker.template.Configuration;
import freemarker.template.Template;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EmailServiceImpl: who really receives the e-mail")
class EmailServiceImplRecipientsTest {

    private final JavaMailSender sender = mock(JavaMailSender.class);

    private MimeMessage send(MailRecipientPolicy policy, List<EmailContact> to, List<EmailContact> cc) throws Exception {
        var freemarker = mock(Configuration.class);
        when(freemarker.getTemplate("t.ftl")).thenReturn(new Template("t", new StringReader("hi"), new Configuration(Configuration.VERSION_2_3_31)));
        var folders = mock(com.umdc.mercury.template.ApplicationTemplateFolders.class);
        when(folders.nameOf(any(), any())).thenReturn("t.ftl");
        var service = new EmailServiceImpl(freemarker, mock(com.umdc.mercury.jpa.nosql.repository.EmailMessageNSRepository.class), sender,
                mock(MailTrackingService.class), folders, policy);
        var doc = new EmailMessageDocument("1", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "from@x.test", to, cc, "s", "",
                LocalDateTime.now(), Map.of(), DeliveryStatusType.PENDING);
        var definition = new TemplateDefinedTO(null, new TemplateTO(null, null, "t.ftl", null, null, null, null, null, true), null, null, null, null, null, true, null);

        service.sendEmail(doc, definition);

        var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        return captor.getValue();
    }

    private static String addresses(MimeMessage message, Message.RecipientType type) throws Exception {
        var all = message.getRecipients(type);
        return all == null ? "" : Arrays.stream(all).map(Object::toString).sorted().collect(Collectors.joining(","));
    }

    @Test
    @DisplayName("with the request honored the message goes to its own to and cc")
    void honoredRequest() throws Exception {
        var message = send(new MailRecipientPolicy(new MailRecipientProperties(List.of("qa@x.test"), true)),
                List.of(new EmailContact("real@x.test", "R", null)), List.of(new EmailContact("copy@x.test", "C", null)));

        assertEquals("real@x.test", addresses(message, Message.RecipientType.TO));
        assertEquals("copy@x.test", addresses(message, Message.RecipientType.CC));
    }

    @Test
    @DisplayName("with the request ignored the message goes only to the default list, with no cc")
    void ignoredRequest() throws Exception {
        var message = send(new MailRecipientPolicy(new MailRecipientProperties(List.of("qa1@x.test", "qa2@x.test"), false)),
                List.of(new EmailContact("real@x.test", "R", null)), List.of(new EmailContact("copy@x.test", "C", null)));

        assertEquals("qa1@x.test,qa2@x.test", addresses(message, Message.RecipientType.TO));
        assertEquals("", addresses(message, Message.RecipientType.CC));
    }

    @Test
    @DisplayName("a message with no recipient goes to the default list")
    void emptyRequestUsesDefaults() throws Exception {
        var message = send(new MailRecipientPolicy(new MailRecipientProperties(List.of("qa@x.test"), true)), List.of(), List.of());

        assertEquals("qa@x.test", addresses(message, Message.RecipientType.TO));
    }
}

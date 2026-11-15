package com.umdc.mercury.mail;

import com.umdc.mercury.api.v1.to.EmailContact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Decides who really receives an e-mail, from the recipients of the request and {@link MailRecipientProperties}, so QA can test the whole
 * flow without writing to real people.
 *
 * <ul>
 *   <li>{@code honorRequest=true}: the request's recipients; the default list only when the message names none.</li>
 *   <li>{@code honorRequest=false}: always the default list, and no copies (cc) at all.</li>
 * </ul>
 */
@Component
public class MailRecipientPolicy {

    private static final Logger logger = LoggerFactory.getLogger(MailRecipientPolicy.class);

    private final MailRecipientProperties properties;

    public MailRecipientPolicy(MailRecipientProperties properties) {
        this.properties = properties;
        if (properties.honorRequest()) {
            if (!properties.defaults().isEmpty()) {
                logger.info("{} default e-mail recipient(s) configured; used only for messages that name none", properties.defaults().size());
            }
        } else {
            logger.warn("E-mail recipient override is ON: every e-mail goes to the {} default recipient(s), the recipients of the request are ignored",
                    properties.defaults().size());
        }
    }

    /** Same behaviour as before the setting existed (tests and callers that do not configure it). */
    public static MailRecipientPolicy passThrough() {
        return new MailRecipientPolicy(MailRecipientProperties.passThrough());
    }

    /** The addresses of the "to" field. */
    public List<String> resolveTo(List<EmailContact> requested) {
        var own = requested == null ? List.<String>of() : requested.stream().filter(Objects::nonNull).map(EmailContact::email).toList();
        if (!properties.honorRequest()) {
            logger.info("E-mail recipients replaced by the default list ({} requested, {} used)", own.size(), properties.defaults().size());
            return properties.defaults();
        }
        return own.isEmpty() ? properties.defaults() : own;
    }

    /** The addresses of the "cc" field: dropped when the request's recipients are ignored. */
    public List<String> resolveCc(List<EmailContact> requested) {
        if (!properties.honorRequest() || requested == null) {
            return List.of();
        }
        return requested.stream().filter(Objects::nonNull).map(EmailContact::email).toList();
    }
}

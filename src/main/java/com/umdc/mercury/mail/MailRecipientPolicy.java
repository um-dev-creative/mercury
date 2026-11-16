package com.umdc.mercury.mail;

import com.umdc.mercury.api.v1.to.EmailContact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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

    @Autowired
    public MailRecipientPolicy(MailRecipientProperties properties, Environment environment) {
        this(properties, environment.getActiveProfiles());
    }

    /**
     * @param activeProfiles the active Spring profiles; with the override on, none may be a production profile
     *                       ({@link MailRecipientProperties#PRODUCTION_PROFILES}); any other environment is allowed
     * @throws IllegalStateException when the override is on in production
     */
    public MailRecipientPolicy(MailRecipientProperties properties, String... activeProfiles) {
        this.properties = properties;
        if (!properties.honorRequest()) {
            requireNonProduction(activeProfiles);
        }
        if (properties.honorRequest()) {
            if (!properties.defaults().isEmpty()) {
                logger.info("{} default e-mail recipient(s) configured; used only for messages that name none", properties.defaults().size());
            }
        } else {
            logger.warn("E-mail recipient override is ON in profile(s) {}: every e-mail goes to the {} default recipient(s), the recipients of the request are ignored",
                    Arrays.toString(activeProfiles), properties.defaults().size());
        }
    }

    /** Same behaviour as before the setting existed (tests and callers that do not configure it). */
    public static MailRecipientPolicy passThrough() {
        return new MailRecipientPolicy(MailRecipientProperties.passThrough(), new String[0]);
    }

    private static void requireNonProduction(String... activeProfiles) {
        var active = Arrays.stream(activeProfiles == null ? new String[0] : activeProfiles)
                .map(p -> p.strip().toLowerCase(Locale.ROOT)).toList();
        var production = active.stream().filter(MailRecipientProperties.PRODUCTION_PROFILES::contains).findFirst();
        if (production.isPresent()) {
            throw new IllegalStateException("umdc.mail.recipients.honor-request=false (MAIL_HONOR_REQUEST_RECIPIENTS) is never allowed in production, "
                    + "but the production profile '" + production.get() + "' is active (active profiles: " + active + ")");
        }
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

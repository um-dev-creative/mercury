package com.umdc.mercury.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Test-recipient settings bound at {@code umdc.mail.recipients.*}, meant for QA and other non-production environments.
 *
 * <p>{@code defaults} is the list of addresses that receive the e-mails; {@code honorRequest} says whether the recipients named in the
 * request (form or payload) are used. With {@code honorRequest=true} (the default, today's behaviour) the request's recipients are used and
 * {@code defaults} only stands in for a message that names none; with {@code honorRequest=false} the request's recipients are ignored and
 * every e-mail goes to {@code defaults}. Turning the request's recipients off without a default list would leave nobody to send to, so that
 * combination refuses to start.</p>
 *
 * <p>The override works in every environment that is not production (qa, dev, native, remote-supabase, ...). Mercury refuses to start with
 * it on only when a production profile ({@code prd}, {@code prod}, {@code production}) is active.</p>
 *
 * @param defaults         default recipients (blank entries are dropped, every address is validated)
 * @param honorRequest     whether the recipients of the request are used
 */
@ConfigurationProperties(prefix = "umdc.mail.recipients")
public record MailRecipientProperties(List<String> defaults, @DefaultValue("true") boolean honorRequest) {

    /** Profiles that are production: the override can never run there. */
    public static final Set<String> PRODUCTION_PROFILES = Set.of("prd", "prod", "production");

    private static final Pattern ADDRESS = Pattern.compile("^[^\\s@,;<>]+@[^\\s@,;<>]+\\.[^\\s@,;<>]+$");

    @ConstructorBinding
    public MailRecipientProperties {
        defaults = defaults == null ? List.of() : defaults.stream()
                .filter(a -> a != null && !a.isBlank())
                .map(String::strip)
                .distinct()
                .toList();
        for (String address : defaults) {
            if (!ADDRESS.matcher(address).matches()) {
                throw new IllegalStateException("umdc.mail.recipients.defaults holds an invalid e-mail address: '" + address + "'");
            }
        }
        if (!honorRequest && defaults.isEmpty()) {
            throw new IllegalStateException("umdc.mail.recipients.honor-request=false needs umdc.mail.recipients.defaults "
                    + "(MAIL_DEFAULT_RECIPIENTS): with the request's recipients ignored there would be nobody to send to");
        }
    }

    /** The behaviour before this setting existed: the request's recipients, no default list. */
    public static MailRecipientProperties passThrough() {
        return new MailRecipientProperties(List.of(), true);
    }
}

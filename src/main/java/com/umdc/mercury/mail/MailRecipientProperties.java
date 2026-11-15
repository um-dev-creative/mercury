package com.umdc.mercury.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;
import java.util.Locale;
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
 * <p>The override only works in the Spring profiles of {@code overrideProfiles} (default {@code qa} and {@code dev}); in any other profile
 * Mercury refuses to start with it on, and the production profiles ({@code prd}, {@code prod}, {@code production}) can never be listed.</p>
 *
 * @param defaults         default recipients (blank entries are dropped, every address is validated)
 * @param honorRequest     whether the recipients of the request are used
 * @param overrideProfiles profiles where {@code honorRequest=false} is allowed (default {@code qa}, {@code dev})
 */
@ConfigurationProperties(prefix = "umdc.mail.recipients")
public record MailRecipientProperties(List<String> defaults, @DefaultValue("true") boolean honorRequest, List<String> overrideProfiles) {

    /** Profiles that are production: the override can never run there. */
    public static final Set<String> PRODUCTION_PROFILES = Set.of("prd", "prod", "production");

    private static final Pattern ADDRESS = Pattern.compile("^[^\\s@,;<>]+@[^\\s@,;<>]+\\.[^\\s@,;<>]+$");

    /** Default allowed profiles ({@code qa}, {@code dev}). */
    public MailRecipientProperties(List<String> defaults, boolean honorRequest) {
        this(defaults, honorRequest, null);
    }

    @ConstructorBinding
    public MailRecipientProperties {
        defaults = defaults == null ? List.of() : defaults.stream()
                .filter(a -> a != null && !a.isBlank())
                .map(String::strip)
                .distinct()
                .toList();
        overrideProfiles = overrideProfiles == null || overrideProfiles.stream().allMatch(p -> p == null || p.isBlank())
                ? List.of("qa", "dev")
                : overrideProfiles.stream().filter(p -> p != null && !p.isBlank()).map(p -> p.strip().toLowerCase(Locale.ROOT)).distinct().toList();
        for (String profile : overrideProfiles) {
            if (PRODUCTION_PROFILES.contains(profile)) {
                throw new IllegalStateException("umdc.mail.recipients.override-profiles (MAIL_RECIPIENT_OVERRIDE_PROFILES) may not include the production profile '"
                        + profile + "'");
            }
        }
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

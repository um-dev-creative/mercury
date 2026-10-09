package com.umdc.mercury.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Expected {@code iss} (issuer) and {@code aud} (audience) claims of a Mercury
 * {@code session-token}, bound at {@code umdc.security.jwt.*}.
 * <p>
 * Deliberately separate from security-oauth's {@code JwtConfigProperties}
 * (secret + expirationMs, bound at the same prefix): that class lives in a
 * closed-source jar and cannot be extended here.
 * </p>
 * <p>
 * The {@code session-token} an end user presents is issued by backbone-rest (same HS256
 * secret), not by Mercury, so Mercury accepts more than its own {@code issuer}/{@code audience}:
 * {@code trustedIssuers}/{@code trustedAudiences} list the additional values to accept (e.g.
 * backbone-rest's). {@code issuer}/{@code audience} are what Mercury stamps on tokens it mints
 * itself and are always accepted.
 * </p>
 * <p>
 * {@code allowMissingClaims} (default {@code false}: {@code iss} and {@code aud} are mandatory) is
 * an escape hatch for an environment whose backbone-rest does not stamp them. While {@code true}
 * an <em>absent</em> claim is tolerated, but a <em>present</em> claim must still match.
 * </p>
 * <p>
 * {@code validateWithBackbone}: tokens Mercury did not mint itself are also checked against
 * backbone-rest's own validation (its {@code jti} deny-list), so a logout there is honoured here.
 * Positive answers are cached for {@code backboneValidationCacheTtl}, which is therefore the
 * longest a Backbone logout can take to be reflected.
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "umdc.security.jwt")
public class SessionJwtClaimsProperties {

    private String issuer = "mercury";
    private String audience = "mercury";
    private List<String> trustedIssuers = new ArrayList<>();
    private List<String> trustedAudiences = new ArrayList<>();
    private boolean allowMissingClaims;
    private boolean validateWithBackbone = true;
    private Duration backboneValidationCacheTtl = Duration.ofSeconds(30);

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public List<String> getTrustedIssuers() {
        return trustedIssuers;
    }

    public void setTrustedIssuers(List<String> trustedIssuers) {
        this.trustedIssuers = trustedIssuers;
    }

    public List<String> getTrustedAudiences() {
        return trustedAudiences;
    }

    public void setTrustedAudiences(List<String> trustedAudiences) {
        this.trustedAudiences = trustedAudiences;
    }

    /** Every {@code iss} value to accept: Mercury's own plus {@code trustedIssuers}. */
    public Set<String> acceptedIssuers() {
        return accepted(issuer, trustedIssuers);
    }

    /** Every {@code aud} value to accept: Mercury's own plus {@code trustedAudiences}. */
    public Set<String> acceptedAudiences() {
        return accepted(audience, trustedAudiences);
    }

    private static Set<String> accepted(String own, List<String> extra) {
        Set<String> values = new HashSet<>();
        values.add(own);
        if (extra != null) {
            values.addAll(extra);
        }
        return values;
    }

    public boolean isValidateWithBackbone() {
        return validateWithBackbone;
    }

    public void setValidateWithBackbone(boolean validateWithBackbone) {
        this.validateWithBackbone = validateWithBackbone;
    }

    public Duration getBackboneValidationCacheTtl() {
        return backboneValidationCacheTtl;
    }

    public void setBackboneValidationCacheTtl(Duration backboneValidationCacheTtl) {
        this.backboneValidationCacheTtl = backboneValidationCacheTtl;
    }

    public boolean isAllowMissingClaims() {
        return allowMissingClaims;
    }

    public void setAllowMissingClaims(boolean allowMissingClaims) {
        this.allowMissingClaims = allowMissingClaims;
    }
}

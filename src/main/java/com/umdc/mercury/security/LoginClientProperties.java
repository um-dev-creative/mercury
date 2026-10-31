package com.umdc.mercury.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry of backend-service credentials allowed to call
 * {@code POST /api/v1/auth/token} — the endpoint other UMDC services (e.g.
 * directory-backend) use to obtain a Mercury session-token before calling
 * {@code /api/v1/mail} or {@code /api/v1/verification-code}.
 * <p>
 * Deliberately separate from security-oauth's {@code AuthProperties}/
 * {@code ClientProperties} (bound at {@code umdc.security.auth.clients}) — that
 * list is Mercury's own OUTBOUND client-credentials config for calling other
 * services (backbone, etc.). Reusing it here would conflate two unrelated
 * concerns: who Mercury authenticates AS, versus who is allowed to
 * authenticate TO Mercury.
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "umdc.mercury")
public class LoginClientProperties {

    private List<LoginClient> loginClients = new ArrayList<>();

    public List<LoginClient> getLoginClients() {
        return loginClients;
    }

    public void setLoginClients(List<LoginClient> loginClients) {
        this.loginClients = loginClients;
    }

    /**
     * One backend caller allowed to obtain a Mercury session-token.
     */
    public static class LoginClient {

        private String alias;
        private String password;

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}

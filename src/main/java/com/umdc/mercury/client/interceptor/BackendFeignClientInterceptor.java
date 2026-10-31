package com.umdc.mercury.client.interceptor;

import com.umdc.commons.constants.httpstatus.type.MessageType;
import com.umdc.commons.exception.StandardException;
import com.umdc.commons.general.pojo.UserSession;
import com.umdc.commons.general.to.TokenResponse;
import com.umdc.mercury.constant.MercuryKey;
import com.umdc.security.properties.AuthProperties;
import com.umdc.security.properties.ClientProperties;
import feign.RequestInterceptor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;

import static com.umdc.commons.constants.keys.ManagementAuthKey.*;
import static com.umdc.mercury.constant.MercuryMessage.RUNTIME_EXCEPTION;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.springframework.cloud.openfeign.security.OAuth2AccessTokenInterceptor.BEARER;

/**
 * Feign {@code configuration} for {@link com.umdc.mercury.client.BackboneClient} only.
 * <p>
 * Deliberately NOT {@code @Configuration} — Spring Cloud OpenFeign's own docs warn
 * against annotating a per-client {@code configuration} class that way: it would
 * then also be picked up by the main application context's component scan and its
 * {@code @Bean RequestInterceptor} would apply to every Feign client in the app —
 * including {@link com.umdc.security.client.BackbonePublicClient}, whose calls to
 * backbone's public (permitAll) endpoints must go out with NO Authorization header.
 * Feign resolves this class's {@code @Bean} methods into {@code BackboneClient}'s
 * own isolated child context regardless of this annotation, constructor-injecting
 * {@link AuthProperties} from the parent context as usual.
 * </p>
 */
public class BackendFeignClientInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackendFeignClientInterceptor.class);
    private static final String BACKBONE_ID = "backbone";

    @Value("${umdc.logging.trace.enabled}")
    private boolean isTraceEnabled;

    private final ClientProperties clientProperties;

    public BackendFeignClientInterceptor(AuthProperties authProperties) {
        if (Objects.nonNull(authProperties.getClients())) {
            this.clientProperties = authProperties.getClients().stream()
                    .filter(authProperties1 -> authProperties1.getId()
                            .equalsIgnoreCase(BACKBONE_ID)).findFirst().orElse(null);
        } else {
            this.clientProperties = null;
        }
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            String token;
            try {
                token = getToken();
            } catch (Exception e) {
                throw new StandardException(MercuryKey.APPLICATION_ID.getProjectId(), RUNTIME_EXCEPTION, e);
            }
            // Add the session-token header to the request
            template.header(AUTHORIZATION, BEARER.concat(" ").concat(token));
            if (isTraceEnabled) {
                LOGGER.info("Headers key/value :::::");
                template.headers().forEach((key, value) ->
                        LOGGER.info("KEY: {}, VALUE: {} :::::", key, value));
            }
        };
    }

    private String getToken() {
        RestTemplate client = new RestTemplate(new HttpComponentsClientHttpRequestFactory(trustedHttpClient()));
        HttpHeaders headers = new HttpHeaders();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        parameters.add(GRANT_TYPE.value, clientProperties.getAuthorizationGrantType());
        parameters.add(CLIENT_ID.value, clientProperties.getClientId());
        parameters.add(USERNAME.value, clientProperties.getUsername());
        parameters.add(PASSWORD.value, clientProperties.getPassword());
        parameters.add(CLIENT_SECRET.value, clientProperties.getClientSecret());

        var response = client.postForObject(clientProperties.getRedirectUri(), new HttpEntity<>(parameters, headers), TokenResponse.class);
        if (Objects.isNull(response)) {
            LOGGER.error("Error occurred while connect with the Manager authenticator");
            throw new StandardException("Error occurred while connect with the Manager authenticator", MessageType.DEFAULT_MESSAGE);
        }
        return create(response, UUID.randomUUID()).token();
    }

    public static UserSession create(TokenResponse tokenResponse, UUID id) {
        return new UserSession(id, "", tokenResponse.accessToken());
    }

    /**
     * Builds an Apache HttpClient5 client whose TLS strategy is wired explicitly from
     * {@link SSLContext#getDefault()} — the JVM-wide default {@code TrustStoreInitializer}
     * installs at startup (merging the PRX Internal CA in), via {@code SSLContext.setDefault(...)}.
     * <p>
     * A bare {@code new RestTemplate()} does NOT reliably pick that up: HttpClient5's own
     * "system default" SSL resolution reads {@code javax.net.ssl.trustStore} system properties
     * directly rather than {@code SSLContext.getDefault()}, and empirically (see
     * {@code TrustStoreInitializer}'s own "Real-deployment finding" javadoc) does not reach this
     * far — every call through an unconfigured client fails with
     * {@code PKIX path building failed: unable to find valid certification path to requested target}
     * against PRX-internal hosts. Reading {@link SSLContext#getDefault()} explicitly, at call
     * time (this method only ever runs well after {@code MercuryApplication.main()} has already
     * installed the merged context), sidesteps that ambiguity entirely.
     * </p>
     */
    private static CloseableHttpClient trustedHttpClient() {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getDefault();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to resolve the JVM default SSLContext", e);
        }
        TlsSocketStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext);
        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tlsStrategy)
                .build();
        return HttpClients.custom().setConnectionManager(connectionManager).build();
    }

}

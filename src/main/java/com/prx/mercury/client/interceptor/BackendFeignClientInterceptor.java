package com.prx.mercury.client.interceptor;

import com.prx.commons.general.pojo.UserSession;
import com.prx.commons.general.to.TokenResponse;
import com.prx.security.properties.AuthProperties;
import com.prx.security.properties.ClientProperties;
import feign.RequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.UUID;

import static com.prx.commons.constants.keys.ManagementAuthKey.*;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.springframework.cloud.openfeign.security.OAuth2AccessTokenInterceptor.BEARER;

@Configuration
public class BackendFeignClientInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackendFeignClientInterceptor.class);
    private static final String BACKBONE_ID = "backbone";

    @Value("${prx.logging.trace.enabled}")
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
                throw new RuntimeException(e);
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

    private String getToken() throws Exception {
        RestTemplate client = new RestTemplate();
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
            throw new Exception("Error occurred while connect with the Manager authenticator");
        }
        return create(response, UUID.randomUUID()).token();
    }

    public static UserSession create(TokenResponse tokenResponse, UUID id) {
        return new UserSession(id, "", tokenResponse.accessToken());
    }

}

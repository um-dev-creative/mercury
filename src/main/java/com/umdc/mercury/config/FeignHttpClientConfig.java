package com.umdc.mercury.config;

import feign.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;

/**
 * Supplies the default {@link Client} bean every {@code @FeignClient} in this app uses for its
 * actual HTTP transport, unless a per-client {@code configuration} class overrides it — none do.
 * <p>
 * Without this bean, Spring Cloud OpenFeign falls back to its own default transport, which — like
 * a bare {@code new RestTemplate()} (see {@code BackendFeignClientInterceptor}'s own explanation) —
 * does not reliably pick up the JVM-wide default {@link SSLContext} {@code TrustStoreInitializer}
 * installs at startup (merging the PRX Internal CA in). That left every Feign call to a
 * PRX-internal host — including {@code BackbonePublicClient.introspect(...)}, used on <em>every</em>
 * M2M-authenticated request to {@code /api/v1/campaigns/**}/{@code /api/v1/channel-types/**} via
 * {@code ManagedClientSecurityConfig} — failing with the same
 * {@code PKIX path building failed: unable to find valid certification path to requested target}
 * symptom, even after {@code BackendFeignClientInterceptor}'s own manual {@code RestTemplate} call
 * (a completely separate HTTP call, used only to fetch an M2M token) was fixed the same way.
 * <p>
 * Explicitly building Feign's plain JDK-backed {@link Client.Default} with an
 * {@code SSLSocketFactory} sourced from {@link SSLContext#getDefault()} — read at bean-creation
 * time, well after {@code MercuryApplication.main()} has already installed the merged context —
 * removes the ambiguity entirely, for every Feign client in the app at once.
 */
@Configuration
public class FeignHttpClientConfig {

    @Bean
    public Client feignClient() throws NoSuchAlgorithmException {
        return new Client.Default(SSLContext.getDefault().getSocketFactory(), null);
    }
}

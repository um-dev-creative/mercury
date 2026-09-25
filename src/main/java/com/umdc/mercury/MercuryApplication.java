package com.umdc.mercury;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.umdc.mercury.security.TrustStoreInitializer;

/**
 * MercuryApplication.
 *
 * @author Luis Antonio Mata
 * @version 1.0.0, 03-05-2022
 * @since 11
 */
@EnableFeignClients(basePackages = "com.umdc.mercury.client")
@EnableScheduling
@EnableDiscoveryClient
@SpringBootApplication(
        scanBasePackages = {
                "com.umdc.commons.services",
                "com.umdc.mercury",
                "com.umdc.security"
        }
)
public class MercuryApplication {

    public static void main(String[] args) {
        // eureka.client.tls.trust-store/key-store are sourced from the remote Config Server
        // (mercury-remote-supabase.yml et al.), and spring.config.import's imported property
        // source always gives that remote source priority over this file's own eureka.client.tls
        // values for any key both define - spring.cloud.config.override-none does NOT change
        // this, since it's bound from the remote source's own properties, never from
        // application.yml itself.
        // That remote value currently resolves to a ServletContext-relative path that can never
        // exist for a jar-deployed app, so eureka.client.tls stays effectively unusable from
        // Spring config alone; setupTLS() then skips building a custom SSLContext and Eureka's
        // RestClient transport falls back to the JVM's default one. Merging PRX Internal CA into
        // a COPY of the JDK's own cacerts - exactly what docker-entrypoint.sh used to do only for
        // Docker - makes that JVM default trust monitor.umdc-qa.tst too, for local/IDE runs and
        // Docker alike, without discarding the public CA bundle other TLS targets (MongoDB
        // Atlas, etc.) still need. javax.net.ssl.trustStore is a raw JSSE system property, never
        // a Spring property, so nothing above can shadow it.
        if (System.getProperty("javax.net.ssl.trustStore") == null) {
            TrustStoreInitializer.importLocalCertsIntoDefaultTrustStore();
        }
        SpringApplication.run(MercuryApplication.class, args);
    }
}

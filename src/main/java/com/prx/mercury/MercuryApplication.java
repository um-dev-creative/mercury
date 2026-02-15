package com.prx.mercury;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MercuryApplication.
 *
 * @author Luis Antonio Mata
 * @version 1.0.0, 03-05-2022
 * @since 11
 */
@EnableFeignClients(basePackages = "com.prx.mercury.client")
@EnableScheduling
@SpringBootApplication(
        scanBasePackages = {
                "com.prx.commons.services",
                "com.prx.mercury",
                "com.prx.security"
        }
)
public class MercuryApplication {
    public static void main(String[] args) {
        SpringApplication.run(MercuryApplication.class, args);
    }
}

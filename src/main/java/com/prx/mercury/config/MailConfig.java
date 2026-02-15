package com.prx.mercury.config.mapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * FreeMakerConfig.
 *
 * @author Luis Antonio Mata
 * @version 1.0.0, 03-05-2022
 * @since 11
 */
@Configuration
public class MailConfig {
    @Value("${spring.mail.port}")
    private Integer port;
    @Value("${spring.mail.host}")
    private String host;
    @Value("${spring.mail.username}")
    private String username;
    @Value("${spring.mail.password}")
    private String password;
    @Value("${spring.mail.protocol}")
    private String protocol;
    @Value("${spring.mail.properties.mail.timeout}")
    private int timeout;
    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private boolean starttls;
    @Value("${spring.mail.properties.mail.smtp.auth}")
    private boolean auth;

    /**
     * Create an instance from object type {@link JavaMailSender}
     * @return Object type {@link JavaMailSender}.
     */
    @Bean
    public JavaMailSender getJavaMailSender(){
        final var javaMailSenderImpl = new JavaMailSenderImpl();
        javaMailSenderImpl.setHost(host);
        javaMailSenderImpl.setPort(port);
        javaMailSenderImpl.setProtocol(protocol);
        javaMailSenderImpl.setUsername(username);
        javaMailSenderImpl.setPassword(password);
        Properties props = javaMailSenderImpl.getJavaMailProperties();
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttls);
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.timeout", timeout);
        props.put("mail.debug", true);

        return javaMailSenderImpl;
    }

    public Integer getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isStarttls() {
        return starttls;
    }

    public boolean isAuth() {
        return auth;
    }
}

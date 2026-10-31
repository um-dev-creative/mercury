package com.umdc.mercury.telegram;

import freemarker.template.Configuration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Unit tests build these classes by hand, so only a real context shows that Spring can create and bind them. */
class TelegramWiringTest {

    @ComponentScan(basePackageClasses = TelegramBotApi.class)
    static class Scan { }

    @Test
    void springCreatesEveryTelegramBeanAndBindsTheProperties() {
        new ApplicationContextRunner()
                .withUserConfiguration(Scan.class)
                .withBean(TelegramLinkRepository.class, () -> Mockito.mock(TelegramLinkRepository.class))
                .withBean(TelegramLinkTokenRepository.class, () -> Mockito.mock(TelegramLinkTokenRepository.class))
                .withBean(TelegramInboxStateRepository.class, () -> Mockito.mock(TelegramInboxStateRepository.class))
                .withBean(TelegramMessageRepository.class, () -> Mockito.mock(TelegramMessageRepository.class))
                .withBean(Configuration.class, () -> Mockito.mock(Configuration.class))
                .withPropertyValues("umdc.mercury.telegram.application=zerobound", "umdc.mercury.telegram.mode=webhook")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertNotNull(context.getBean(TelegramSender.class));
                    assertNotNull(context.getBean(TelegramUpdatePoller.class));
                    assertNotNull(context.getBean(TelegramMessageService.class));
                    assertNotNull(context.getBean(TelegramRateLimiter.class));
                    assertEquals("zerobound", context.getBean(TelegramBotProperties.class).application());
                    assertEquals("webhook", context.getBean(TelegramBotProperties.class).mode());
                });
    }
}

package com.umdc.mercury.provider;

import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.TelegramMessageDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Objects;

/**
 * Delivers a queued Telegram message through the Telegram Bot API.
 *
 * <p>This is the first real provider integration in the service: the other channels still only
 * persist their messages. It is deliberately the narrow "send one message" seam so the
 * surrounding orchestration (scheduler, status reconciliation, metrics) is shared with the
 * channels that follow.</p>
 */
@Component
public class TelegramMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(TelegramMessageDispatcher.class);

    private final TelegramClient telegramClient;

    /**
     * @param telegramClient the Bot API client, absent when no bot token is configured
     */
    public TelegramMessageDispatcher(@Autowired(required = false) TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    /** Whether a bot token is configured and dispatching can happen at all. */
    public boolean isEnabled() {
        return Objects.nonNull(telegramClient);
    }

    /**
     * Sends {@code message} through the Bot API.
     *
     * @return {@link DeliveryStatusType#SENT} when Telegram accepted the message,
     *         {@link DeliveryStatusType#FAILED} otherwise
     * @throws IllegalStateException if called while no bot token is configured
     */
    public DeliveryStatusType dispatch(TelegramMessageDocument message) {
        if (!isEnabled()) {
            throw new IllegalStateException("Telegram dispatch requested without a configured bot token");
        }
        if (Objects.isNull(message.getChatId())) {
            logger.warn("Telegram message has no chatId; marking as FAILED. id={}", message.getId());
            return DeliveryStatusType.FAILED;
        }

        SendMessage sendMessage = SendMessage.builder()
                .chatId(message.getChatId())
                .text(message.getMessage())
                .parseMode(message.getParseMode())
                .build();

        try {
            telegramClient.execute(sendMessage);
            logger.info("Telegram message sent. id={}, campaignId={}", message.getId(), message.getCampaignId());
            return DeliveryStatusType.SENT;
        } catch (TelegramApiException e) {
            logger.error("Telegram rejected the message. id={}, campaignId={}, reason={}",
                    message.getId(), message.getCampaignId(), e.getClass().getSimpleName());
            return DeliveryStatusType.FAILED;
        }
    }
}

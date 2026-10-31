package com.umdc.mercury.provider;

import com.umdc.mercury.constant.DeliveryStatusType;
import com.umdc.mercury.jpa.nosql.document.TelegramMessageDocument;
import com.umdc.mercury.provider.TelegramMessageDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelegramMessageDispatcher")
class TelegramMessageDispatcherTest {

    @Mock
    private TelegramClient telegramClient;

    private TelegramMessageDocument message(Long chatId) {
        TelegramMessageDocument document = new TelegramMessageDocument();
        document.setChatId(chatId);
        document.setMessage("hola");
        document.setParseMode("HTML");
        document.setCampaignId(UUID.randomUUID());
        return document;
    }

    @Test
    @DisplayName("is disabled when no bot token produced a client")
    void disabledWithoutClient() {
        assertThat(new TelegramMessageDispatcher(null).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("refuses to dispatch while disabled instead of silently reporting success")
    void dispatchWhileDisabledThrows() {
        TelegramMessageDispatcher dispatcher = new TelegramMessageDispatcher(null);

        assertThatThrownBy(() -> dispatcher.dispatch(message(123L)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("sends through the Bot API and reports SENT")
    void dispatchSendsAndReportsSent() throws TelegramApiException {
        TelegramMessageDispatcher dispatcher = new TelegramMessageDispatcher(telegramClient);
        TelegramMessageDocument document = message(12345L);
        when(telegramClient.execute(any(SendMessage.class))).thenReturn(null);

        DeliveryStatusType result = dispatcher.dispatch(document);

        assertThat(result).isEqualTo(DeliveryStatusType.SENT);
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(captor.capture());
        SendMessage sent = captor.getValue();
        assertThat(sent.getChatId()).isEqualTo("12345");
        assertThat(sent.getText()).isEqualTo("hola");
        assertThat(sent.getParseMode()).isEqualTo("HTML");
    }

    @Test
    @DisplayName("reports FAILED when Telegram rejects the message")
    void dispatchReportsFailedOnApiException() throws TelegramApiException {
        TelegramMessageDispatcher dispatcher = new TelegramMessageDispatcher(telegramClient);
        when(telegramClient.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("chat not found"));

        assertThat(dispatcher.dispatch(message(999L))).isEqualTo(DeliveryStatusType.FAILED);
    }

    @Test
    @DisplayName("reports FAILED without calling the API when the chatId is missing")
    void dispatchWithoutChatIdFails() {
        TelegramMessageDispatcher dispatcher = new TelegramMessageDispatcher(telegramClient);

        assertThat(dispatcher.dispatch(message(null))).isEqualTo(DeliveryStatusType.FAILED);
        verifyNoInteractions(telegramClient);
    }
}

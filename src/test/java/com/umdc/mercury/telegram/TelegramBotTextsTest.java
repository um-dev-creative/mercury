package com.umdc.mercury.telegram;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("Telegram bot texts")
class TelegramBotTextsTest {

    @Test
    @DisplayName("every text exists in Spanish and English and the language falls back to Spanish")
    void bothLanguages() {
        assertEquals("es", TelegramBotTexts.lang(null));
        assertEquals("es", TelegramBotTexts.lang("pt-BR"));
        assertEquals("en", TelegramBotTexts.lang("EN-us"));
        for (java.util.function.UnaryOperator<String> f : java.util.List.<java.util.function.UnaryOperator<String>>of(TelegramBotTexts::linked, TelegramBotTexts::invalidToken,
                TelegramBotTexts::howToLink, TelegramBotTexts::help, TelegramBotTexts::statusLinked, TelegramBotTexts::statusBlocked,
                TelegramBotTexts::statusNone, TelegramBotTexts::unlinked, TelegramBotTexts::replaced)) {
            assertNotEquals(f.apply("es"), f.apply("en"));
        }
    }
}

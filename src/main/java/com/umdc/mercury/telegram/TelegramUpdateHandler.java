package com.umdc.mercury.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Handles one Telegram update of an application (T-114): the commands of a private chat and the block
 * of the bot. Anything else is ignored. A message's text is read to find the command and nothing of it
 * is stored or logged; only the recognised command name is.
 */
@Service
public class TelegramUpdateHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramUpdateHandler.class);

    private final TelegramInbox inbox;
    private final TelegramLinkService links;
    private final TelegramBotApi api;

    public TelegramUpdateHandler(TelegramInbox inbox, TelegramLinkService links, TelegramBotApi api) {
        this.inbox = inbox;
        this.links = links;
        this.api = api;
    }

    /** Handles the update unless its id was already handled. */
    @Transactional
    public void handle(String application, Map<?, ?> update) {
        if (!(update.get("update_id") instanceof Number id) || !inbox.firstTime(application, id.longValue())) {
            return;
        }
        if (update.get("my_chat_member") instanceof Map<?, ?> member) {
            onMemberChange(application, member);
        } else if (update.get("message") instanceof Map<?, ?> message) {
            onMessage(application, message);
        }
    }

    private void onMemberChange(String application, Map<?, ?> member) {
        final Long chat = privateChatId(member);
        if (chat != null && member.get("new_chat_member") instanceof Map<?, ?> now
                && ("kicked".equals(now.get("status")) || "left".equals(now.get("status")))) {
            links.markBlocked(application, chat);
            LOG.info("Telegram chat blocked the bot");
        }
    }

    private void onMessage(String application, Map<?, ?> message) {
        final Long chat = privateChatId(message);
        if (chat == null || !(message.get("text") instanceof String text) || !text.startsWith("/")) {
            return;
        }
        final String[] parts = text.strip().split("\\s+", 2);
        final String command = parts[0].substring(1).split("@", 2)[0].toLowerCase(Locale.ROOT);
        final String argument = parts.length > 1 ? parts[1] : "";
        final String fallback = message.get("from") instanceof Map<?, ?> from && from.get("language_code") instanceof String code
                ? TelegramBotTexts.lang(code) : "es";
        switch (command) {
            case "start" -> start(application, chat, argument, fallback);
            case "ayuda", "help" -> reply(application, chat, TelegramBotTexts.help(languageOf(application, chat, fallback)));
            case "estado", "status" -> status(application, chat, fallback);
            case "desvincular", "unlink" -> unlink(application, chat, fallback);
            default -> LOG.debug("Telegram command ignored");
        }
        LOG.info("Telegram command handled: {}", command);
    }

    private void start(String application, long chat, String token, String fallback) {
        if (token.isBlank()) {
            reply(application, chat, TelegramBotTexts.howToLink(languageOf(application, chat, fallback)));
            return;
        }
        if (links.redeem(application, token, chat) instanceof TelegramLinkService.Redemption.Linked linked) {
            reply(application, chat, TelegramBotTexts.linked(linked.language()));
            if (linked.replacedChatId() != null) {
                reply(application, linked.replacedChatId(), TelegramBotTexts.replaced(linked.language()));
            }
        } else {
            reply(application, chat, TelegramBotTexts.invalidToken(fallback));
        }
    }

    private void status(String application, long chat, String fallback) {
        final var status = links.statusOfChat(application, chat);
        final String lang = status.map(TelegramLinkService.LinkStatus::language).orElse(fallback);
        final String text = status.map(s -> s.status() == TelegramLinkService.Status.ACTIVE
                        ? TelegramBotTexts.statusLinked(lang) : TelegramBotTexts.statusBlocked(lang))
                .orElseGet(() -> TelegramBotTexts.statusNone(lang));
        reply(application, chat, text);
    }

    private void unlink(String application, long chat, String fallback) {
        final String lang = languageOf(application, chat, fallback);
        if (links.unlinkChat(application, chat)) {
            reply(application, chat, TelegramBotTexts.unlinked(lang));
        } else {
            reply(application, chat, TelegramBotTexts.statusNone(lang));
        }
    }

    private String languageOf(String application, long chat, String fallback) {
        return links.statusOfChat(application, chat).map(TelegramLinkService.LinkStatus::language).orElse(fallback);
    }

    private void reply(String application, long chat, String text) {
        if (api.sendMessage(chat, text, false) instanceof BotResult.Forbidden) {
            links.markBlocked(application, chat);
        }
    }

    /** The chat id when the update's chat is a private one. */
    private static Long privateChatId(Map<?, ?> container) {
        return Optional.ofNullable(container.get("chat")).filter(Map.class::isInstance).map(c -> (Map<?, ?>) c)
                .filter(c -> "private".equals(c.get("type"))).map(c -> c.get("id")).filter(Number.class::isInstance)
                .map(id -> ((Number) id).longValue()).orElse(null);
    }
}

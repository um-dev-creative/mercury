package com.umdc.mercury.telegram;

/**
 * What the Telegram Bot API answered, reduced to what the callers decide on (T-112). Neither the token
 * (it is in the URL) nor a message text is ever part of a result.
 */
public sealed interface BotResult {

    /** {@code ok: true}; {@code result} is the parsed JSON ({@code Map}, {@code List}, {@code Boolean}). */
    record Ok(Object result) implements BotResult { }

    /** 429: wait {@code retryAfterSeconds} before the next call. */
    record RateLimited(int retryAfterSeconds) implements BotResult { }

    /** 403: the user blocked the bot or the chat is gone. */
    record Forbidden(String description) implements BotResult { }

    /** Any other 4xx (400 bad request or chat not found, 401 wrong token, 409 webhook/poll conflict): retrying does not help. */
    record Rejected(int status, String description) implements BotResult { }

    /** 5xx, a timeout or a connection error: retry later. */
    record Unavailable(String reason) implements BotResult { }
}

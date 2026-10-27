package com.umdc.mercury.telegram;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The Bot API's rates, per mercury instance (T-115): about one message per second to a chat and about 30
 * per second for the bot. A message that does not fit is not dropped: the sender leaves it for the next turn.
 */
@Component
public class TelegramRateLimiter {

    static final long CHAT_INTERVAL_MILLIS = 1000;
    static final int BOT_PER_SECOND = 30;
    private static final int PRUNE_ABOVE = 10_000;

    private final Clock clock;
    private final Map<Long, Long> lastToChat = new ConcurrentHashMap<>();
    private final Deque<Long> recent = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();

    public TelegramRateLimiter() {
        this(Clock.systemUTC());
    }

    TelegramRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** True when a message to {@code chatId} may go out now (and counts it). */
    public boolean tryAcquire(long chatId) {
        lock.lock();
        try {
            return acquire(chatId);
        } finally {
            lock.unlock();
        }
    }

    private boolean acquire(long chatId) {
        final long now = clock.millis();
        while (!recent.isEmpty() && now - recent.peekFirst() >= 1000) {
            recent.pollFirst();
        }
        final Long last = lastToChat.get(chatId);
        if (recent.size() >= BOT_PER_SECOND || last != null && now - last < CHAT_INTERVAL_MILLIS) {
            return false;
        }
        if (lastToChat.size() > PRUNE_ABOVE) {
            lastToChat.values().removeIf(t -> now - t >= CHAT_INTERVAL_MILLIS);
        }
        lastToChat.put(chatId, now);
        recent.addLast(now);
        return true;
    }
}

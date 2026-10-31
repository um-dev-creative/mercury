package com.umdc.mercury.telegram;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/** Update bookkeeping (T-114): handle each Telegram update once, and let one instance poll at a time. */
@Service
public class TelegramInbox {

    private final TelegramInboxStateRepository state;
    private final Clock clock;

    @Autowired
    public TelegramInbox(TelegramInboxStateRepository state) {
        this(state, Clock.systemUTC());
    }

    TelegramInbox(TelegramInboxStateRepository state, Clock clock) {
        this.state = state;
        this.clock = clock;
    }

    /** True the first time this update id is seen (ids only grow), false for a replay. */
    @Transactional
    public boolean firstTime(String application, long updateId) {
        state.ensure(application);
        return state.advance(application, updateId) == 1;
    }

    /** The {@code offset} for the next {@code getUpdates}: last handled + 1, or null when none was handled yet. */
    @Transactional
    public Long nextOffset(String application) {
        state.ensure(application);
        return state.findById(application).map(TelegramInboxStateEntity::getLastUpdateId)
                .filter(id -> id > 0).map(id -> id + 1).orElse(null);
    }

    /** True when {@code owner} is the one that polls now; the lease lasts {@code ttl} and is renewed by the owner. */
    @Transactional
    public boolean acquireLease(String application, String owner, Duration ttl) {
        state.ensure(application);
        final LocalDateTime now = LocalDateTime.now(clock);
        return state.acquire(application, owner, now, now.plus(ttl)) == 1;
    }
}

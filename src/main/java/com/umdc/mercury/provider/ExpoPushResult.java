package com.umdc.mercury.provider;

import java.util.List;
import java.util.Map;

/**
 * What the Expo Push API answered, reduced to what the dispatcher decides on. No recipient token and no provider message text is part of a result
 * (Expo's error messages quote the token).
 */
public sealed interface ExpoPushResult {

    /** One ticket (or receipt): {@code status} is {@code ok} or {@code error}; {@code error} is {@code details.error} (for example {@code DeviceNotRegistered}). */
    record Ticket(boolean ok, String id, String error) { }

    /** HTTP 200 on {@code send}: one ticket per message, in the order sent. */
    record Tickets(List<Ticket> tickets) implements ExpoPushResult { }

    /** HTTP 200 on {@code getReceipts}: receipts by receipt id (ids without a receipt are absent). */
    record Receipts(Map<String, Ticket> byId) implements ExpoPushResult { }

    /** A 4xx other than 401, 403 and 429: the request itself is wrong (400 malformed, {@code PUSH_TOO_MANY_EXPERIENCE_IDS}...); retrying the same request does not help. */
    record Rejected(int status, String code) implements ExpoPushResult { }

    /** 401 or 403: the access token is missing or wrong while enhanced push security is on; an operator must fix it. */
    record Unauthorized(int status) implements ExpoPushResult { }

    /** 429, 5xx, a timeout or a network error that outlasted the bounded retries. */
    record Unavailable(String reason) implements ExpoPushResult { }
}

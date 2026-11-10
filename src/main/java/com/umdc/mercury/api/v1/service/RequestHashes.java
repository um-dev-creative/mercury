package com.umdc.mercury.api.v1.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/** SHA-256 over a canonical form of a request, with maps in key order, so equal requests hash equal and a reused {@code Idempotency-Key} with another body is told apart. */
public final class RequestHashes {

    private RequestHashes() {
    }

    /** Hex SHA-256 of {@code fields} (lists and maps are walked, maps in key order). */
    public static String of(Iterable<?> fields) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical(fields).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            var sorted = new TreeMap<String, String>();
            map.forEach((k, v) -> sorted.put(String.valueOf(k), canonical(v)));
            return sorted.toString();
        }
        if (value instanceof Iterable<?> items) {
            var out = new StringBuilder("[");
            items.forEach(i -> out.append(canonical(i)).append(','));
            return out.append(']').toString();
        }
        return String.valueOf(value);
    }
}

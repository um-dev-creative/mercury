package com.umdc.mercury.api.v1.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RequestHashesTest {

    @Test
    void mapsHashInKeyOrderAndListsInOrder() {
        assertEquals(RequestHashes.of(List.of(Map.of("a", 1, "b", List.of(1, 2)))), RequestHashes.of(List.of(Map.of("b", List.of(1, 2), "a", 1))));
        assertNotEquals(RequestHashes.of(List.of(List.of(1, 2))), RequestHashes.of(List.of(List.of(2, 1))));
    }

    @Test
    void theHashIsA64CharHexSha256() {
        assertEquals(64, RequestHashes.of(List.of("x")).length());
    }
}

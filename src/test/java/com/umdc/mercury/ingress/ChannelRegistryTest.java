package com.umdc.mercury.ingress;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelRegistryTest {

    private static ChannelProvider provider(String code) {
        return new ChannelProvider() {
            @Override
            public String code() {
                return code;
            }

            @Override
            public boolean validTarget(String target) {
                return true;
            }

            @Override
            public void queue(OutboundMessage message) {
                // not needed here
            }
        };
    }

    @Test
    void aProviderIsFoundByItsCodeWhateverTheCase() {
        var sms = provider("sms");
        var registry = new ChannelRegistry(List.of(sms, provider("Email")));

        assertSame(sms, registry.find(" SMS ").orElseThrow());
        assertTrue(registry.find("email").isPresent());
        assertTrue(registry.find("slack").isEmpty());
        assertTrue(registry.find(null).isEmpty());
    }

    @Test
    void theCodesAreSortedSoAnAddedChannelAppearsWithoutAnyOtherChange() {
        var registry = new ChannelRegistry(List.of(provider("whatsapp"), provider("email"), provider("slack")));

        assertEquals(List.of("email", "slack", "whatsapp"), registry.codes());
    }

    @Test
    void twoProvidersForOneCodeStopTheStartUp() {
        var e = assertThrows(IllegalStateException.class, () -> new ChannelRegistry(List.of(provider("sms"), provider("SMS"))));

        assertTrue(e.getMessage().contains("sms"));
    }

    @Test
    void aProviderWithoutACodeStopsTheStartUp() {
        assertThrows(IllegalStateException.class, () -> new ChannelRegistry(List.of(provider(" "))));
        assertThrows(IllegalStateException.class, () -> new ChannelRegistry(List.of(provider(null))));
    }
}

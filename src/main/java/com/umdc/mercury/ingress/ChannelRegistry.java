package com.umdc.mercury.ingress;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.Optional;

/**
 * The providers mercury has, by channel code. Spring hands over every {@link ChannelProvider} bean, so adding a channel is adding a bean.
 * Two providers for one code is a mistake that must stop the start-up rather than pick one silently.
 */
@Component
public class ChannelRegistry {

    private final ConcurrentMap<String, ChannelProvider> providers = new ConcurrentSkipListMap<>();

    public ChannelRegistry(List<ChannelProvider> found) {
        for (ChannelProvider provider : found) {
            String code = normalise(provider.code());
            if (code.isEmpty()) {
                throw new IllegalStateException("A channel provider without a code: " + provider.getClass().getName());
            }
            if (providers.putIfAbsent(code, provider) != null) {
                throw new IllegalStateException("Two channel providers for the code '" + code + "'");
            }
        }
    }

    /** The provider of {@code code} (case-insensitive), if mercury has one. */
    public Optional<ChannelProvider> find(String code) {
        return Optional.ofNullable(providers.get(normalise(code)));
    }

    /** Every channel code mercury can serve, sorted. */
    public List<String> codes() {
        return List.copyOf(providers.keySet());
    }

    static String normalise(String code) {
        return code == null ? "" : code.strip().toLowerCase(Locale.ROOT);
    }
}

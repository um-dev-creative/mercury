package com.umdc.mercury.client.to;

/**
 * Request body for backbone-rest's {@code POST /api/v1/managed-clients/introspect}.
 *
 * @param token the Backbone M2M token to introspect
 */
public record TokenIntrospectionRequest(String token) {

    @Override
    public String toString() {
        return "TokenIntrospectionRequest[token=***]";
    }
}

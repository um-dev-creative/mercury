package com.umdc.mercury.provider;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A scripted stand-in for exp.host (JDK HttpServer on a loopback port, no real network): answers are queued in order (an empty queue answers
 * {@code 200 {"data":[]}}), and each request is recorded with its path, Authorization and Content-Type headers and body.
 */
final class FakeExpoServer implements AutoCloseable {

    record Call(String path, String authorization, String contentType, String body) { }

    private record Reply(int status, String body) { }

    private final HttpServer server;
    private final Deque<Reply> replies = new ArrayDeque<>();
    private final List<Call> calls = new ArrayList<>();
    private volatile long delayMillis;

    FakeExpoServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            final var request = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            synchronized (calls) {
                calls.add(new Call(ex.getRequestURI().getPath(), ex.getRequestHeaders().getFirst("Authorization"),
                        ex.getRequestHeaders().getFirst("Content-Type"), request));
            }
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            final Reply reply;
            synchronized (replies) {
                reply = replies.isEmpty() ? new Reply(200, "{\"data\":[]}") : replies.poll();
            }
            final var bytes = reply.body().getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(reply.status(), bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        server.start();
    }

    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    FakeExpoServer reply(int status, String body) {
        synchronized (replies) {
            replies.add(new Reply(status, body));
        }
        return this;
    }

    FakeExpoServer delay(long millis) {
        delayMillis = millis;
        return this;
    }

    List<Call> calls() {
        synchronized (calls) {
            return List.copyOf(calls);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}

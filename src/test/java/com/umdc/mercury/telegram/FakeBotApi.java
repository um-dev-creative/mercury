package com.umdc.mercury.telegram;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** A scripted stand-in for api.telegram.org (JDK HttpServer): answers are queued, requests are recorded. */
final class FakeBotApi implements AutoCloseable {

    record Call(String path, String body) { }

    private final HttpServer server;
    private final Deque<int[]> statuses = new ArrayDeque<>();
    private final Deque<String> bodies = new ArrayDeque<>();
    private final List<Call> calls = new ArrayList<>();
    private volatile long delayMillis;

    FakeBotApi() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            final var request = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            synchronized (calls) {
                calls.add(new Call(ex.getRequestURI().getPath(), request));
            }
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            final int status;
            final String body;
            synchronized (this) {
                status = statuses.isEmpty() ? 200 : statuses.poll()[0];
                body = bodies.isEmpty() ? "{\"ok\":true,\"result\":true}" : bodies.poll();
            }
            final var bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(status, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        server.start();
    }

    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    synchronized FakeBotApi reply(int status, String body) {
        statuses.add(new int[] {status});
        bodies.add(body);
        return this;
    }

    FakeBotApi delay(long millis) {
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

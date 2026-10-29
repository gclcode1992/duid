package com.duid.spi;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 HTTP 的 DUID 客户端实现，仅负责远程调用，不做本地编码。
 *
 * @author gclcode@126.com
 */
final class HttpDuidClient implements DuidClient {

    private final String baseUrl;
    private final URI longIdUri;
    private final URI base62IdUri;
    private final URI base36UpperIdUri;
    private final HttpClient httpClient;
    private final int readTimeoutMs;
    private final int prefetch;
    private final BlockingQueue<Long> buffer;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Thread prefetchThread;

    HttpDuidClient(String serverUrl, int connectTimeoutMs, int readTimeoutMs, int prefetch) {
        this.baseUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.longIdUri = URI.create(baseUrl + "/long-id");
        this.base62IdUri = URI.create(baseUrl + "/base62-id");
        this.base36UpperIdUri = URI.create(baseUrl + "/base36-upper-id");
        this.readTimeoutMs = readTimeoutMs;
        this.prefetch = Math.max(1, prefetch);
        this.buffer = new LinkedBlockingQueue<>(this.prefetch * 2);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        eagerWarmup();
        this.prefetchThread = new Thread(this::prefetchLoop, "duid-prefetch");
        this.prefetchThread.setDaemon(true);
        this.prefetchThread.start();
    }

    @Override
    public long nextId() {
        if (closed.get()) {
            throw new IllegalStateException("DuidClient is closed");
        }
        try {
            Long id = buffer.poll(100, TimeUnit.MILLISECONDS);
            if (id == null) {
                return fetchLong();
            }
            return id;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for ID", e);
        }
    }

    @Override
    public String nextLongIdWithPrefix(String prefix) {
        return fetchText(uriWithPrefix("/long-id", prefix));
    }

    @Override
    public String nextBase62Id() {
        return fetchText(base62IdUri);
    }

    @Override
    public String nextBase62IdWithPrefix(String prefix) {
        return fetchText(uriWithPrefix("/base62-id", prefix));
    }

    @Override
    public String nextBase36UpperId() {
        return fetchText(base36UpperIdUri);
    }

    @Override
    public String nextBase36UpperIdWithPrefix(String prefix) {
        return fetchText(uriWithPrefix("/base36-upper-id", prefix));
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            prefetchThread.interrupt();
        }
    }

    private void eagerWarmup() {
        int initialFetch = Math.min(prefetch, 4);
        for (int i = 0; i < initialFetch; i++) {
            try {
                buffer.offer(fetchLong());
            } catch (RuntimeException e) {
                break;
            }
        }
    }

    private void prefetchLoop() {
        while (!closed.get() && !Thread.currentThread().isInterrupted()) {
            try {
                while (!closed.get() && buffer.size() < prefetch) {
                    buffer.offer(fetchLong());
                }
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException ignored) {
                sleepQuietly(50);
            }
        }
    }

    private long fetchLong() {
        return Long.parseLong(fetchText(longIdUri));
    }

    private URI uriWithPrefix(String path, String prefix) {
        String normalized = prefix == null ? "" : prefix;
        return URI.create(baseUrl + path + "?prefix="
                + URLEncoder.encode(normalized, StandardCharsets.UTF_8));
    }

    private String fetchText(URI uri) {
        if (closed.get()) {
            throw new IllegalStateException("DuidClient is closed");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(Duration.ofMillis(readTimeoutMs))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("DUID server returned HTTP " + response.statusCode());
            }
            return response.body().trim();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed to fetch ID from DUID server: " + uri, e);
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

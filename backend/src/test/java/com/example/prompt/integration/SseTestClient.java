package com.example.prompt.integration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class SseTestClient {
    private final String url;
    private HttpURLConnection connection;
    private Thread readerThread;
    private final List<String> events = new CopyOnWriteArrayList<>();
    private final CompletableFuture<Void> completionFuture = new CompletableFuture<>();

    public SseTestClient(String url) {
        this.url = url;
    }

    public void connect() throws IOException {
        URL sseUrl = new URL(url);
        connection = (HttpURLConnection) sseUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        connection.connect();

        readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                    System.out.println("[SseTestClient] Read line: " + line);
                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        if (!"connected".equals(data)) {
                            events.add(data);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("[SseTestClient] Connection exception: " + e.getMessage());
            } finally {
                completionFuture.complete(null);
            }
        });
        readerThread.start();
    }

    public void disconnect() {
        if (readerThread != null) {
            readerThread.interrupt();
        }
        if (connection != null) {
            connection.disconnect();
        }
        completionFuture.complete(null);
    }

    public List<String> getEvents() {
        return new ArrayList<>(events);
    }

    public CompletableFuture<Void> getCompletionFuture() {
        return completionFuture;
    }

    public void waitForEvents(int count, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (events.size() < count && (System.currentTimeMillis() - start) < timeoutMs) {
            Thread.sleep(100);
        }
    }
}

package com.example.prompt.integration;

import com.example.prompt.PromptApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    classes = PromptApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class SseIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void fullSseFlow_ReceivesEventAfterSendingPrompt() throws Exception {
        String baseUrl = "http://localhost:" + port;

        // Step 1: Connect to SSE endpoint and start reading events
        AtomicReference<String> receivedEvent = new AtomicReference<>();
        Thread sseThread = new Thread(() -> {
            try {
                URL url = new URL(baseUrl + "/api/events");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.setRequestProperty("Cache-Control", "no-cache");
                connection.connect();

                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            String data = line.substring(5).trim();
                            if (!"connected".equals(data)) {
                                receivedEvent.set(data);
                                break;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                fail("SSE connection failed: " + e.getMessage());
            }
        });
        sseThread.start();

        // Give SSE connection a moment to establish
        Thread.sleep(500);

        // Step 2: Send a prompt via HTTP POST
        String promptUrl = baseUrl + "/api/prompt";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "{\"prompt\": \"Hello\"}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(promptUrl, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Step 3: Wait for SSE event and verify content
        sseThread.join(5000); // Wait up to 5 seconds
        assertNotNull(receivedEvent.get(), "No SSE event received");

        String eventData = receivedEvent.get();
        assertTrue(eventData.contains("\"user\": \"Hello\"") || eventData.contains("\"user\":\"Hello\""), "Event should contain user message");
        assertTrue(eventData.contains("\"ai\""), "Event should contain AI response");
        assertTrue(eventData.contains("\"time\""), "Event should contain timestamp");
    }
}

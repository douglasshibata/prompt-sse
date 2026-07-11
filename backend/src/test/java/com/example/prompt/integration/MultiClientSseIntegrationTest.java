package com.example.prompt.integration;

import com.example.prompt.PromptApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    classes = PromptApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class MultiClientSseIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void multipleClients_ReceiveBroadcastEvents() throws Exception {
        String baseUrl = "http://localhost:" + port;
        int clientCount = 3;
        List<SseTestClient> clients = new ArrayList<>();

        // Step 1: Connect multiple clients
        for (int i = 0; i < clientCount; i++) {
            SseTestClient client = new SseTestClient(baseUrl + "/api/events");
            client.connect();
            clients.add(client);
            Thread.sleep(100); // Small delay between connections
        }

        // Step 2: Send a prompt
        String promptUrl = baseUrl + "/api/prompt";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "{\"prompt\": \"Hello all\"}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(promptUrl, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Step 3: Wait for events on all clients
        for (SseTestClient client : clients) {
            client.waitForEvents(1, 5000);
            List<String> events = client.getEvents();
            assertFalse(events.isEmpty(), "Client should have received at least one event");
            assertTrue(events.get(0).contains("Hello all"), "Event should contain the sent message");
        }

        // Step 4: Cleanup
        for (SseTestClient client : clients) {
            client.disconnect();
        }
    }
}

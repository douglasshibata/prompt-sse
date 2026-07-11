package com.example.prompt.integration;

import com.example.prompt.PromptApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    classes = PromptApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ExtendedFlowIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void extendedConversationFlow_WorksEndToEnd() throws Exception {
        String baseUrl = "http://localhost:" + port;
        SseTestClient client = new SseTestClient(baseUrl + "/api/events");
        client.connect();

        String promptUrl = baseUrl + "/api/prompt";

        // Step 1: Start conversation
        sendPrompt(promptUrl, "Hi");
        client.waitForEvents(1, 4000);
        assertFalse(client.getEvents().isEmpty(), "No events received for Hi");
        assertTrue(client.getEvents().get(0).contains("What's your name?"), "Unexpected response: " + client.getEvents().get(0));

        // Step 2: Provide name
        sendPrompt(promptUrl, "Alice");
        client.waitForEvents(2, 4000);
        assertTrue(client.getEvents().get(1).contains("Nice to meet you, Alice!"));

        // Step 3: Choose topic
        sendPrompt(promptUrl, "SSE");
        client.waitForEvents(3, 4000);
        assertTrue(client.getEvents().get(2).contains("interested in sse"));

        // Step 4: Ask question
        sendPrompt(promptUrl, "What is SSE?");
        client.waitForEvents(4, 4000);
        assertTrue(client.getEvents().get(3).contains("Server-Sent Events"));

        // Step 5: Follow-up
        sendPrompt(promptUrl, "How does it work?");
        client.waitForEvents(5, 4000);
        assertTrue(client.getEvents().get(4).contains("text/event-stream"));

        // Step 6: End conversation
        sendPrompt(promptUrl, "bye");
        client.waitForEvents(6, 4000);
        assertTrue(client.getEvents().get(5).contains("Goodbye!"));

        client.disconnect();
    }

    private void sendPrompt(String url, String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Session-Id", "alice-session-1");
        String requestBody = "{\"prompt\": \"" + prompt + "\"}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        restTemplate.postForEntity(url, request, String.class);
    }
}

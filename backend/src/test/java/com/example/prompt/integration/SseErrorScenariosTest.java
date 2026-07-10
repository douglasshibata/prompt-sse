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
class SseErrorScenariosTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void clientDisconnect_ServerHandlesGracefully() throws Exception {
        String baseUrl = "http://localhost:" + port;
        SseTestClient client = new SseTestClient(baseUrl + "/api/events");
        client.connect();

        // Wait a bit for connection to establish
        Thread.sleep(500);

        // Disconnect client
        client.disconnect();

        // Server should not crash; we can still send a prompt
        String promptUrl = baseUrl + "/api/prompt";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "{\"prompt\": \"Test after disconnect\"}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(promptUrl, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void invalidPrompt_NoEventBroadcast() throws Exception {
        String baseUrl = "http://localhost:" + port;
        SseTestClient client = new SseTestClient(baseUrl + "/api/events");
        client.connect();

        // Send empty prompt
        String promptUrl = baseUrl + "/api/prompt";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "{\"prompt\": \"   \"}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(promptUrl, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Wait a bit and check no events were received
        Thread.sleep(1000);
        assertTrue(client.getEvents().isEmpty(), "No events should be broadcast for empty prompt");

        client.disconnect();
    }
}

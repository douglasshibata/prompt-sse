package com.example.prompt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConversationServiceTest {
    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationService();
    }

    @Test
    void processUserMessage_StartToNameFlow() {
        String sessionId = "session1";

        String response1 = conversationService.processUserMessage(sessionId, "Hi");
        assertEquals("Hello! I'm a simple chatbot. What's your name?", response1);

        String response2 = conversationService.processUserMessage(sessionId, "Alice");
        assertEquals("Nice to meet you, Alice! What topic are you interested in? (SSE, Angular, Spring Boot)", response2);

        String response3 = conversationService.processUserMessage(sessionId, "SSE");
        assertTrue(response3.contains("Great! You're interested in sse."));
        assertTrue(response3.contains("Ask me a question about it"));

        String response4 = conversationService.processUserMessage(sessionId, "What is SSE?");
        assertTrue(response4.contains("SSE stands for Server-Sent Events"));

        String response5 = conversationService.processUserMessage(sessionId, "How does it work?");
        assertTrue(response5.contains("text/event-stream"));

        String response6 = conversationService.processUserMessage(sessionId, "bye");
        assertEquals("Goodbye! Feel free to come back anytime.", response6);
    }

    @Test
    void processUserMessage_ByeAtAnyState() {
        String sessionIdA = "session2_A";
        conversationService.processUserMessage(sessionIdA, "Hi");
        String response1 = conversationService.processUserMessage(sessionIdA, "bye");
        assertEquals("Goodbye! Feel free to come back anytime.", response1);

        String sessionIdB = "session2_B";
        conversationService.processUserMessage(sessionIdB, "Hi");
        conversationService.processUserMessage(sessionIdB, "Alice");
        String response2 = conversationService.processUserMessage(sessionIdB, "bye");
        assertEquals("Goodbye! Feel free to come back anytime.", response2);

        String sessionIdC = "session2_C";
        conversationService.processUserMessage(sessionIdC, "Hi");
        conversationService.processUserMessage(sessionIdC, "Alice");
        conversationService.processUserMessage(sessionIdC, "SSE");
        String response3 = conversationService.processUserMessage(sessionIdC, "bye");
        assertEquals("Goodbye! Feel free to come back anytime.", response3);
    }

    @Test
    void processUserMessage_EndStateMessage() {
        String sessionId = "session3";

        conversationService.processUserMessage(sessionId, "Hi");
        conversationService.processUserMessage(sessionId, "bye"); // -> END

        String response = conversationService.processUserMessage(sessionId, "Hello again?");
        assertEquals("This conversation has ended. Refresh the page to start a new one.", response);
    }
}

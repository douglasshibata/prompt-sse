package com.example.prompt.controller;

import com.example.prompt.model.Conversation;
import com.example.prompt.service.SseService;
import com.example.prompt.service.ConversationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PromptController {
    private final SseService sseService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    public PromptController(SseService sseService, ConversationService conversationService, ObjectMapper objectMapper) {
        this.sseService = sseService;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/prompt")
    public void handlePrompt(@RequestBody Map<String, String> request,
                             @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        String userPrompt = request.get("prompt");
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return;
        }

        if (sessionId == null) {
            sessionId = "default-session";
        }

        // Get bot response based on conversation state
        Conversation conv = conversationService.getOrCreate(sessionId);
        String aiResponse = conversationService.processUserMessage(sessionId, userPrompt);

        // Build event data via Jackson ObjectMapper
        try {
            Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("user", userPrompt);
            eventMap.put("ai", aiResponse);
            eventMap.put("time", Instant.now().toString());
            eventMap.put("userName", conv.getUserName() != null ? conv.getUserName() : "");
            eventMap.put("topic", conv.getPreferredTopic() != null ? conv.getPreferredTopic() : "");

            String eventData = objectMapper.writeValueAsString(eventMap);

            // Broadcast to all connected clients
            sseService.broadcast(eventData);
        } catch (Exception e) {
            System.err.println("Error serializing JSON event data: " + e.getMessage());
        }
    }
}

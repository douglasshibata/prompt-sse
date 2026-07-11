package com.example.prompt.controller;

import com.example.prompt.model.Conversation;
import com.example.prompt.service.ConversationService;
import com.example.prompt.service.SseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptControllerTest {
    @Mock
    private SseService sseService;

    @Mock
    private ConversationService conversationService;

    private PromptController promptController;

    @BeforeEach
    void setUp() {
        promptController = new PromptController(sseService, conversationService, new ObjectMapper());
    }

    @Test
    void handlePrompt_ValidPrompt_CallsServices() {
        Map<String, String> request = new HashMap<>();
        request.put("prompt", "Hello");

        when(conversationService.getOrCreate(anyString()))
            .thenReturn(new Conversation("session123"));

        when(conversationService.processUserMessage(anyString(), anyString()))
            .thenReturn("Mocked response");

        promptController.handlePrompt(request, "session123");

        verify(conversationService).processUserMessage("session123", "Hello");
        verify(sseService).broadcast(anyString());
    }

    @Test
    void handlePrompt_EmptyPrompt_DoesNothing() {
        Map<String, String> request = new HashMap<>();
        request.put("prompt", "   ");

        promptController.handlePrompt(request, "session123");

        verifyNoInteractions(conversationService);
        verifyNoInteractions(sseService);
    }

    @Test
    void handlePrompt_NullPrompt_DoesNothing() {
        Map<String, String> request = new HashMap<>();
        request.put("prompt", null);

        promptController.handlePrompt(request, "session123");

        verifyNoInteractions(conversationService);
        verifyNoInteractions(sseService);
    }
}

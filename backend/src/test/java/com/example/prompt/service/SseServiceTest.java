package com.example.prompt.service;

import com.example.prompt.factory.SseEmitterFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SseServiceTest {
    @Mock
    private SseEmitterFactory emitterFactory;

    private SseService sseService;

    @BeforeEach
    void setUp() {
        sseService = new SseService(emitterFactory);
    }

    @Test
    void createEmitter_AddsEmitterToList() {
        when(emitterFactory.create(anyLong())).thenReturn(new SseEmitter());

        SseEmitter emitter = sseService.createEmitter();
        assertNotNull(emitter);
    }

    @Test
    void broadcast_SendsDataToAllEmitters() {
        when(emitterFactory.create(anyLong())).thenReturn(new SseEmitter());
        sseService.createEmitter();

        sseService.broadcast("test data");
    }
}

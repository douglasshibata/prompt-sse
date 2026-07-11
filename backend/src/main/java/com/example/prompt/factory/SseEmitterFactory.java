package com.example.prompt.factory;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.stereotype.Component;

@Component
public class SseEmitterFactory {
    public SseEmitter create(long timeout) {
        return new SseEmitter(timeout);
    }
}

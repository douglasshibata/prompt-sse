package com.example.prompt.service;

import com.example.prompt.factory.SseEmitterFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final SseEmitterFactory emitterFactory;

    public SseService(SseEmitterFactory emitterFactory) {
        this.emitterFactory = emitterFactory;
    }

    public SseEmitter createEmitter() {
        SseEmitter emitter = emitterFactory.create(60_000L); // 60s timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            System.out.println("Emitter completed, remaining: " + emitters.size());
        });

        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            System.out.println("Emitter timeout, remaining: " + emitters.size());
        });

        emitter.onError((e) -> {
            emitters.remove(emitter);
            System.out.println("Emitter error, remaining: " + emitters.size());
        });

        // Send an initial handshake asynchronously after the controller returns the emitter
        CompletableFuture.runAsync(() -> {
            try {
                // Wait a tiny bit to ensure controller has returned the emitter and async request is initialized
                Thread.sleep(100);
                emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        });

        return emitter;
    }

    public void broadcast(String data) {
        List<SseEmitter> failedEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .data(data)
                    .name("response"));
            } catch (IOException e) {
                failedEmitters.add(emitter);
            }
        }

        emitters.removeAll(failedEmitters);
    }
}

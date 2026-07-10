package com.example.prompt.service;

import com.example.prompt.model.Conversation;
import com.example.prompt.model.ConversationState;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationService {
    private final Map<String, Conversation> conversations = new ConcurrentHashMap<>();

    public Conversation getOrCreate(String sessionId) {
        return conversations.computeIfAbsent(sessionId, Conversation::new);
    }

    public String processUserMessage(String sessionId, String userMessage) {
        Conversation conv = getOrCreate(sessionId);
        ConversationState state = conv.getState();
        String response;

        switch (state) {
            case START:
                response = "Hello! I'm a simple chatbot. What's your name?";
                conv.setState(ConversationState.WAITING_FOR_NAME);
                break;

            case WAITING_FOR_NAME:
                if (isByeMessage(userMessage)) {
                    response = "Goodbye! Feel free to come back anytime.";
                    conv.setState(ConversationState.END);
                } else {
                    conv.setUserName(userMessage.trim());
                    response = String.format("Nice to meet you, %s! What topic are you interested in? (SSE, Angular, Spring Boot)",
                        conv.getUserName());
                    conv.setState(ConversationState.WAITING_FOR_TOPIC);
                }
                break;

            case WAITING_FOR_TOPIC:
                if (isByeMessage(userMessage)) {
                    response = "Goodbye! Feel free to come back anytime.";
                    conv.setState(ConversationState.END);
                } else {
                    String topic = userMessage.trim().toLowerCase();
                    conv.setPreferredTopic(topic);
                    response = String.format("Great! You're interested in %s. Ask me a question about it, or say 'bye'.",
                        topic);
                    conv.setState(ConversationState.WAITING_FOR_QUESTION);
                }
                break;

            case WAITING_FOR_QUESTION:
                if (isByeMessage(userMessage)) {
                    response = "Goodbye! Feel free to come back anytime.";
                    conv.setState(ConversationState.END);
                } else {
                    response = generateAnswer(conv.getPreferredTopic(), userMessage);
                    conv.setState(ConversationState.WAITING_FOR_FOLLOWUP);
                }
                break;

            case WAITING_FOR_FOLLOWUP:
                if (isByeMessage(userMessage)) {
                    response = "Goodbye! Feel free to come back anytime.";
                    conv.setState(ConversationState.END);
                } else {
                    response = generateAnswer(conv.getPreferredTopic(), userMessage);
                    // Stay in WAITING_FOR_FOLLOWUP
                }
                break;

            case END:
            default:
                response = "This conversation has ended. Refresh the page to start a new one.";
        }

        return response;
    }

    public boolean isByeMessage(String message) {
        return message != null && message.trim().toLowerCase().contains("bye");
    }

    private String generateAnswer(String topic, String question) {
        if (topic == null) topic = "";
        String q = question.toLowerCase();
        String t = topic.toLowerCase();
        if (t.contains("sse")) {
            if (q.contains("what") || q.contains("sse")) {
                return "SSE stands for Server-Sent Events. It's a way for a server to push data to a client over HTTP.";
            } else if (q.contains("how") && q.contains("work")) {
                return "The server sends events in a text/event-stream format, and the client listens using EventSource.";
            }
        } else if (t.contains("angular")) {
            return "Angular is a frontend framework for building web applications.";
        } else if (t.contains("spring boot")) {
            return "Spring Boot is a Java framework for building web services and applications.";
        }
        return "I'm a simple demo chatbot. I understand basic questions about SSE, Angular, and Spring Boot.";
    }
}

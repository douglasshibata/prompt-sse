package com.example.prompt.model;

public enum ConversationState {
    START,
    WAITING_FOR_NAME,
    WAITING_FOR_TOPIC,
    WAITING_FOR_QUESTION,
    WAITING_FOR_FOLLOWUP,
    END
}

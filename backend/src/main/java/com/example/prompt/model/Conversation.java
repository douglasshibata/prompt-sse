package com.example.prompt.model;

public class Conversation {
    private String sessionId;
    private ConversationState state;
    private String userName;
    private String preferredTopic;

    public Conversation(String sessionId) {
        this.sessionId = sessionId;
        this.state = ConversationState.START;
    }

    public String getSessionId() { return sessionId; }
    public ConversationState getState() { return state; }
    public void setState(ConversationState state) { this.state = state; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPreferredTopic() { return preferredTopic; }
    public void setPreferredTopic(String preferredTopic) { this.preferredTopic = preferredTopic; }
}

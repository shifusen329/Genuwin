package com.genuwin.app.api.models;

import java.util.List;

/**
 * Simplified request model for Ollama chat API without tools
 */
public class SimpleChatRequest {
    private String model;
    private List<ChatMessage> messages;
    private boolean stream;
    private String system;
    
    public SimpleChatRequest() {}
    
    public SimpleChatRequest(String model, List<ChatMessage> messages, boolean stream, String system) {
        this.model = model;
        this.messages = messages;
        this.stream = stream;
        this.system = system;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    public boolean isStream() {
        return stream;
    }
    
    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }
}

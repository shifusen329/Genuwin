package com.genuwin.app.api.models;

import java.util.List;

/**
 * Request model for Ollama chat API and OpenAI Responses API
 */
public class ChatRequest {
    private String model;
    private List<ChatMessage> messages;
    private boolean stream;
    private String system;
    private Object tools;
    
    // OpenAI-specific fields
    private Object input;
    private String instructions;
    
    public ChatRequest() {}
    
    public ChatRequest(String model, List<ChatMessage> messages, boolean stream, String system) {
        this.model = model;
        this.messages = messages;
        this.stream = stream;
        this.system = system;
    }
    
    public ChatRequest(String model, List<ChatMessage> messages, boolean stream, String system, Object tools) {
        this.model = model;
        this.messages = messages;
        this.stream = stream;
        this.system = system;
        this.tools = tools;
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

    public Object getTools() {
        return tools;
    }

    public void setTools(Object tools) {
        this.tools = tools;
    }

    public Object getInput() {
        return input;
    }

    public void setInput(Object input) {
        this.input = input;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}

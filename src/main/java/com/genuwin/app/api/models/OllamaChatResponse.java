package com.genuwin.app.api.models;

import java.util.List;

/**
 * Response model for Ollama chat API and OpenAI Responses API
 */
public class OllamaChatResponse {
    private String model;
    private String created_at;
    private ChatMessage message;
    private boolean done;
    
    // OpenAI-specific fields for response format
    private String id;
    private String object;
    private String type; // Missing field declaration
    private String role; // Missing field declaration
    private String status;
    private String output_text; // OpenAI convenience field that aggregates all text outputs
    private String response; // For OpenAI text response (fallback)
    private List<ToolCall> tool_calls; // For OpenAI tool calls
    private List<Object> output; // OpenAI output array containing the actual response content
    
    public OllamaChatResponse() {}
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getCreated_at() {
        return created_at;
    }
    
    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }
    
    public ChatMessage getMessage() {
        return message;
    }
    
    public void setMessage(ChatMessage message) {
        this.message = message;
    }
    
    public boolean isDone() {
        return done;
    }
    
    public void setDone(boolean done) {
        this.done = done;
    }
    
    // OpenAI-specific getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getOutputText() {
        return output_text;
    }
    
    public void setOutputText(String output_text) {
        this.output_text = output_text;
    }
    
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    public List<ToolCall> getToolCalls() {
        return tool_calls;
    }
    
    public void setToolCalls(List<ToolCall> tool_calls) {
        this.tool_calls = tool_calls;
    }
    
    public boolean hasToolCalls() {
        return tool_calls != null && !tool_calls.isEmpty();
    }
    
    public List<Object> getOutput() {
        return output;
    }
    
    public void setOutput(List<Object> output) {
        this.output = output;
    }
}

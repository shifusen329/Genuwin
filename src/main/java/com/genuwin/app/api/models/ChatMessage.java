package com.genuwin.app.api.models;

import java.util.List;

/**
 * Model class for chat messages
 */
public class ChatMessage {
    private String role;
    private String content;
    private List<ToolCall> tool_calls;
    private String tool_call_id;
    private String timestamp;
    
    public ChatMessage() {}
    
    public ChatMessage(String role, String content, String timestamp) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }
    
    public ChatMessage(String role, String content, String toolCallId, String timestamp) {
        this.role = role;
        this.content = content;
        this.tool_call_id = toolCallId;
        this.timestamp = timestamp;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
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
    
    public String getToolCallId() {
        return tool_call_id;
    }
    
    public void setToolCallId(String tool_call_id) {
        this.tool_call_id = tool_call_id;
    }
}

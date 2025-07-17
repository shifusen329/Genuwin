package com.genuwin.app.memory.operations;

import com.genuwin.app.memory.models.MemoryType;

/**
 * Operation to create a new memory
 * Used when the Memory Agent determines that new information should be stored
 */
public class CreateMemoryOperation extends MemoryOperation {
    
    private final String content;
    private final MemoryType type;
    private final float importance;
    private final String metadata;
    
    public CreateMemoryOperation(String content, MemoryType type, float importance, 
                               String reasoning, float confidence, String metadata) {
        super(OperationType.CREATE, reasoning, confidence);
        this.content = content;
        this.type = type;
        this.importance = importance;
        this.metadata = metadata;
    }
    
    public CreateMemoryOperation(String content, MemoryType type, float importance, 
                               String reasoning, float confidence) {
        this(content, type, importance, reasoning, confidence, null);
    }
    
    public String getContent() {
        return content;
    }
    
    public MemoryType getType() {
        return type;
    }
    
    public float getImportance() {
        return importance;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    @Override
    public boolean isValid() {
        // Validate content
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        // Content should be reasonable length (not too short or too long)
        String trimmedContent = content.trim();
        if (trimmedContent.length() < 3 || trimmedContent.length() > 2000) {
            return false;
        }
        
        // Validate type
        if (type == null) {
            return false;
        }
        
        // Validate importance (should be between 0.0 and 1.0)
        if (importance < 0.0f || importance > 1.0f) {
            return false;
        }
        
        // Validate confidence (should be between 0.0 and 1.0)
        if (confidence < 0.0f || confidence > 1.0f) {
            return false;
        }
        
        // Reasoning should be provided for transparency
        if (reasoning == null || reasoning.trim().isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return String.format("CREATE memory: '%s' (type=%s, importance=%.2f, confidence=%.2f)", 
                           content.length() > 50 ? content.substring(0, 47) + "..." : content,
                           type, importance, confidence);
    }
    
    @Override
    public String toString() {
        return "CreateMemoryOperation{" +
                "content='" + content + '\'' +
                ", type=" + type +
                ", importance=" + importance +
                ", reasoning='" + reasoning + '\'' +
                ", confidence=" + confidence +
                ", metadata='" + metadata + '\'' +
                '}';
    }
}

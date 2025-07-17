package com.genuwin.app.memory.operations;

import com.genuwin.app.memory.models.MemoryType;

/**
 * Operation to completely replace an existing memory
 * Used when the Memory Agent determines that existing information is incorrect or outdated
 * and should be completely replaced rather than updated
 */
public class ReplaceMemoryOperation extends MemoryOperation {
    
    private final String memoryId;
    private final String newContent;
    private final MemoryType newType;
    private final float newImportance;
    private final String newMetadata;
    
    public ReplaceMemoryOperation(String memoryId, String newContent, MemoryType newType, 
                                float newImportance, String reasoning, float confidence, 
                                String newMetadata) {
        super(OperationType.REPLACE, reasoning, confidence);
        this.memoryId = memoryId;
        this.newContent = newContent;
        this.newType = newType;
        this.newImportance = newImportance;
        this.newMetadata = newMetadata;
    }
    
    public ReplaceMemoryOperation(String memoryId, String newContent, MemoryType newType, 
                                float newImportance, String reasoning, float confidence) {
        this(memoryId, newContent, newType, newImportance, reasoning, confidence, null);
    }
    
    public String getMemoryId() {
        return memoryId;
    }
    
    public String getNewContent() {
        return newContent;
    }
    
    public MemoryType getNewType() {
        return newType;
    }
    
    public float getNewImportance() {
        return newImportance;
    }
    
    public String getNewMetadata() {
        return newMetadata;
    }
    
    @Override
    public boolean isValid() {
        // Must have a valid memory ID
        if (memoryId == null || memoryId.trim().isEmpty()) {
            return false;
        }
        
        // Must have new content
        if (newContent == null || newContent.trim().isEmpty()) {
            return false;
        }
        
        // Content should be reasonable length
        String trimmedContent = newContent.trim();
        if (trimmedContent.length() < 3 || trimmedContent.length() > 2000) {
            return false;
        }
        
        // Must have a type
        if (newType == null) {
            return false;
        }
        
        // Validate importance
        if (newImportance < 0.0f || newImportance > 1.0f) {
            return false;
        }
        
        // Validate confidence
        if (confidence < 0.0f || confidence > 1.0f) {
            return false;
        }
        
        // Reasoning should be provided for transparency (especially important for replace operations)
        if (reasoning == null || reasoning.trim().isEmpty()) {
            return false;
        }
        
        // Replace operations should have high confidence since they're destructive
        if (confidence < 0.7f) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        String contentPreview = newContent.length() > 50 ? newContent.substring(0, 47) + "..." : newContent;
        return String.format("REPLACE memory %s: '%s' (type=%s, importance=%.2f, confidence=%.2f)", 
                           memoryId, contentPreview, newType, newImportance, confidence);
    }
    
    @Override
    public String toString() {
        return "ReplaceMemoryOperation{" +
                "memoryId='" + memoryId + '\'' +
                ", newContent='" + newContent + '\'' +
                ", newType=" + newType +
                ", newImportance=" + newImportance +
                ", reasoning='" + reasoning + '\'' +
                ", confidence=" + confidence +
                ", newMetadata='" + newMetadata + '\'' +
                '}';
    }
}

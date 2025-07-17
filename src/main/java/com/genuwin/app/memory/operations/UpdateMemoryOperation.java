package com.genuwin.app.memory.operations;

import com.genuwin.app.memory.models.MemoryType;

/**
 * Operation to update an existing memory
 * Used when the Memory Agent determines that existing information should be refined or enhanced
 */
public class UpdateMemoryOperation extends MemoryOperation {
    
    private final String memoryId;
    private final String newContent;
    private final MemoryType newType;
    private final Float newImportance;
    private final String newMetadata;
    
    public UpdateMemoryOperation(String memoryId, String newContent, MemoryType newType, 
                               Float newImportance, String reasoning, float confidence, 
                               String newMetadata) {
        super(OperationType.UPDATE, reasoning, confidence);
        this.memoryId = memoryId;
        this.newContent = newContent;
        this.newType = newType;
        this.newImportance = newImportance;
        this.newMetadata = newMetadata;
    }
    
    public UpdateMemoryOperation(String memoryId, String newContent, MemoryType newType, 
                               Float newImportance, String reasoning, float confidence) {
        this(memoryId, newContent, newType, newImportance, reasoning, confidence, null);
    }
    
    // Constructor for content-only updates
    public UpdateMemoryOperation(String memoryId, String newContent, String reasoning, float confidence) {
        this(memoryId, newContent, null, null, reasoning, confidence, null);
    }
    
    // Constructor for importance-only updates
    public UpdateMemoryOperation(String memoryId, Float newImportance, String reasoning, float confidence) {
        this(memoryId, null, null, newImportance, reasoning, confidence, null);
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
    
    public Float getNewImportance() {
        return newImportance;
    }
    
    public String getNewMetadata() {
        return newMetadata;
    }
    
    public boolean hasContentUpdate() {
        return newContent != null && !newContent.trim().isEmpty();
    }
    
    public boolean hasTypeUpdate() {
        return newType != null;
    }
    
    public boolean hasImportanceUpdate() {
        return newImportance != null;
    }
    
    public boolean hasMetadataUpdate() {
        return newMetadata != null;
    }
    
    @Override
    public boolean isValid() {
        // Must have a valid memory ID
        if (memoryId == null || memoryId.trim().isEmpty()) {
            return false;
        }
        
        // Must have at least one field to update
        if (!hasContentUpdate() && !hasTypeUpdate() && !hasImportanceUpdate() && !hasMetadataUpdate()) {
            return false;
        }
        
        // Validate content if provided
        if (hasContentUpdate()) {
            String trimmedContent = newContent.trim();
            if (trimmedContent.length() < 3 || trimmedContent.length() > 2000) {
                return false;
            }
        }
        
        // Validate importance if provided
        if (hasImportanceUpdate()) {
            if (newImportance < 0.0f || newImportance > 1.0f) {
                return false;
            }
        }
        
        // Validate confidence
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
        StringBuilder desc = new StringBuilder("UPDATE memory " + memoryId + ": ");
        
        if (hasContentUpdate()) {
            String contentPreview = newContent.length() > 30 ? newContent.substring(0, 27) + "..." : newContent;
            desc.append("content='").append(contentPreview).append("' ");
        }
        
        if (hasTypeUpdate()) {
            desc.append("type=").append(newType).append(" ");
        }
        
        if (hasImportanceUpdate()) {
            desc.append("importance=").append(String.format("%.2f", newImportance)).append(" ");
        }
        
        if (hasMetadataUpdate()) {
            desc.append("metadata=updated ");
        }
        
        desc.append("(confidence=").append(String.format("%.2f", confidence)).append(")");
        
        return desc.toString().trim();
    }
    
    @Override
    public String toString() {
        return "UpdateMemoryOperation{" +
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

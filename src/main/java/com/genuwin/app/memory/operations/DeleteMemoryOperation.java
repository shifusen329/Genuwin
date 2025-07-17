package com.genuwin.app.memory.operations;

/**
 * Operation to delete an existing memory
 * Used when the Memory Agent determines that information is no longer relevant,
 * incorrect, or should be removed for privacy/safety reasons
 */
public class DeleteMemoryOperation extends MemoryOperation {
    
    private final String memoryId;
    private final boolean deleteRelationships;
    
    public DeleteMemoryOperation(String memoryId, String reasoning, float confidence, 
                               boolean deleteRelationships) {
        super(OperationType.DELETE, reasoning, confidence);
        this.memoryId = memoryId;
        this.deleteRelationships = deleteRelationships;
    }
    
    public DeleteMemoryOperation(String memoryId, String reasoning, float confidence) {
        this(memoryId, reasoning, confidence, true); // Default to deleting relationships
    }
    
    public String getMemoryId() {
        return memoryId;
    }
    
    public boolean shouldDeleteRelationships() {
        return deleteRelationships;
    }
    
    @Override
    public boolean isValid() {
        // Must have a valid memory ID
        if (memoryId == null || memoryId.trim().isEmpty()) {
            return false;
        }
        
        // Validate confidence
        if (confidence < 0.0f || confidence > 1.0f) {
            return false;
        }
        
        // Reasoning should be provided for transparency (especially important for delete operations)
        if (reasoning == null || reasoning.trim().isEmpty()) {
            return false;
        }
        
        // Delete operations should have high confidence since they're destructive
        if (confidence < 0.8f) {
            return false;
        }
        
        // Reasoning should be substantial for delete operations
        if (reasoning.trim().length() < 10) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return String.format("DELETE memory %s (deleteRelationships=%s, confidence=%.2f)", 
                           memoryId, deleteRelationships, confidence);
    }
    
    @Override
    public String toString() {
        return "DeleteMemoryOperation{" +
                "memoryId='" + memoryId + '\'' +
                ", reasoning='" + reasoning + '\'' +
                ", confidence=" + confidence +
                ", deleteRelationships=" + deleteRelationships +
                '}';
    }
}

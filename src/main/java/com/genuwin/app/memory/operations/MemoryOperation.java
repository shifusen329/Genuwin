package com.genuwin.app.memory.operations;

import com.genuwin.app.memory.models.MemoryType;

/**
 * Base class for all memory operations
 * Represents an action that the Memory Agent wants to perform on the memory system
 */
public abstract class MemoryOperation {
    
    public enum OperationType {
        CREATE,
        UPDATE, 
        REPLACE,
        DELETE,
        MERGE
    }
    
    protected final OperationType operationType;
    protected final String reasoning;
    protected final float confidence;
    
    public MemoryOperation(OperationType operationType, String reasoning, float confidence) {
        this.operationType = operationType;
        this.reasoning = reasoning;
        this.confidence = confidence;
    }
    
    public OperationType getOperationType() {
        return operationType;
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public float getConfidence() {
        return confidence;
    }
    
    /**
     * Validate that this operation is safe and reasonable to execute
     * @return true if the operation should be executed, false otherwise
     */
    public abstract boolean isValid();
    
    /**
     * Get a human-readable description of this operation for logging/debugging
     */
    public abstract String getDescription();
}

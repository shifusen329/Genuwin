package com.genuwin.app.memory.operations;

import com.genuwin.app.memory.models.MemoryType;
import java.util.List;

/**
 * Operation to merge multiple existing memories into a single memory
 * Used when the Memory Agent determines that multiple memories contain
 * related or duplicate information that should be consolidated
 */
public class MergeMemoryOperation extends MemoryOperation {
    
    private final List<String> sourceMemoryIds;
    private final String mergedContent;
    private final MemoryType mergedType;
    private final float mergedImportance;
    private final String mergedMetadata;
    private final boolean deleteSourceMemories;
    
    public MergeMemoryOperation(List<String> sourceMemoryIds, String mergedContent, 
                              MemoryType mergedType, float mergedImportance, 
                              String reasoning, float confidence, String mergedMetadata,
                              boolean deleteSourceMemories) {
        super(OperationType.MERGE, reasoning, confidence);
        this.sourceMemoryIds = sourceMemoryIds;
        this.mergedContent = mergedContent;
        this.mergedType = mergedType;
        this.mergedImportance = mergedImportance;
        this.mergedMetadata = mergedMetadata;
        this.deleteSourceMemories = deleteSourceMemories;
    }
    
    public MergeMemoryOperation(List<String> sourceMemoryIds, String mergedContent, 
                              MemoryType mergedType, float mergedImportance, 
                              String reasoning, float confidence) {
        this(sourceMemoryIds, mergedContent, mergedType, mergedImportance, 
             reasoning, confidence, null, true); // Default to deleting source memories
    }
    
    public List<String> getSourceMemoryIds() {
        return sourceMemoryIds;
    }
    
    public String getMergedContent() {
        return mergedContent;
    }
    
    public MemoryType getMergedType() {
        return mergedType;
    }
    
    public float getMergedImportance() {
        return mergedImportance;
    }
    
    public String getMergedMetadata() {
        return mergedMetadata;
    }
    
    public boolean shouldDeleteSourceMemories() {
        return deleteSourceMemories;
    }
    
    @Override
    public boolean isValid() {
        // Must have at least 2 source memories to merge
        if (sourceMemoryIds == null || sourceMemoryIds.size() < 2) {
            return false;
        }
        
        // All source memory IDs must be valid
        for (String memoryId : sourceMemoryIds) {
            if (memoryId == null || memoryId.trim().isEmpty()) {
                return false;
            }
        }
        
        // Must have merged content
        if (mergedContent == null || mergedContent.trim().isEmpty()) {
            return false;
        }
        
        // Content should be reasonable length
        String trimmedContent = mergedContent.trim();
        if (trimmedContent.length() < 3 || trimmedContent.length() > 3000) { // Allow longer for merged content
            return false;
        }
        
        // Must have a type
        if (mergedType == null) {
            return false;
        }
        
        // Validate importance
        if (mergedImportance < 0.0f || mergedImportance > 1.0f) {
            return false;
        }
        
        // Validate confidence
        if (confidence < 0.0f || confidence > 1.0f) {
            return false;
        }
        
        // Reasoning should be provided for transparency
        if (reasoning == null || reasoning.trim().isEmpty()) {
            return false;
        }
        
        // Merge operations should have reasonable confidence
        if (confidence < 0.6f) {
            return false;
        }
        
        // Reasoning should explain why these memories should be merged
        if (reasoning.trim().length() < 15) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        String contentPreview = mergedContent.length() > 40 ? mergedContent.substring(0, 37) + "..." : mergedContent;
        return String.format("MERGE %d memories into: '%s' (type=%s, importance=%.2f, confidence=%.2f, deleteSource=%s)", 
                           sourceMemoryIds.size(), contentPreview, mergedType, mergedImportance, 
                           confidence, deleteSourceMemories);
    }
    
    @Override
    public String toString() {
        return "MergeMemoryOperation{" +
                "sourceMemoryIds=" + sourceMemoryIds +
                ", mergedContent='" + mergedContent + '\'' +
                ", mergedType=" + mergedType +
                ", mergedImportance=" + mergedImportance +
                ", reasoning='" + reasoning + '\'' +
                ", confidence=" + confidence +
                ", mergedMetadata='" + mergedMetadata + '\'' +
                ", deleteSourceMemories=" + deleteSourceMemories +
                '}';
    }
}

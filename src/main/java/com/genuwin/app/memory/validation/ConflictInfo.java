package com.genuwin.app.memory.validation;

import com.genuwin.app.memory.models.Memory;

import java.util.ArrayList;
import java.util.List;

/**
 * Conflict Info - Represents a specific conflict detected in a memory operation
 * 
 * Contains details about the conflict, the conflicting memory, and suggested resolutions.
 * Used by MemoryConflictResolver to provide detailed conflict analysis.
 */
public class ConflictInfo {
    
    private final MemoryConflictResolver.ConflictType conflictType;
    private final Memory conflictingMemory;
    private final String description;
    private final float similarityScore;
    private final List<ResolutionSuggestion> suggestedResolutions;
    
    public ConflictInfo(MemoryConflictResolver.ConflictType conflictType, 
                       Memory conflictingMemory, 
                       String description, 
                       float similarityScore) {
        this.conflictType = conflictType;
        this.conflictingMemory = conflictingMemory;
        this.description = description;
        this.similarityScore = similarityScore;
        this.suggestedResolutions = new ArrayList<>();
    }
    
    /**
     * Get the type of conflict
     */
    public MemoryConflictResolver.ConflictType getConflictType() {
        return conflictType;
    }
    
    /**
     * Get the memory that conflicts with the operation
     */
    public Memory getConflictingMemory() {
        return conflictingMemory;
    }
    
    /**
     * Get a description of the conflict
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the similarity score between conflicting content (if applicable)
     * @return Similarity score 0.0-1.0, or 0.0 if not similarity-based
     */
    public float getSimilarityScore() {
        return similarityScore;
    }
    
    /**
     * Get suggested resolutions for this conflict
     */
    public List<ResolutionSuggestion> getSuggestedResolutions() {
        return new ArrayList<>(suggestedResolutions);
    }
    
    /**
     * Add a suggested resolution for this conflict
     */
    public void addSuggestedResolution(MemoryConflictResolver.ResolutionStrategy strategy, String reasoning) {
        suggestedResolutions.add(new ResolutionSuggestion(strategy, reasoning));
    }
    
    /**
     * Check if this conflict has any suggested resolutions
     */
    public boolean hasSuggestedResolutions() {
        return !suggestedResolutions.isEmpty();
    }
    
    /**
     * Get the severity of this conflict
     */
    public ConflictSeverity getSeverity() {
        switch (conflictType) {
            case CONTRADICTION:
                return ConflictSeverity.HIGH;
            case INFORMATION_LOSS:
                return ConflictSeverity.HIGH;
            case DEPENDENCY_BREAK:
                return ConflictSeverity.MEDIUM;
            case RELATED_CONTRADICTION:
                return ConflictSeverity.MEDIUM;
            case DUPLICATE:
                return ConflictSeverity.LOW;
            default:
                return ConflictSeverity.MEDIUM;
        }
    }
    
    /**
     * Get a formatted summary of this conflict
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("🔥 CONFLICT: ").append(conflictType).append("\n");
        summary.append("📝 Description: ").append(description).append("\n");
        summary.append("🎯 Conflicting Memory: ").append(conflictingMemory.getId()).append("\n");
        summary.append("📊 Content: ").append(
            conflictingMemory.getContent().length() > 50 ? 
            conflictingMemory.getContent().substring(0, 47) + "..." : 
            conflictingMemory.getContent()
        ).append("\n");
        
        if (similarityScore > 0.0f) {
            summary.append("🔍 Similarity: ").append(String.format("%.2f", similarityScore)).append("\n");
        }
        
        summary.append("⚠️ Severity: ").append(getSeverity()).append("\n");
        
        if (hasSuggestedResolutions()) {
            summary.append("\n💡 Suggested Resolutions:\n");
            for (int i = 0; i < suggestedResolutions.size(); i++) {
                ResolutionSuggestion suggestion = suggestedResolutions.get(i);
                summary.append(i + 1).append(". ").append(suggestion.getStrategy())
                       .append(": ").append(suggestion.getReasoning()).append("\n");
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Get a concise summary for logging
     */
    public String getLogSummary() {
        return String.format("%s conflict with memory %s (similarity: %.2f, %d resolutions)", 
                           conflictType, conflictingMemory.getId(), similarityScore, 
                           suggestedResolutions.size());
    }
    
    @Override
    public String toString() {
        return "ConflictInfo{" +
                "conflictType=" + conflictType +
                ", conflictingMemory=" + conflictingMemory.getId() +
                ", description='" + description + '\'' +
                ", similarityScore=" + similarityScore +
                ", resolutions=" + suggestedResolutions.size() +
                '}';
    }
    
    /**
     * Conflict severity levels
     */
    public enum ConflictSeverity {
        LOW,     // Minor issues that can be ignored or easily resolved
        MEDIUM,  // Moderate issues that should be addressed
        HIGH     // Critical issues that must be resolved before proceeding
    }
    
    /**
     * Resolution suggestion with strategy and reasoning
     */
    public static class ResolutionSuggestion {
        private final MemoryConflictResolver.ResolutionStrategy strategy;
        private final String reasoning;
        
        public ResolutionSuggestion(MemoryConflictResolver.ResolutionStrategy strategy, String reasoning) {
            this.strategy = strategy;
            this.reasoning = reasoning;
        }
        
        public MemoryConflictResolver.ResolutionStrategy getStrategy() {
            return strategy;
        }
        
        public String getReasoning() {
            return reasoning;
        }
        
        @Override
        public String toString() {
            return strategy + ": " + reasoning;
        }
    }
}

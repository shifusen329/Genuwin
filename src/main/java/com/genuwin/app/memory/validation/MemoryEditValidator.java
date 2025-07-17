package com.genuwin.app.memory.validation;

import android.content.Context;
import android.util.Log;

import com.genuwin.app.api.services.EmbeddingService;
import com.genuwin.app.memory.models.Memory;
import com.genuwin.app.memory.models.MemoryType;
import com.genuwin.app.memory.operations.*;
import com.genuwin.app.memory.storage.MemoryStore;
import com.genuwin.app.settings.SettingsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Memory Edit Validator - Validates memory operations for safety and consistency
 * 
 * This class provides comprehensive validation for all memory operations to ensure:
 * - Safety: Prevents accidental deletion of important memories
 * - Consistency: Maintains semantic coherence in memory updates
 * - Quality: Ensures memory operations improve the overall memory system
 */
public class MemoryEditValidator {
    private static final String TAG = "MemoryEditValidator";
    
    private final Context context;
    private final MemoryStore memoryStore;
    private final EmbeddingService embeddingService;
    private final SettingsManager settingsManager;
    
    // Validation thresholds
    private static final float MIN_SEMANTIC_SIMILARITY = 0.3f; // Minimum similarity for updates
    private static final float HIGH_IMPORTANCE_THRESHOLD = 0.7f; // High importance memories need extra protection
    private static final float CONTRADICTION_THRESHOLD = 0.2f; // Below this similarity = contradiction
    private static final int MAX_CONTENT_CHANGE_RATIO = 3; // Max ratio of new content to old content
    
    public MemoryEditValidator(Context context, MemoryStore memoryStore, EmbeddingService embeddingService) {
        this.context = context;
        this.memoryStore = memoryStore;
        this.embeddingService = embeddingService;
        this.settingsManager = SettingsManager.getInstance(context);
    }
    
    /**
     * Validate a memory operation before execution
     * @param operation The operation to validate
     * @return ValidationResult with success/failure and detailed feedback
     */
    public CompletableFuture<ValidationResult> validateOperation(MemoryOperation operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Validating operation: " + operation.getDescription());
                
                // Basic validation (already done in operation.isValid(), but double-check)
                if (!operation.isValid()) {
                    return ValidationResult.failure("Operation failed basic validation: " + operation.getDescription());
                }
                
                // Type-specific validation
                switch (operation.getOperationType()) {
                    case CREATE:
                        return validateCreateOperation((CreateMemoryOperation) operation);
                    case UPDATE:
                        return validateUpdateOperation((UpdateMemoryOperation) operation);
                    case REPLACE:
                        return validateReplaceOperation((ReplaceMemoryOperation) operation);
                    case DELETE:
                        return validateDeleteOperation((DeleteMemoryOperation) operation);
                    case MERGE:
                        return validateMergeOperation((MergeMemoryOperation) operation);
                    default:
                        return ValidationResult.failure("Unknown operation type: " + operation.getOperationType());
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error validating operation", e);
                return ValidationResult.failure("Validation error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Validate CREATE operation
     */
    private ValidationResult validateCreateOperation(CreateMemoryOperation operation) {
        List<String> warnings = new ArrayList<>();
        
        // Check for duplicate content
        try {
            float[] embedding = embeddingService.generateEmbedding(operation.getContent()).get();
            List<Memory> similarMemories = memoryStore.findSimilarMemories(
                EmbeddingService.floatArrayToByteArray(embedding), 5);
            
            if (!similarMemories.isEmpty()) {
                warnings.add("Similar memories already exist. Consider updating existing memory instead.");
                
                // If very similar memory exists, suggest merge instead
                for (Memory similar : similarMemories) {
                    float similarity = calculateSimilarity(operation.getContent(), similar.getContent());
                    if (similarity > 0.9f) {
                        return ValidationResult.failure(
                            "Very similar memory already exists (ID: " + similar.getId() + 
                            "). Consider updating or merging instead of creating new memory.");
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not check for similar memories", e);
            warnings.add("Could not verify uniqueness due to search error.");
        }
        
        // Check importance threshold
        float minImportance = settingsManager.getMemoryImportanceThreshold();
        if (operation.getImportance() < minImportance) {
            return ValidationResult.failure(
                "Memory importance (" + operation.getImportance() + 
                ") below threshold (" + minImportance + ")");
        }
        
        // Validate content quality
        ValidationResult contentValidation = validateContentQuality(operation.getContent());
        if (!contentValidation.isSuccess()) {
            return contentValidation;
        }
        
        return ValidationResult.success("CREATE operation validated", warnings);
    }
    
    /**
     * Validate UPDATE operation
     */
    private ValidationResult validateUpdateOperation(UpdateMemoryOperation operation) {
        List<String> warnings = new ArrayList<>();
        
        // Get existing memory
        Memory existingMemory;
        try {
            existingMemory = memoryStore.getMemory(operation.getMemoryId());
            if (existingMemory == null) {
                return ValidationResult.failure("Memory not found: " + operation.getMemoryId());
            }
        } catch (Exception e) {
            return ValidationResult.failure("Could not retrieve existing memory: " + e.getMessage());
        }
        
        // Validate content update if provided
        if (operation.hasContentUpdate()) {
            ValidationResult contentValidation = validateContentUpdate(
                existingMemory, operation.getNewContent());
            if (!contentValidation.isSuccess()) {
                return contentValidation;
            }
            warnings.addAll(contentValidation.getWarnings());
        }
        
        // Validate importance update if provided
        if (operation.hasImportanceUpdate()) {
            ValidationResult importanceValidation = validateImportanceUpdate(
                existingMemory, operation.getNewImportance());
            if (!importanceValidation.isSuccess()) {
                return importanceValidation;
            }
            warnings.addAll(importanceValidation.getWarnings());
        }
        
        // Validate type change if provided
        if (operation.hasTypeUpdate()) {
            ValidationResult typeValidation = validateTypeUpdate(
                existingMemory, operation.getNewType());
            if (!typeValidation.isSuccess()) {
                return typeValidation;
            }
            warnings.addAll(typeValidation.getWarnings());
        }
        
        return ValidationResult.success("UPDATE operation validated", warnings);
    }
    
    /**
     * Validate REPLACE operation
     */
    private ValidationResult validateReplaceOperation(ReplaceMemoryOperation operation) {
        List<String> warnings = new ArrayList<>();
        
        // Get existing memory
        Memory existingMemory;
        try {
            existingMemory = memoryStore.getMemory(operation.getMemoryId());
            if (existingMemory == null) {
                return ValidationResult.failure("Memory not found: " + operation.getMemoryId());
            }
        } catch (Exception e) {
            return ValidationResult.failure("Could not retrieve existing memory: " + e.getMessage());
        }
        
        // High importance memories need extra protection
        if (existingMemory.getImportance() >= HIGH_IMPORTANCE_THRESHOLD) {
            if (operation.getConfidence() < 0.9f) {
                return ValidationResult.failure(
                    "High importance memory requires confidence ≥0.9 for replacement. " +
                    "Current confidence: " + operation.getConfidence());
            }
            warnings.add("Replacing high importance memory - ensure this is necessary.");
        }
        
        // Check if replacement is semantically related
        float similarity = calculateSimilarity(existingMemory.getContent(), operation.getNewContent());
        if (similarity < MIN_SEMANTIC_SIMILARITY) {
            warnings.add("Replacement content has low semantic similarity to original. " +
                        "Consider creating a new memory instead.");
        }
        
        // Validate new content quality
        ValidationResult contentValidation = validateContentQuality(operation.getNewContent());
        if (!contentValidation.isSuccess()) {
            return contentValidation;
        }
        
        return ValidationResult.success("REPLACE operation validated", warnings);
    }
    
    /**
     * Validate DELETE operation
     */
    private ValidationResult validateDeleteOperation(DeleteMemoryOperation operation) {
        List<String> warnings = new ArrayList<>();
        
        // Get existing memory
        Memory existingMemory;
        try {
            existingMemory = memoryStore.getMemory(operation.getMemoryId());
            if (existingMemory == null) {
                return ValidationResult.failure("Memory not found: " + operation.getMemoryId());
            }
        } catch (Exception e) {
            return ValidationResult.failure("Could not retrieve existing memory: " + e.getMessage());
        }
        
        // High importance memories need extra protection
        if (existingMemory.getImportance() >= HIGH_IMPORTANCE_THRESHOLD) {
            if (operation.getConfidence() < 0.95f) {
                return ValidationResult.failure(
                    "High importance memory requires confidence ≥0.95 for deletion. " +
                    "Current confidence: " + operation.getConfidence());
            }
            warnings.add("Deleting high importance memory - ensure this is necessary.");
        }
        
        // Frequently accessed memories need extra protection
        if (existingMemory.getAccessCount() > 10) {
            warnings.add("Deleting frequently accessed memory (access count: " + 
                        existingMemory.getAccessCount() + ")");
        }
        
        // Check for relationships that would be affected
        try {
            int relationshipCount = memoryStore.getRelationshipsFor(operation.getMemoryId()).size();
            if (relationshipCount > 0) {
                if (operation.shouldDeleteRelationships()) {
                    warnings.add("Will delete " + relationshipCount + " memory relationships.");
                } else {
                    warnings.add("Memory has " + relationshipCount + " relationships that will be orphaned.");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not check relationships", e);
            warnings.add("Could not verify relationship impact.");
        }
        
        return ValidationResult.success("DELETE operation validated", warnings);
    }
    
    /**
     * Validate MERGE operation
     */
    private ValidationResult validateMergeOperation(MergeMemoryOperation operation) {
        List<String> warnings = new ArrayList<>();
        
        // Get all source memories
        List<Memory> sourceMemories = new ArrayList<>();
        for (String memoryId : operation.getSourceMemoryIds()) {
            try {
                Memory memory = memoryStore.getMemory(memoryId);
                if (memory == null) {
                    return ValidationResult.failure("Source memory not found: " + memoryId);
                }
                sourceMemories.add(memory);
            } catch (Exception e) {
                return ValidationResult.failure("Could not retrieve source memory " + memoryId + ": " + e.getMessage());
            }
        }
        
        // Validate that memories are actually similar enough to merge
        float avgSimilarity = calculateAverageSimilarity(sourceMemories);
        if (avgSimilarity < 0.5f) {
            return ValidationResult.failure(
                "Source memories have low average similarity (" + avgSimilarity + 
                "). Merging may lose important distinctions.");
        }
        
        // Check if any source memories are high importance
        boolean hasHighImportance = sourceMemories.stream()
            .anyMatch(m -> m.getImportance() >= HIGH_IMPORTANCE_THRESHOLD);
        
        if (hasHighImportance && operation.getConfidence() < 0.8f) {
            return ValidationResult.failure(
                "Merging high importance memories requires confidence ≥0.8. " +
                "Current confidence: " + operation.getConfidence());
        }
        
        // Validate merged content quality
        ValidationResult contentValidation = validateContentQuality(operation.getMergedContent());
        if (!contentValidation.isSuccess()) {
            return contentValidation;
        }
        
        // Check that merged content preserves important information
        ValidationResult preservationValidation = validateInformationPreservation(
            sourceMemories, operation.getMergedContent());
        if (!preservationValidation.isSuccess()) {
            return preservationValidation;
        }
        warnings.addAll(preservationValidation.getWarnings());
        
        return ValidationResult.success("MERGE operation validated", warnings);
    }
    
    /**
     * Validate content quality (common checks for all content)
     */
    private ValidationResult validateContentQuality(String content) {
        List<String> warnings = new ArrayList<>();
        
        // Check for empty or very short content
        if (content.trim().length() < 5) {
            return ValidationResult.failure("Content too short to be meaningful");
        }
        
        // Check for very long content that might be better split
        if (content.length() > 1000) {
            warnings.add("Content is quite long - consider splitting into multiple memories");
        }
        
        // Check for potentially sensitive information (basic patterns)
        if (containsSensitiveInfo(content)) {
            warnings.add("Content may contain sensitive information - review carefully");
        }
        
        return ValidationResult.success("Content quality validated", warnings);
    }
    
    /**
     * Validate content update for semantic consistency
     */
    private ValidationResult validateContentUpdate(Memory existingMemory, String newContent) {
        List<String> warnings = new ArrayList<>();
        
        // Check semantic similarity
        float similarity = calculateSimilarity(existingMemory.getContent(), newContent);
        
        if (similarity < CONTRADICTION_THRESHOLD) {
            return ValidationResult.failure(
                "New content contradicts existing memory (similarity: " + similarity + 
                "). Consider REPLACE operation instead.");
        }
        
        if (similarity < MIN_SEMANTIC_SIMILARITY) {
            warnings.add("New content has low semantic similarity to existing memory");
        }
        
        // Check content length change
        int oldLength = existingMemory.getContent().length();
        int newLength = newContent.length();
        
        if (newLength > oldLength * MAX_CONTENT_CHANGE_RATIO) {
            warnings.add("New content is significantly longer than original - consider creating separate memory");
        }
        
        return ValidationResult.success("Content update validated", warnings);
    }
    
    /**
     * Validate importance update
     */
    private ValidationResult validateImportanceUpdate(Memory existingMemory, float newImportance) {
        List<String> warnings = new ArrayList<>();
        
        float oldImportance = existingMemory.getImportance();
        float change = Math.abs(newImportance - oldImportance);
        
        // Large importance changes should be justified
        if (change > 0.3f) {
            warnings.add("Large importance change: " + oldImportance + " → " + newImportance);
        }
        
        // Decreasing importance of frequently accessed memories
        if (newImportance < oldImportance && existingMemory.getAccessCount() > 5) {
            warnings.add("Decreasing importance of frequently accessed memory");
        }
        
        return ValidationResult.success("Importance update validated", warnings);
    }
    
    /**
     * Validate type update
     */
    private ValidationResult validateTypeUpdate(Memory existingMemory, MemoryType newType) {
        List<String> warnings = new ArrayList<>();
        
        MemoryType oldType = existingMemory.getType();
        
        // Some type changes are more significant than others
        if (isSignificantTypeChange(oldType, newType)) {
            warnings.add("Significant type change: " + oldType + " → " + newType);
        }
        
        return ValidationResult.success("Type update validated", warnings);
    }
    
    /**
     * Validate that merged content preserves important information from source memories
     */
    private ValidationResult validateInformationPreservation(List<Memory> sourceMemories, String mergedContent) {
        List<String> warnings = new ArrayList<>();
        
        // Check that merged content has reasonable similarity to each source
        for (Memory source : sourceMemories) {
            float similarity = calculateSimilarity(source.getContent(), mergedContent);
            if (similarity < 0.3f) {
                warnings.add("Merged content has low similarity to source memory: " + source.getId());
            }
        }
        
        // Check that merged content isn't just a simple concatenation
        int totalSourceLength = sourceMemories.stream()
            .mapToInt(m -> m.getContent().length())
            .sum();
        
        if (mergedContent.length() > totalSourceLength * 0.8f) {
            warnings.add("Merged content may be too verbose - consider more concise summary");
        }
        
        return ValidationResult.success("Information preservation validated", warnings);
    }
    
    /**
     * Calculate semantic similarity between two text strings
     */
    private float calculateSimilarity(String text1, String text2) {
        try {
            // Use embedding service to calculate similarity
            float[] embedding1 = embeddingService.generateEmbedding(text1).get();
            float[] embedding2 = embeddingService.generateEmbedding(text2).get();
            
            return EmbeddingService.calculateCosineSimilarity(embedding1, embedding2);
        } catch (Exception e) {
            Log.w(TAG, "Could not calculate semantic similarity, using text similarity", e);
            // Fallback to simple text similarity
            return calculateTextSimilarity(text1, text2);
        }
    }
    
    /**
     * Calculate average similarity between a list of memories
     */
    private float calculateAverageSimilarity(List<Memory> memories) {
        if (memories.size() < 2) return 1.0f;
        
        float totalSimilarity = 0.0f;
        int comparisons = 0;
        
        for (int i = 0; i < memories.size(); i++) {
            for (int j = i + 1; j < memories.size(); j++) {
                totalSimilarity += calculateSimilarity(
                    memories.get(i).getContent(),
                    memories.get(j).getContent()
                );
                comparisons++;
            }
        }
        
        return comparisons > 0 ? totalSimilarity / comparisons : 1.0f;
    }
    
    /**
     * Simple text similarity fallback (Jaccard similarity)
     */
    private float calculateTextSimilarity(String text1, String text2) {
        String[] words1 = text1.toLowerCase().split("\\s+");
        String[] words2 = text2.toLowerCase().split("\\s+");
        
        java.util.Set<String> set1 = new java.util.HashSet<>(java.util.Arrays.asList(words1));
        java.util.Set<String> set2 = new java.util.HashSet<>(java.util.Arrays.asList(words2));
        
        java.util.Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);
        
        java.util.Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0f : (float) intersection.size() / union.size();
    }
    
    /**
     * Check if content contains potentially sensitive information
     */
    private boolean containsSensitiveInfo(String content) {
        String lower = content.toLowerCase();
        
        // Basic patterns for sensitive information
        return lower.contains("password") ||
               lower.contains("ssn") ||
               lower.contains("social security") ||
               lower.contains("credit card") ||
               lower.matches(".*\\b\\d{3}-\\d{2}-\\d{4}\\b.*") || // SSN pattern
               lower.matches(".*\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b.*"); // Credit card pattern
    }
    
    /**
     * Check if a type change is significant
     */
    private boolean isSignificantTypeChange(MemoryType oldType, MemoryType newType) {
        // Some type changes are more significant than others
        if (oldType == MemoryType.FACT && newType == MemoryType.PREFERENCE) return true;
        if (oldType == MemoryType.PREFERENCE && newType == MemoryType.FACT) return true;
        if (oldType == MemoryType.EVENT && newType != MemoryType.EVENT) return true;
        if (newType == MemoryType.EVENT && oldType != MemoryType.EVENT) return true;
        
        return false;
    }
}

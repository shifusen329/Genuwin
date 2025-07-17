package com.genuwin.app.memory.validation;

import android.content.Context;
import android.util.Log;

import com.genuwin.app.api.services.EmbeddingService;
import com.genuwin.app.memory.models.Memory;
import com.genuwin.app.memory.models.MemoryType;
import com.genuwin.app.memory.models.MemoryRelationship;
import com.genuwin.app.memory.models.RelationshipType;
import com.genuwin.app.memory.operations.*;
import com.genuwin.app.memory.storage.MemoryStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Memory Conflict Resolver - Detects and resolves conflicts between memory operations
 * 
 * This class handles situations where new information contradicts existing memories,
 * helping to maintain consistency and accuracy in the memory system while preserving
 * important historical context when appropriate.
 */
public class MemoryConflictResolver {
    private static final String TAG = "MemoryConflictResolver";
    
    private final Context context;
    private final MemoryStore memoryStore;
    private final EmbeddingService embeddingService;
    private final ExecutorService executorService;

    // Conflict detection thresholds
    private static final float CONTRADICTION_THRESHOLD = 0.2f; // Below this similarity = contradiction
    private static final float REFINEMENT_THRESHOLD = 0.6f; // Above this = refinement, not contradiction
    private static final float HIGH_CONFIDENCE_THRESHOLD = 0.8f; // High confidence operations can override
    
    public MemoryConflictResolver(Context context, MemoryStore memoryStore, EmbeddingService embeddingService) {
        this.context = context;
        this.memoryStore = memoryStore;
        this.embeddingService = embeddingService;
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Detect conflicts for a memory operation
     * @param operation The operation to check for conflicts
     * @return ConflictResolution with detected conflicts and suggested resolutions
     */
    public CompletableFuture<ConflictResolution> detectConflicts(MemoryOperation operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Detecting conflicts for operation: " + operation.getDescription());
                
                switch (operation.getOperationType()) {
                    case CREATE:
                        return detectCreateConflicts((CreateMemoryOperation) operation).join();
                    case UPDATE:
                        return detectUpdateConflicts((UpdateMemoryOperation) operation);
                    case REPLACE:
                        return detectReplaceConflicts((ReplaceMemoryOperation) operation);
                    case DELETE:
                        return detectDeleteConflicts((DeleteMemoryOperation) operation);
                    case MERGE:
                        return detectMergeConflicts((MergeMemoryOperation) operation);
                    default:
                        return ConflictResolution.noConflicts("Unknown operation type");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error detecting conflicts", e);
                return ConflictResolution.error("Conflict detection failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Detect conflicts for CREATE operations
     */
    private CompletableFuture<ConflictResolution> detectCreateConflicts(CreateMemoryOperation operation) {
        return CompletableFuture.supplyAsync(() -> {
            List<ConflictInfo> conflicts = new ArrayList<>();
            try {
                // Search for semantically similar memories
                float[] embedding = embeddingService.generateEmbedding(operation.getContent()).get();
                List<Memory> similarMemories = memoryStore.findSimilarMemories(
                    EmbeddingService.floatArrayToByteArray(embedding), 10);
                
                for (Memory existing : similarMemories) {
                    float similarity = calculateSimilarity(operation.getContent(), existing.getContent()).join();
                    
                    // Check for contradictions
                    if (similarity < CONTRADICTION_THRESHOLD && 
                        operation.getType() == existing.getType()) {
                        
                        ConflictInfo conflict = new ConflictInfo(
                            ConflictType.CONTRADICTION,
                            existing,
                            "New memory contradicts existing memory",
                            similarity
                        );
                        
                        // Suggest resolution based on confidence and importance
                        if (operation.getConfidence() > HIGH_CONFIDENCE_THRESHOLD && 
                            operation.getImportance() > existing.getImportance()) {
                            conflict.addSuggestedResolution(
                                ResolutionStrategy.REPLACE_EXISTING,
                                "High confidence new information should replace less important existing memory"
                            );
                        } else {
                            conflict.addSuggestedResolution(
                                ResolutionStrategy.CREATE_ALTERNATIVE,
                                "Create alternative memory to preserve both perspectives"
                            );
                        }
                        
                        conflicts.add(conflict);
                    }
                    
                    // Check for near-duplicates
                    else if (similarity > 0.8f) {
                        ConflictInfo conflict = new ConflictInfo(
                            ConflictType.DUPLICATE,
                            existing,
                            "Very similar memory already exists",
                            similarity
                        );
                        
                        conflict.addSuggestedResolution(
                            ResolutionStrategy.MERGE_MEMORIES,
                            "Merge with existing similar memory"
                        );
                        conflict.addSuggestedResolution(
                            ResolutionStrategy.UPDATE_EXISTING,
                            "Update existing memory with new details"
                        );
                        
                        conflicts.add(conflict);
                    }
                }
                
            } catch (Exception e) {
                Log.w(TAG, "Could not search for similar memories", e);
                return ConflictResolution.error("Could not check for conflicts: " + e.getMessage());
            }
            
            return conflicts.isEmpty() ? 
                ConflictResolution.noConflicts("No conflicts detected for CREATE operation") :
                ConflictResolution.withConflicts("Conflicts detected for CREATE operation", conflicts);
        }, executorService);
    }
    
    /**
     * Detect conflicts for UPDATE operations
     */
    private ConflictResolution detectUpdateConflicts(UpdateMemoryOperation operation) {
        List<ConflictInfo> conflicts = new ArrayList<>();
        
        try {
            // Get the existing memory
            Memory existingMemory = memoryStore.getMemory(operation.getMemoryId());
            if (existingMemory == null) {
                return ConflictResolution.error("Memory not found: " + operation.getMemoryId());
            }
            
            // Check content update conflicts
            if (operation.hasContentUpdate()) {
                float similarity = calculateSimilarity(
                    existingMemory.getContent(), operation.getNewContent()).join();
                
                if (similarity < CONTRADICTION_THRESHOLD) {
                    ConflictInfo conflict = new ConflictInfo(
                        ConflictType.CONTRADICTION,
                        existingMemory,
                        "Update content contradicts existing memory",
                        similarity
                    );
                    
                    conflict.addSuggestedResolution(
                        ResolutionStrategy.REPLACE_EXISTING,
                        "Use REPLACE operation instead of UPDATE for contradictory content"
                    );
                    conflict.addSuggestedResolution(
                        ResolutionStrategy.CREATE_ALTERNATIVE,
                        "Create new memory for contradictory information"
                    );
                    
                    conflicts.add(conflict);
                }
                
                // Check if update would conflict with related memories
                List<Memory> relatedMemories = new ArrayList<>();
                List<MemoryRelationship> relationships = memoryStore.getRelationshipsFor(existingMemory.getId());
                for (MemoryRelationship relationship : relationships) {
                    relatedMemories.add(memoryStore.getMemory(relationship.getToMemoryId()));
                }
                for (Memory related : relatedMemories) {
                    float relatedSimilarity = calculateSimilarity(
                        operation.getNewContent(), related.getContent()).join();
                    
                    if (relatedSimilarity < CONTRADICTION_THRESHOLD) {
                        ConflictInfo conflict = new ConflictInfo(
                            ConflictType.RELATED_CONTRADICTION,
                            related,
                            "Update would contradict related memory",
                            relatedSimilarity
                        );
                        
                        conflict.addSuggestedResolution(
                            ResolutionStrategy.UPDATE_RELATIONSHIPS,
                            "Update memory relationships to reflect new information"
                        );
                        
                        conflicts.add(conflict);
                    }
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Could not check update conflicts", e);
            return ConflictResolution.error("Could not check for conflicts: " + e.getMessage());
        }
        
        return conflicts.isEmpty() ? 
            ConflictResolution.noConflicts("No conflicts detected for UPDATE operation") :
            ConflictResolution.withConflicts("Conflicts detected for UPDATE operation", conflicts);
    }
    
    /**
     * Detect conflicts for REPLACE operations
     */
    private ConflictResolution detectReplaceConflicts(ReplaceMemoryOperation operation) {
        List<ConflictInfo> conflicts = new ArrayList<>();
        
        try {
            // Get the existing memory
            Memory existingMemory = memoryStore.getMemory(operation.getMemoryId());
            if (existingMemory == null) {
                return ConflictResolution.error("Memory not found: " + operation.getMemoryId());
            }
            
            // Check if replacement would affect related memories
            List<Memory> relatedMemories = new ArrayList<>();
            List<MemoryRelationship> relationships = memoryStore.getRelationshipsFor(existingMemory.getId());
            for (MemoryRelationship relationship : relationships) {
                relatedMemories.add(memoryStore.getMemory(relationship.getToMemoryId()));
            }
            for (Memory related : relatedMemories) {
                float similarity = calculateSimilarity(
                    operation.getNewContent(), related.getContent()).join();
                
                if (similarity < CONTRADICTION_THRESHOLD) {
                    ConflictInfo conflict = new ConflictInfo(
                        ConflictType.RELATED_CONTRADICTION,
                        related,
                        "Replacement would contradict related memory",
                        similarity
                    );
                    
                    conflict.addSuggestedResolution(
                        ResolutionStrategy.UPDATE_RELATIONSHIPS,
                        "Update or remove relationships that no longer apply"
                    );
                    conflict.addSuggestedResolution(
                        ResolutionStrategy.CREATE_ALTERNATIVE,
                        "Create new memory instead of replacing to preserve relationships"
                    );
                    
                    conflicts.add(conflict);
                }
            }
            
            // Check if replacement loses important information
            if (existingMemory.getImportance() > operation.getNewImportance() + 0.2f) {
                ConflictInfo conflict = new ConflictInfo(
                    ConflictType.INFORMATION_LOSS,
                    existingMemory,
                    "Replacement has significantly lower importance",
                    0.0f // Not similarity-based
                );
                
                conflict.addSuggestedResolution(
                    ResolutionStrategy.PRESERVE_ORIGINAL,
                    "Keep original memory and create new one for additional information"
                );
                
                conflicts.add(conflict);
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Could not check replace conflicts", e);
            return ConflictResolution.error("Could not check for conflicts: " + e.getMessage());
        }
        
        return conflicts.isEmpty() ? 
            ConflictResolution.noConflicts("No conflicts detected for REPLACE operation") :
            ConflictResolution.withConflicts("Conflicts detected for REPLACE operation", conflicts);
    }
    
    /**
     * Detect conflicts for DELETE operations
     */
    private ConflictResolution detectDeleteConflicts(DeleteMemoryOperation operation) {
        List<ConflictInfo> conflicts = new ArrayList<>();
        
        try {
            // Get the existing memory
            Memory existingMemory = memoryStore.getMemory(operation.getMemoryId());
            if (existingMemory == null) {
                return ConflictResolution.error("Memory not found: " + operation.getMemoryId());
            }
            
            // Check for high importance memory deletion
            if (existingMemory.getImportance() > 0.7f) {
                ConflictInfo conflict = new ConflictInfo(
                    ConflictType.INFORMATION_LOSS,
                    existingMemory,
                    "Deleting high importance memory",
                    0.0f
                );
                
                conflict.addSuggestedResolution(
                    ResolutionStrategy.PRESERVE_ORIGINAL,
                    "Consider reducing importance instead of deleting"
                );
                
                conflicts.add(conflict);
            }
            
            // Check for memories that depend on this one
            List<Memory> dependentMemories = new ArrayList<>();
            List<MemoryRelationship> relationships = memoryStore.getRelationshipsByType(existingMemory.getId(), RelationshipType.BUILDS_ON);
            for (MemoryRelationship relationship : relationships) {
                dependentMemories.add(memoryStore.getMemory(relationship.getToMemoryId()));
            }
            if (!dependentMemories.isEmpty()) {
                ConflictInfo conflict = new ConflictInfo(
                    ConflictType.DEPENDENCY_BREAK,
                    existingMemory,
                    "Other memories depend on this memory",
                    0.0f
                );
                
                conflict.addSuggestedResolution(
                    ResolutionStrategy.UPDATE_RELATIONSHIPS,
                    "Update dependent memories before deletion"
                );
                conflict.addSuggestedResolution(
                    ResolutionStrategy.PRESERVE_ORIGINAL,
                    "Keep memory but mark as deprecated"
                );
                
                conflicts.add(conflict);
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Could not check delete conflicts", e);
            return ConflictResolution.error("Could not check for conflicts: " + e.getMessage());
        }
        
        return conflicts.isEmpty() ? 
            ConflictResolution.noConflicts("No conflicts detected for DELETE operation") :
            ConflictResolution.withConflicts("Conflicts detected for DELETE operation", conflicts);
    }
    
    /**
     * Detect conflicts for MERGE operations
     */
    private ConflictResolution detectMergeConflicts(MergeMemoryOperation operation) {
        List<ConflictInfo> conflicts = new ArrayList<>();
        
        try {
            // Get all source memories
            List<Memory> sourceMemories = new ArrayList<>();
            for (String memoryId : operation.getSourceMemoryIds()) {
                Memory memory = memoryStore.getMemory(memoryId);
                if (memory != null) {
                    sourceMemories.add(memory);
                }
            }
            
            // Check for contradictions between source memories
            for (int i = 0; i < sourceMemories.size(); i++) {
                for (int j = i + 1; j < sourceMemories.size(); j++) {
                    Memory mem1 = sourceMemories.get(i);
                    Memory mem2 = sourceMemories.get(j);
                    
                    float similarity = calculateSimilarity(mem1.getContent(), mem2.getContent()).join();
                    
                    if (similarity < CONTRADICTION_THRESHOLD) {
                        ConflictInfo conflict = new ConflictInfo(
                            ConflictType.CONTRADICTION,
                            mem1,
                            "Source memories contradict each other",
                            similarity
                        );
                        
                        conflict.addSuggestedResolution(
                            ResolutionStrategy.SELECTIVE_MERGE,
                            "Merge only non-contradictory memories"
                        );
                        conflict.addSuggestedResolution(
                            ResolutionStrategy.CREATE_ALTERNATIVE,
                            "Create separate memories for contradictory information"
                        );
                        
                        conflicts.add(conflict);
                    }
                }
            }
            
            // Check if merged content loses important information
            for (Memory source : sourceMemories) {
                float preservationScore = calculateSimilarity(
                    source.getContent(), operation.getMergedContent()).join();
                
                if (preservationScore < 0.3f && source.getImportance() > 0.5f) {
                    ConflictInfo conflict = new ConflictInfo(
                        ConflictType.INFORMATION_LOSS,
                        source,
                        "Merged content doesn't preserve important source information",
                        preservationScore
                    );
                    
                    conflict.addSuggestedResolution(
                        ResolutionStrategy.IMPROVE_MERGE,
                        "Revise merged content to better preserve source information"
                    );
                    
                    conflicts.add(conflict);
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Could not check merge conflicts", e);
            return ConflictResolution.error("Could not check for conflicts: " + e.getMessage());
        }
        
        return conflicts.isEmpty() ? 
            ConflictResolution.noConflicts("No conflicts detected for MERGE operation") :
            ConflictResolution.withConflicts("Conflicts detected for MERGE operation", conflicts);
    }
    
    /**
     * Get memories related to the given memory
     */
    private List<Memory> getRelatedMemories(Memory memory) {
        try {
            List<Memory> relatedMemories = new ArrayList<>();
            List<MemoryRelationship> relationships = memoryStore.getRelationshipsFor(memory.getId());
            for (MemoryRelationship relationship : relationships) {
                relatedMemories.add(memoryStore.getMemory(relationship.getToMemoryId()));
            }
            return relatedMemories;
        } catch (Exception e) {
            Log.w(TAG, "Could not get related memories", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get memories that depend on the given memory
     */
    private List<Memory> getDependentMemories(Memory memory) {
        try {
            List<Memory> dependentMemories = new ArrayList<>();
            List<MemoryRelationship> relationships = memoryStore.getRelationshipsByType(memory.getId(), RelationshipType.BUILDS_ON);
            for (MemoryRelationship relationship : relationships) {
                dependentMemories.add(memoryStore.getMemory(relationship.getToMemoryId()));
            }
            return dependentMemories;
        } catch (Exception e) {
            Log.w(TAG, "Could not get dependent memories", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Calculate semantic similarity between two text strings
     */
    private CompletableFuture<Float> calculateSimilarity(String text1, String text2) {
        return embeddingService.generateEmbedding(text1).thenCombine(embeddingService.generateEmbedding(text2), (embedding1, embedding2) -> {
            try {
                return EmbeddingService.calculateCosineSimilarity(embedding1, embedding2);
            } catch (Exception e) {
                Log.w(TAG, "Could not calculate semantic similarity, using text similarity", e);
                return calculateTextSimilarity(text1, text2);
            }
        });
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
     * Types of conflicts that can occur
     */
    public enum ConflictType {
        CONTRADICTION,          // Direct contradiction between memories
        DUPLICATE,             // Near-duplicate content
        RELATED_CONTRADICTION, // Contradiction with related memories
        INFORMATION_LOSS,      // Operation would lose important information
        DEPENDENCY_BREAK       // Operation would break memory dependencies
    }
    
    /**
     * Strategies for resolving conflicts
     */
    public enum ResolutionStrategy {
        REPLACE_EXISTING,      // Replace the existing conflicting memory
        UPDATE_EXISTING,       // Update the existing memory instead
        CREATE_ALTERNATIVE,    // Create alternative memory for different perspective
        MERGE_MEMORIES,        // Merge conflicting memories
        PRESERVE_ORIGINAL,     // Keep original and modify operation
        UPDATE_RELATIONSHIPS,  // Update memory relationships
        SELECTIVE_MERGE,       // Merge only non-conflicting parts
        IMPROVE_MERGE          // Improve merge content to preserve information
    }
}

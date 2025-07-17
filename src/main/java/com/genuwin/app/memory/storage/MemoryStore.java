package com.genuwin.app.memory.storage;

import com.genuwin.app.memory.models.Memory;
import com.genuwin.app.memory.models.MemoryType;
import com.genuwin.app.memory.models.MemoryRelationship;
import com.genuwin.app.memory.models.RelationshipType;

import java.util.List;

/**
 * Interface for memory storage operations
 * Provides abstraction layer for different storage implementations
 */
public interface MemoryStore {
    
    // ========== Memory Operations ==========
    
    /**
     * Save a memory to storage
     */
    void saveMemory(Memory memory);
    
    /**
     * Save multiple memories to storage
     */
    void saveMemories(List<Memory> memories);
    
    /**
     * Get a memory by ID
     */
    Memory getMemory(String memoryId);
    
    /**
     * Get all memories
     */
    List<Memory> getAllMemories();
    
    /**
     * Update an existing memory
     */
    void updateMemory(Memory memory);
    
    /**
     * Update multiple memories
     */
    void updateMemories(List<Memory> memories);
    
    /**
     * Delete a memory
     */
    void deleteMemory(String memoryId);
    
    /**
     * Delete multiple memories
     */
    void deleteMemories(List<String> memoryIds);
    
    // ========== Memory Search & Retrieval ==========
    
    /**
     * Get memories by type
     */
    List<Memory> getMemoriesByType(MemoryType type, int limit);
    
    /**
     * Get memories by importance threshold
     */
    List<Memory> getMemoriesByImportance(float minImportance, int limit);
    
    /**
     * Get recent memories
     */
    List<Memory> getRecentMemories(int limit);
    
    /**
     * Get frequently accessed memories
     */
    List<Memory> getFrequentMemories(int limit);
    
    /**
     * Get memories with high overall score
     */
    List<Memory> getTopScoredMemories(int limit);
    
    /**
     * Search memories by content
     */
    List<Memory> searchMemoriesByContent(String searchText, int limit);
    
    /**
     * Get memories within a time range
     */
    List<Memory> getMemoriesInTimeRange(long startTime, long endTime);
    
    /**
     * Get memories with emotional weight above threshold
     */
    List<Memory> getEmotionalMemories(float minWeight, int limit);
    
    /**
     * Get memories with embeddings for similarity search
     */
    List<Memory> getMemoriesWithEmbeddings();
    
    /**
     * Find similar memories using vector similarity
     * This method should be implemented to use embedding similarity
     */
    List<Memory> findSimilarMemories(byte[] queryEmbedding, int limit);
    
    // ========== Memory Relationship Operations ==========
    
    /**
     * Save a memory relationship
     */
    void saveRelationship(MemoryRelationship relationship);
    
    /**
     * Save multiple relationships
     */
    void saveRelationships(List<MemoryRelationship> relationships);
    
    /**
     * Get a relationship by ID
     */
    MemoryRelationship getRelationship(String relationshipId);
    
    /**
     * Get all relationships for a memory
     */
    List<MemoryRelationship> getRelationshipsFor(String memoryId);
    
    /**
     * Get relationships by type from a memory
     */
    List<MemoryRelationship> getRelationshipsByType(String memoryId, RelationshipType type);
    
    /**
     * Check if relationship exists between two memories
     */
    boolean relationshipExists(String fromMemoryId, String toMemoryId);
    
    /**
     * Update a relationship
     */
    void updateRelationship(MemoryRelationship relationship);
    
    /**
     * Delete a relationship
     */
    void deleteRelationship(String relationshipId);
    
    /**
     * Delete all relationships for a memory
     */
    void deleteAllRelationshipsFor(String memoryId);
    
    // ========== Memory Access Tracking ==========
    
    /**
     * Update access tracking for a memory
     */
    void updateAccessTracking(String memoryId, long accessTime);
    
    /**
     * Update overall score for a memory
     */
    void updateOverallScore(String memoryId, float score);
    
    // ========== Storage Management ==========
    
    /**
     * Get total memory count
     */
    int getTotalMemoryCount();
    
    /**
     * Get memory count by type
     */
    int getMemoryCountByType(MemoryType type);
    
    /**
     * Get total relationship count
     */
    int getTotalRelationshipCount();
    
    /**
     * Get total storage size estimate
     */
    long getTotalStorageSize();
    
    /**
     * Delete old memories beyond limit (keep most important)
     */
    void deleteOldMemories(int keepCount);
    
    /**
     * Delete memories older than timestamp
     */
    void deleteMemoriesOlderThan(long cutoffTime);
    
    /**
     * Delete weak relationships below threshold
     */
    void deleteWeakRelationships(float minStrength);
    
    /**
     * Clear all memories and relationships (for testing or reset)
     */
    void clearAllData();
    
    // ========== Lifecycle Management ==========
    
    /**
     * Initialize the storage system
     */
    void initialize();
    
    /**
     * Close and cleanup storage resources
     */
    void close();
    
    /**
     * Check if storage is initialized and ready
     */
    boolean isReady();
}

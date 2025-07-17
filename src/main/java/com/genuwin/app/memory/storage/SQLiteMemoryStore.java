package com.genuwin.app.memory.storage;

import android.util.Log;

import com.genuwin.app.api.services.EmbeddingService;
import com.genuwin.app.memory.database.MemoryDatabase;
import com.genuwin.app.memory.database.MemoryDao;
import com.genuwin.app.memory.database.MemoryRelationshipDao;
import com.genuwin.app.memory.database.VersionedMemoryDao;
import com.genuwin.app.memory.models.Memory;
import com.genuwin.app.memory.models.MemoryType;
import com.genuwin.app.memory.models.MemoryRelationship;
import com.genuwin.app.memory.models.RelationshipType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Character-specific SQLite implementation of MemoryStore using Room database
 * 
 * Each instance operates on a single character's database, providing
 * complete isolation between character memories while maintaining
 * the same interface for all memory operations.
 */
public class SQLiteMemoryStore implements MemoryStore {
    private static final String TAG = "SQLiteMemoryStore";
    
    private final MemoryDatabase database;
    private final MemoryDao memoryDao;
    private final MemoryRelationshipDao relationshipDao;
    private final VersionedMemoryDao versionedMemoryDao;
    private boolean isReady = false;
    
    /**
     * Create a SQLiteMemoryStore for a specific character database
     * 
     * @param database The character-specific MemoryDatabase instance
     */
    public SQLiteMemoryStore(MemoryDatabase database) {
        this.database = database;
        this.memoryDao = database.memoryDao();
        this.relationshipDao = database.memoryRelationshipDao();
        this.versionedMemoryDao = database.versionedMemoryDao();
        this.isReady = true;
        
        Log.d(TAG, "SQLiteMemoryStore initialized for character database");
    }
    
    @Override
    public void initialize() {
        // Database is already initialized by the factory
        Log.d(TAG, "SQLiteMemoryStore initialization complete");
    }
    
    @Override
    public boolean isReady() {
        return isReady && database != null;
    }
    
    @Override
    public void close() {
        Log.d(TAG, "SQLiteMemoryStore close requested - database managed by factory");
        // Database lifecycle is managed by CharacterMemoryDatabaseFactory
        // Individual stores don't close the database
        isReady = false;
    }
    
    // ========== Memory Operations ==========
    
    @Override
    public void saveMemory(Memory memory) {
        checkReady();
        try {
            memoryDao.insertMemory(memory);
            Log.d(TAG, "Memory saved: " + memory.getId());
        } catch (Exception e) {
            Log.e(TAG, "Failed to save memory: " + memory.getId(), e);
            throw new RuntimeException("Failed to save memory", e);
        }
    }
    
    @Override
    public void saveMemories(List<Memory> memories) {
        checkReady();
        try {
            memoryDao.insertMemories(memories);
            Log.d(TAG, "Saved " + memories.size() + " memories");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save memories", e);
            throw new RuntimeException("Failed to save memories", e);
        }
    }
    
    @Override
    public Memory getMemory(String memoryId) {
        checkReady();
        try {
            Memory memory = memoryDao.getMemoryById(memoryId);
            if (memory != null) {
                // Update access tracking
                updateAccessTracking(memoryId, System.currentTimeMillis());
            }
            return memory;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get memory: " + memoryId, e);
            return null;
        }
    }
    
    @Override
    public List<Memory> getAllMemories() {
        checkReady();
        try {
            return memoryDao.getAllMemories();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get all memories", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public void updateMemory(Memory memory) {
        checkReady();
        try {
            memoryDao.updateMemory(memory);
            Log.d(TAG, "Memory updated: " + memory.getId());
        } catch (Exception e) {
            Log.e(TAG, "Failed to update memory: " + memory.getId(), e);
            throw new RuntimeException("Failed to update memory", e);
        }
    }
    
    @Override
    public void updateMemories(List<Memory> memories) {
        checkReady();
        try {
            memoryDao.updateMemories(memories);
            Log.d(TAG, "Updated " + memories.size() + " memories");
        } catch (Exception e) {
            Log.e(TAG, "Failed to update memories", e);
            throw new RuntimeException("Failed to update memories", e);
        }
    }
    
    @Override
    public void deleteMemory(String memoryId) {
        checkReady();
        try {
            // Delete all relationships for this memory first
            deleteAllRelationshipsFor(memoryId);
            
            // Delete the memory
            memoryDao.deleteMemoryById(memoryId);
            Log.d(TAG, "Memory deleted: " + memoryId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete memory: " + memoryId, e);
            throw new RuntimeException("Failed to delete memory", e);
        }
    }
    
    @Override
    public void deleteMemories(List<String> memoryIds) {
        checkReady();
        try {
            for (String memoryId : memoryIds) {
                deleteMemory(memoryId);
            }
            Log.d(TAG, "Deleted " + memoryIds.size() + " memories");
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete memories", e);
            throw new RuntimeException("Failed to delete memories", e);
        }
    }
    
    // ========== Memory Search & Retrieval ==========
    
    @Override
    public List<Memory> getMemoriesByType(MemoryType type, int limit) {
        checkReady();
        try {
            return memoryDao.getMemoriesByType(type, limit);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get memories by type: " + type, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Memory> getMemoriesByImportance(float minImportance, int limit) {
        checkReady();
        try {
            return memoryDao.getMemoriesByImportance(minImportance, limit);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get memories by importance", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Memory> getRecentMemories(int limit) {
        checkReady();
        try {
            return memoryDao.getRecentMemories(limit);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get recent memories", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Memory> getFrequentMemories(int limit) {
        checkReady();
        try {
            return memoryDao.getFrequentMemories(limit);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get frequent memories", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Memory> getTopScoredMemories(int limit) {
        checkReady();
        try {
            return memoryDao.getTopScoredMemories(limit);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get top scored memories", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Memory> searchMemoriesByContent(String searchText, int limit) {
        checkReady();
        try {
            return memoryDao.searchMemoriesByContent(searchText, limit);
        } catch (Exception e) {
            Log.e(TAG, "Failed to search memories by content", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Memory> getMemoriesInTimeRange(long startTime, long endTime) {
        checkReady();
        try {
            return memoryDao.getMemoriesInTimeRange(startTime, endTime);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get memories in time range", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Memory> getEmotionalMemories(float minWeight, int limit) {
        checkReady();
        try {
            return memoryDao.getEmotionalMemories(minWeight, limit);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get emotional memories", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Memory> getMemoriesWithEmbeddings() {
        checkReady();
        try {
            return memoryDao.getMemoriesWithEmbeddings();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get memories with embeddings", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Memory> findSimilarMemories(byte[] queryEmbedding, int limit) {
        checkReady();
        try {
            // Get all memories with embeddings for this character
            List<Memory> memoriesWithEmbeddings = getMemoriesWithEmbeddings();
            
            if (memoriesWithEmbeddings.isEmpty() || queryEmbedding == null) {
                return new ArrayList<>();
            }
            
            // Convert query embedding to float array
            float[] queryFloats = EmbeddingService.byteArrayToFloatArray(queryEmbedding);
            if (queryFloats == null) {
                Log.w(TAG, "Failed to convert query embedding to float array");
                return new ArrayList<>();
            }
            
            // Calculate similarities and sort
            List<MemorySimilarity> similarities = new ArrayList<>();
            
            for (Memory memory : memoriesWithEmbeddings) {
                if (memory.getEmbedding() != null) {
                    float[] memoryFloats = EmbeddingService.byteArrayToFloatArray(memory.getEmbedding());
                    if (memoryFloats != null) {
                        float similarity = EmbeddingService.calculateCosineSimilarity(queryFloats, memoryFloats);
                        similarities.add(new MemorySimilarity(memory, similarity));
                    }
                }
            }
            
            // Sort by similarity (highest first) and return top results
            Collections.sort(similarities, (a, b) -> Float.compare(b.similarity, a.similarity));
            
            List<Memory> result = new ArrayList<>();
            int count = Math.min(limit, similarities.size());
            for (int i = 0; i < count; i++) {
                result.add(similarities.get(i).memory);
            }
            
            Log.d(TAG, "Found " + result.size() + " similar memories for character");
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to find similar memories", e);
            return new ArrayList<>();
        }
    }
    
    // ========== Memory Relationship Operations ==========
    
    @Override
    public void saveRelationship(MemoryRelationship relationship) {
        checkReady();
        try {
            relationshipDao.insertRelationship(relationship);
            Log.d(TAG, "Relationship saved: " + relationship.getId());
        } catch (Exception e) {
            Log.e(TAG, "Failed to save relationship: " + relationship.getId(), e);
            throw new RuntimeException("Failed to save relationship", e);
        }
    }
    
    @Override
    public void saveRelationships(List<MemoryRelationship> relationships) {
        checkReady();
        try {
            relationshipDao.insertRelationships(relationships);
            Log.d(TAG, "Saved " + relationships.size() + " relationships");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save relationships", e);
            throw new RuntimeException("Failed to save relationships", e);
        }
    }
    
    @Override
    public MemoryRelationship getRelationship(String relationshipId) {
        checkReady();
        try {
            return relationshipDao.getRelationshipById(relationshipId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get relationship: " + relationshipId, e);
            return null;
        }
    }
    
    @Override
    public List<MemoryRelationship> getRelationshipsFor(String memoryId) {
        checkReady();
        try {
            return relationshipDao.getAllRelationshipsFor(memoryId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get relationships for memory: " + memoryId, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<MemoryRelationship> getRelationshipsByType(String memoryId, RelationshipType type) {
        checkReady();
        try {
            return relationshipDao.getRelationshipsByType(memoryId, type);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get relationships by type for memory: " + memoryId, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean relationshipExists(String fromMemoryId, String toMemoryId) {
        checkReady();
        try {
            return relationshipDao.relationshipExists(fromMemoryId, toMemoryId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to check relationship existence", e);
            return false;
        }
    }
    
    @Override
    public void updateRelationship(MemoryRelationship relationship) {
        checkReady();
        try {
            relationshipDao.updateRelationship(relationship);
            Log.d(TAG, "Relationship updated: " + relationship.getId());
        } catch (Exception e) {
            Log.e(TAG, "Failed to update relationship: " + relationship.getId(), e);
            throw new RuntimeException("Failed to update relationship", e);
        }
    }
    
    @Override
    public void deleteRelationship(String relationshipId) {
        checkReady();
        try {
            relationshipDao.deleteRelationshipById(relationshipId);
            Log.d(TAG, "Relationship deleted: " + relationshipId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete relationship: " + relationshipId, e);
            throw new RuntimeException("Failed to delete relationship", e);
        }
    }
    
    @Override
    public void deleteAllRelationshipsFor(String memoryId) {
        checkReady();
        try {
            relationshipDao.deleteAllRelationshipsFor(memoryId);
            Log.d(TAG, "All relationships deleted for memory: " + memoryId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete relationships for memory: " + memoryId, e);
            throw new RuntimeException("Failed to delete relationships", e);
        }
    }
    
    // ========== Memory Access Tracking ==========
    
    @Override
    public void updateAccessTracking(String memoryId, long accessTime) {
        checkReady();
        try {
            memoryDao.updateAccessTracking(memoryId, accessTime);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update access tracking for memory: " + memoryId, e);
        }
    }
    
    @Override
    public void updateOverallScore(String memoryId, float score) {
        checkReady();
        try {
            memoryDao.updateOverallScore(memoryId, score);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update overall score for memory: " + memoryId, e);
        }
    }
    
    // ========== Storage Management ==========
    
    @Override
    public int getTotalMemoryCount() {
        checkReady();
        try {
            return memoryDao.getMemoryCount();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get total memory count", e);
            return 0;
        }
    }
    
    @Override
    public int getMemoryCountByType(MemoryType type) {
        checkReady();
        try {
            return memoryDao.getMemoryCountByType(type);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get memory count by type: " + type, e);
            return 0;
        }
    }
    
    @Override
    public int getTotalRelationshipCount() {
        checkReady();
        try {
            return relationshipDao.getRelationshipCount();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get total relationship count", e);
            return 0;
        }
    }
    
    @Override
    public long getTotalStorageSize() {
        checkReady();
        try {
            return memoryDao.getTotalContentSize();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get total storage size", e);
            return 0;
        }
    }
    
    @Override
    public void deleteOldMemories(int keepCount) {
        checkReady();
        try {
            memoryDao.deleteOldMemories(keepCount);
            Log.d(TAG, "Deleted old memories, keeping " + keepCount);
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete old memories", e);
            throw new RuntimeException("Failed to delete old memories", e);
        }
    }
    
    @Override
    public void deleteMemoriesOlderThan(long cutoffTime) {
        checkReady();
        try {
            memoryDao.deleteMemoriesOlderThan(cutoffTime);
            Log.d(TAG, "Deleted memories older than " + cutoffTime);
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete old memories", e);
            throw new RuntimeException("Failed to delete old memories", e);
        }
    }
    
    @Override
    public void deleteWeakRelationships(float minStrength) {
        checkReady();
        try {
            relationshipDao.deleteWeakRelationships(minStrength);
            Log.d(TAG, "Deleted weak relationships below " + minStrength);
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete weak relationships", e);
            throw new RuntimeException("Failed to delete weak relationships", e);
        }
    }
    
    @Override
    public void clearAllData() {
        checkReady();
        try {
            relationshipDao.clearAllRelationships();
            memoryDao.clearAllMemories();
            Log.d(TAG, "All memory data cleared for character");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear all data", e);
            throw new RuntimeException("Failed to clear all data", e);
        }
    }
    
    // ========== Helper Methods ==========
    
    private void checkReady() {
        if (!isReady()) {
            throw new IllegalStateException("SQLiteMemoryStore is not ready");
        }
    }
    
    /**
     * Helper class for similarity calculations
     */
    private static class MemorySimilarity {
        final Memory memory;
        final float similarity;
        
        MemorySimilarity(Memory memory, float similarity) {
            this.memory = memory;
            this.similarity = similarity;
        }
    }
}

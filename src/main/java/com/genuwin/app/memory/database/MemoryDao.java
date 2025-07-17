package com.genuwin.app.memory.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.genuwin.app.memory.models.Memory;
import com.genuwin.app.memory.models.MemoryType;

import java.util.List;

/**
 * Data Access Object for Memory operations
 * Provides database operations for the Memory entity
 */
@Dao
public interface MemoryDao {
    
    /**
     * Insert a new memory
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMemory(Memory memory);
    
    /**
     * Insert multiple memories
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMemories(List<Memory> memories);
    
    /**
     * Update an existing memory
     */
    @Update
    void updateMemory(Memory memory);
    
    /**
     * Update multiple memories
     */
    @Update
    void updateMemories(List<Memory> memories);
    
    /**
     * Delete a memory
     */
    @Delete
    void deleteMemory(Memory memory);
    
    /**
     * Delete memory by ID
     */
    @Query("DELETE FROM memories WHERE id = :memoryId")
    void deleteMemoryById(String memoryId);
    
    /**
     * Get a memory by ID
     */
    @Query("SELECT * FROM memories WHERE id = :memoryId")
    Memory getMemoryById(String memoryId);
    
    /**
     * Get all memories
     */
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    List<Memory> getAllMemories();
    
    /**
     * Get memories by type
     */
    @Query("SELECT * FROM memories WHERE type = :type ORDER BY timestamp DESC LIMIT :limit")
    List<Memory> getMemoriesByType(MemoryType type, int limit);
    
    /**
     * Get memories by importance threshold
     */
    @Query("SELECT * FROM memories WHERE importance >= :minImportance ORDER BY importance DESC, timestamp DESC LIMIT :limit")
    List<Memory> getMemoriesByImportance(float minImportance, int limit);
    
    /**
     * Get recent memories
     */
    @Query("SELECT * FROM memories ORDER BY timestamp DESC LIMIT :limit")
    List<Memory> getRecentMemories(int limit);
    
    /**
     * Get frequently accessed memories
     */
    @Query("SELECT * FROM memories ORDER BY accessCount DESC, lastAccessed DESC LIMIT :limit")
    List<Memory> getFrequentMemories(int limit);
    
    /**
     * Get memories with high overall score (calculated from importance, recency, and access)
     */
    @Query("SELECT * FROM memories ORDER BY importance DESC, lastAccessed DESC, accessCount DESC LIMIT :limit")
    List<Memory> getTopScoredMemories(int limit);
    
    /**
     * Search memories by content (simple text search)
     */
    @Query("SELECT * FROM memories WHERE content LIKE '%' || :searchText || '%' ORDER BY importance DESC, timestamp DESC LIMIT :limit")
    List<Memory> searchMemoriesByContent(String searchText, int limit);
    
    /**
     * Get memories within a time range
     */
    @Query("SELECT * FROM memories WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    List<Memory> getMemoriesInTimeRange(long startTime, long endTime);
    
    /**
     * Get memories with emotional weight above threshold
     */
    @Query("SELECT * FROM memories WHERE emotionalWeight >= :minWeight ORDER BY emotionalWeight DESC, timestamp DESC LIMIT :limit")
    List<Memory> getEmotionalMemories(float minWeight, int limit);
    
    /**
     * Update access tracking for a memory
     */
    @Query("UPDATE memories SET lastAccessed = :accessTime, accessCount = accessCount + 1 WHERE id = :memoryId")
    void updateAccessTracking(String memoryId, long accessTime);
    
    /**
     * Update overall score for a memory (updates importance as proxy for overall score)
     */
    @Query("UPDATE memories SET importance = :score WHERE id = :memoryId")
    void updateOverallScore(String memoryId, float score);
    
    /**
     * Get memory count
     */
    @Query("SELECT COUNT(*) FROM memories")
    int getMemoryCount();
    
    /**
     * Get memory count by type
     */
    @Query("SELECT COUNT(*) FROM memories WHERE type = :type")
    int getMemoryCountByType(MemoryType type);
    
    /**
     * Get total storage size estimate (sum of content lengths)
     */
    @Query("SELECT SUM(LENGTH(content)) FROM memories")
    long getTotalContentSize();
    
    /**
     * Delete old memories beyond limit (keep most important)
     */
    @Query("DELETE FROM memories WHERE id NOT IN (SELECT id FROM memories ORDER BY importance DESC, timestamp DESC LIMIT :keepCount)")
    void deleteOldMemories(int keepCount);
    
    /**
     * Delete memories older than timestamp
     */
    @Query("DELETE FROM memories WHERE timestamp < :cutoffTime")
    void deleteMemoriesOlderThan(long cutoffTime);
    
    /**
     * Get memories for vector similarity search
     * Returns memories with their embeddings for similarity calculation
     */
    @Query("SELECT * FROM memories WHERE embedding IS NOT NULL ORDER BY importance DESC")
    List<Memory> getMemoriesWithEmbeddings();
    
    /**
     * Clear all memories (for testing or reset)
     */
    @Query("DELETE FROM memories")
    void clearAllMemories();
}

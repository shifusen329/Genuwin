package com.genuwin.app.memory.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.genuwin.app.memory.models.MemoryRelationship;
import com.genuwin.app.memory.models.RelationshipType;

import java.util.List;

/**
 * Data Access Object for MemoryRelationship operations
 * Provides database operations for memory relationships
 */
@Dao
public interface MemoryRelationshipDao {
    
    /**
     * Insert a new relationship
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRelationship(MemoryRelationship relationship);
    
    /**
     * Insert multiple relationships
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRelationships(List<MemoryRelationship> relationships);
    
    /**
     * Update an existing relationship
     */
    @Update
    void updateRelationship(MemoryRelationship relationship);
    
    /**
     * Delete a relationship
     */
    @Delete
    void deleteRelationship(MemoryRelationship relationship);
    
    /**
     * Delete relationship by ID
     */
    @Query("DELETE FROM memory_relationships WHERE id = :relationshipId")
    void deleteRelationshipById(String relationshipId);
    
    /**
     * Get a relationship by ID
     */
    @Query("SELECT * FROM memory_relationships WHERE id = :relationshipId")
    MemoryRelationship getRelationshipById(String relationshipId);
    
    /**
     * Get all relationships for a memory (outgoing)
     */
    @Query("SELECT * FROM memory_relationships WHERE fromMemoryId = :memoryId ORDER BY strength DESC")
    List<MemoryRelationship> getRelationshipsFrom(String memoryId);
    
    /**
     * Get all relationships to a memory (incoming)
     */
    @Query("SELECT * FROM memory_relationships WHERE toMemoryId = :memoryId ORDER BY strength DESC")
    List<MemoryRelationship> getRelationshipsTo(String memoryId);
    
    /**
     * Get all relationships for a memory (both directions)
     */
    @Query("SELECT * FROM memory_relationships WHERE fromMemoryId = :memoryId OR toMemoryId = :memoryId ORDER BY strength DESC")
    List<MemoryRelationship> getAllRelationshipsFor(String memoryId);
    
    /**
     * Get relationships by type from a memory
     */
    @Query("SELECT * FROM memory_relationships WHERE fromMemoryId = :memoryId AND relationshipType = :type ORDER BY strength DESC")
    List<MemoryRelationship> getRelationshipsByType(String memoryId, RelationshipType type);
    
    /**
     * Get relationships with minimum strength
     */
    @Query("SELECT * FROM memory_relationships WHERE strength >= :minStrength ORDER BY strength DESC")
    List<MemoryRelationship> getStrongRelationships(float minStrength);
    
    /**
     * Check if relationship exists between two memories
     */
    @Query("SELECT COUNT(*) > 0 FROM memory_relationships WHERE fromMemoryId = :fromId AND toMemoryId = :toId")
    boolean relationshipExists(String fromId, String toId);
    
    /**
     * Get specific relationship between two memories
     */
    @Query("SELECT * FROM memory_relationships WHERE fromMemoryId = :fromId AND toMemoryId = :toId")
    MemoryRelationship getRelationshipBetween(String fromId, String toId);
    
    /**
     * Get bidirectional relationships (both directions exist)
     */
    @Query("SELECT r1.* FROM memory_relationships r1 " +
           "INNER JOIN memory_relationships r2 ON r1.fromMemoryId = r2.toMemoryId AND r1.toMemoryId = r2.fromMemoryId " +
           "WHERE r1.fromMemoryId = :memoryId")
    List<MemoryRelationship> getBidirectionalRelationships(String memoryId);
    
    /**
     * Get contradictory relationships for conflict detection
     */
    @Query("SELECT * FROM memory_relationships WHERE relationshipType = 'CONTRADICTS' ORDER BY strength DESC")
    List<MemoryRelationship> getContradictoryRelationships();
    
    /**
     * Get similar relationships for memory consolidation
     */
    @Query("SELECT * FROM memory_relationships WHERE relationshipType = 'SIMILAR' AND strength >= :minStrength ORDER BY strength DESC")
    List<MemoryRelationship> getSimilarRelationships(float minStrength);
    
    /**
     * Update relationship strength
     */
    @Query("UPDATE memory_relationships SET strength = :strength WHERE id = :relationshipId")
    void updateRelationshipStrength(String relationshipId, float strength);
    
    /**
     * Delete all relationships for a memory (when memory is deleted)
     */
    @Query("DELETE FROM memory_relationships WHERE fromMemoryId = :memoryId OR toMemoryId = :memoryId")
    void deleteAllRelationshipsFor(String memoryId);
    
    /**
     * Delete weak relationships below threshold
     */
    @Query("DELETE FROM memory_relationships WHERE strength < :minStrength")
    void deleteWeakRelationships(float minStrength);
    
    /**
     * Get relationship count
     */
    @Query("SELECT COUNT(*) FROM memory_relationships")
    int getRelationshipCount();
    
    /**
     * Get relationship count by type
     */
    @Query("SELECT COUNT(*) FROM memory_relationships WHERE relationshipType = :type")
    int getRelationshipCountByType(RelationshipType type);
    
    /**
     * Get relationship count for a memory
     */
    @Query("SELECT COUNT(*) FROM memory_relationships WHERE fromMemoryId = :memoryId OR toMemoryId = :memoryId")
    int getRelationshipCountFor(String memoryId);
    
    /**
     * Get most connected memories (highest relationship count)
     */
    @Query("SELECT fromMemoryId as memoryId, COUNT(*) as connectionCount " +
           "FROM memory_relationships " +
           "GROUP BY fromMemoryId " +
           "ORDER BY connectionCount DESC " +
           "LIMIT :limit")
    List<MemoryConnectionCount> getMostConnectedMemories(int limit);
    
    /**
     * Clear all relationships (for testing or reset)
     */
    @Query("DELETE FROM memory_relationships")
    void clearAllRelationships();
    
    /**
     * Helper class for connection count results
     */
    class MemoryConnectionCount {
        public String memoryId;
        public int connectionCount;
    }
}

package com.genuwin.app.memory.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.genuwin.app.memory.models.VersionedMemory;

import java.util.List;

/**
 * DAO for versioned memory operations
 * 
 * Provides database access for memory version tracking, including:
 * - Version history retrieval
 * - Version creation and updates
 * - Rollback support
 * - Audit trail queries
 */
@Dao
public interface VersionedMemoryDao {
    
    // ========== INSERT OPERATIONS ==========
    
    @Insert
    void insertVersion(VersionedMemory versionedMemory);
    
    @Insert
    void insertVersions(List<VersionedMemory> versionedMemories);
    
    // ========== UPDATE OPERATIONS ==========
    
    @Update
    void updateVersion(VersionedMemory versionedMemory);
    
    /**
     * Mark a version as current and all others for the same memory as not current
     */
    @Query("UPDATE versioned_memories SET isCurrent = CASE WHEN versionId = :versionId THEN 1 ELSE 0 END WHERE memoryId = :memoryId")
    void setCurrentVersion(String memoryId, String versionId);
    
    /**
     * Mark a version as backup
     */
    @Query("UPDATE versioned_memories SET isBackup = :isBackup WHERE versionId = :versionId")
    void setBackupStatus(String versionId, boolean isBackup);
    
    // ========== DELETE OPERATIONS ==========
    
    @Delete
    void deleteVersion(VersionedMemory versionedMemory);
    
    @Query("DELETE FROM versioned_memories WHERE versionId = :versionId")
    void deleteVersionById(String versionId);
    
    @Query("DELETE FROM versioned_memories WHERE memoryId = :memoryId")
    void deleteAllVersionsForMemory(String memoryId);
    
    /**
     * Delete old versions, keeping only the specified number of recent versions
     */
    @Query("DELETE FROM versioned_memories WHERE memoryId = :memoryId AND versionNumber NOT IN " +
           "(SELECT versionNumber FROM versioned_memories WHERE memoryId = :memoryId " +
           "ORDER BY versionNumber DESC LIMIT :keepCount)")
    void pruneOldVersions(String memoryId, int keepCount);
    
    // ========== QUERY OPERATIONS ==========
    
    /**
     * Get a specific version by ID
     */
    @Query("SELECT * FROM versioned_memories WHERE versionId = :versionId")
    VersionedMemory getVersionById(String versionId);
    
    /**
     * Get all versions for a specific memory, ordered by version number
     */
    @Query("SELECT * FROM versioned_memories WHERE memoryId = :memoryId ORDER BY versionNumber ASC")
    List<VersionedMemory> getVersionsForMemory(String memoryId);
    
    /**
     * Get recent versions for a memory (limited count)
     */
    @Query("SELECT * FROM versioned_memories WHERE memoryId = :memoryId " +
           "ORDER BY versionNumber DESC LIMIT :limit")
    List<VersionedMemory> getRecentVersionsForMemory(String memoryId, int limit);
    
    /**
     * Get the current version of a memory
     */
    @Query("SELECT * FROM versioned_memories WHERE memoryId = :memoryId AND isCurrent = 1")
    VersionedMemory getCurrentVersion(String memoryId);
    
    /**
     * Get the original version of a memory
     */
    @Query("SELECT * FROM versioned_memories WHERE memoryId = :memoryId AND isOriginal = 1")
    VersionedMemory getOriginalVersion(String memoryId);
    
    /**
     * Get a specific version number for a memory
     */
    @Query("SELECT * FROM versioned_memories WHERE memoryId = :memoryId AND versionNumber = :versionNumber")
    VersionedMemory getVersionByNumber(String memoryId, int versionNumber);
    
    /**
     * Get the latest version number for a memory
     */
    @Query("SELECT MAX(versionNumber) FROM versioned_memories WHERE memoryId = :memoryId")
    Integer getLatestVersionNumber(String memoryId);
    
    /**
     * Get version count for a memory
     */
    @Query("SELECT COUNT(*) FROM versioned_memories WHERE memoryId = :memoryId")
    int getVersionCount(String memoryId);
    
    // ========== AUDIT TRAIL QUERIES ==========
    
    /**
     * Get all versions by edit source
     */
    @Query("SELECT * FROM versioned_memories WHERE editSource = :editSource " +
           "ORDER BY versionTimestamp DESC LIMIT :limit")
    List<VersionedMemory> getVersionsByEditSource(String editSource, int limit);
    
    /**
     * Get versions within a time range
     */
    @Query("SELECT * FROM versioned_memories WHERE versionTimestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY versionTimestamp DESC")
    List<VersionedMemory> getVersionsInTimeRange(long startTime, long endTime);
    
    /**
     * Get versions with low confidence (for review)
     */
    @Query("SELECT * FROM versioned_memories WHERE editConfidence < :maxConfidence " +
           "ORDER BY editConfidence ASC, versionTimestamp DESC LIMIT :limit")
    List<VersionedMemory> getLowConfidenceVersions(float maxConfidence, int limit);
    
    /**
     * Get recent agent edits
     */
    @Query("SELECT * FROM versioned_memories WHERE editSource = 'AGENT' " +
           "ORDER BY versionTimestamp DESC LIMIT :limit")
    List<VersionedMemory> getRecentAgentEdits(int limit);
    
    /**
     * Get versions by edit reason pattern
     */
    @Query("SELECT * FROM versioned_memories WHERE editReason LIKE :reasonPattern " +
           "ORDER BY versionTimestamp DESC LIMIT :limit")
    List<VersionedMemory> getVersionsByReasonPattern(String reasonPattern, int limit);
    
    // ========== BACKUP AND ROLLBACK QUERIES ==========
    
    /**
     * Get all backup versions
     */
    @Query("SELECT * FROM versioned_memories WHERE isBackup = 1 " +
           "ORDER BY versionTimestamp DESC")
    List<VersionedMemory> getBackupVersions();
    
    /**
     * Get backup versions for a specific memory
     */
    @Query("SELECT * FROM versioned_memories WHERE memoryId = :memoryId AND isBackup = 1 " +
           "ORDER BY versionNumber DESC")
    List<VersionedMemory> getBackupVersionsForMemory(String memoryId);
    
    /**
     * Check if a memory has any versions
     */
    @Query("SELECT EXISTS(SELECT 1 FROM versioned_memories WHERE memoryId = :memoryId)")
    boolean hasVersions(String memoryId);
    
    /**
     * Get memories with multiple versions (indicating edits)
     */
    @Query("SELECT DISTINCT memoryId FROM versioned_memories " +
           "GROUP BY memoryId HAVING COUNT(*) > 1")
    List<String> getMemoriesWithMultipleVersions();
    
    // ========== STATISTICS QUERIES ==========
    
    /**
     * Get total version count across all memories
     */
    @Query("SELECT COUNT(*) FROM versioned_memories")
    int getTotalVersionCount();
    
    // ========== CLEANUP OPERATIONS ==========
    
    /**
     * Delete versions older than specified timestamp, except originals and current versions
     */
    @Query("DELETE FROM versioned_memories WHERE versionTimestamp < :cutoffTimestamp " +
           "AND isOriginal = 0 AND isCurrent = 0 AND isBackup = 0")
    void deleteOldVersions(long cutoffTimestamp);
    
    /**
     * Get storage size estimate (count of versions)
     */
    @Query("SELECT COUNT(*) FROM versioned_memories WHERE versionTimestamp < :cutoffTimestamp " +
           "AND isOriginal = 0 AND isCurrent = 0 AND isBackup = 0")
    int getCleanupCandidateCount(long cutoffTimestamp);
}

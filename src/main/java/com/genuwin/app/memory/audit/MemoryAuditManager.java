package com.genuwin.app.memory.audit;

import android.content.Context;
import android.util.Log;

import com.genuwin.app.memory.database.VersionedMemoryDao;
import com.genuwin.app.memory.models.Memory;
import com.genuwin.app.memory.models.VersionedMemory;
import com.genuwin.app.memory.operations.MemoryOperation;
import com.genuwin.app.memory.storage.MemoryStore;
import com.genuwin.app.settings.SettingsManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Memory Audit Manager - Comprehensive audit trail and rollback system
 * 
 * This manager provides:
 * - Complete logging of all memory operations
 * - Agent reasoning tracking for transparency
 * - Rollback functionality to previous versions
 * - Audit trail export for analysis
 * - Version history management
 */
public class MemoryAuditManager {
    private static final String TAG = "MemoryAuditManager";
    
    private final Context context;
    private final MemoryStore memoryStore;
    private final VersionedMemoryDao versionedMemoryDao;
    private final SettingsManager settingsManager;
    
    // Audit settings
    private static final int DEFAULT_MAX_VERSIONS_PER_MEMORY = 10;
    private static final int DEFAULT_AUDIT_RETENTION_DAYS = 90;
    private static final String AUDIT_LOG_FILENAME = "memory_audit.log";
    
    public MemoryAuditManager(Context context, MemoryStore memoryStore, VersionedMemoryDao versionedMemoryDao) {
        this.context = context;
        this.memoryStore = memoryStore;
        this.versionedMemoryDao = versionedMemoryDao;
        this.settingsManager = SettingsManager.getInstance(context);
    }
    
    // ========== OPERATION LOGGING ==========
    
    /**
     * Log a memory operation before execution
     * @param operation The operation being performed
     * @param agentReasoning The agent's reasoning (if applicable)
     * @return CompletableFuture that completes when logging is done
     */
    public CompletableFuture<Void> logOperationStart(MemoryOperation operation, String agentReasoning) {
        return CompletableFuture.runAsync(() -> {
            try {
                String logEntry = formatOperationStartLog(operation, agentReasoning);
                writeToAuditLog(logEntry);
                Log.d(TAG, "Operation started: " + operation.getDescription());
            } catch (Exception e) {
                Log.e(TAG, "Failed to log operation start", e);
            }
        });
    }
    
    /**
     * Log a memory operation after execution
     * @param operation The operation that was performed
     * @param success Whether the operation succeeded
     * @param errorMessage Error message if operation failed
     * @return CompletableFuture that completes when logging is done
     */
    public CompletableFuture<Void> logOperationComplete(MemoryOperation operation, boolean success, String errorMessage) {
        return CompletableFuture.runAsync(() -> {
            try {
                String logEntry = formatOperationCompleteLog(operation, success, errorMessage);
                writeToAuditLog(logEntry);
                
                if (success) {
                    Log.d(TAG, "Operation completed: " + operation.getDescription());
                } else {
                    Log.w(TAG, "Operation failed: " + operation.getDescription() + " - " + errorMessage);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to log operation completion", e);
            }
        });
    }
    
    /**
     * Create a version snapshot before a memory operation
     * @param memory The memory being modified
     * @param operation The operation being performed
     * @param agentReasoning Agent reasoning for the operation
     * @return CompletableFuture<VersionedMemory> The created version
     */
    public CompletableFuture<VersionedMemory> createVersionSnapshot(Memory memory, MemoryOperation operation, String agentReasoning) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get the next version number
                Integer latestVersion = versionedMemoryDao.getLatestVersionNumber(memory.getId());
                int nextVersion = (latestVersion != null ? latestVersion : 0) + 1;
                
                // Create versioned memory
                VersionedMemory versionedMemory = VersionedMemory.createNewVersion(
                    memory,
                    nextVersion,
                    operation.getDescription(),
                    determineEditSource(operation),
                    operation.getConfidence(),
                    agentReasoning
                );
                
                // Mark previous versions as not current
                if (nextVersion > 1) {
                    versionedMemoryDao.setCurrentVersion(memory.getId(), versionedMemory.getVersionId());
                }
                
                // Save the version
                versionedMemoryDao.insertVersion(versionedMemory);
                
                Log.d(TAG, "Created version snapshot: " + versionedMemory.getVersionId() + 
                          " (v" + nextVersion + ") for memory: " + memory.getId());
                
                return versionedMemory;
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to create version snapshot", e);
                throw new RuntimeException("Failed to create version snapshot", e);
            }
        });
    }
    
    // ========== ROLLBACK FUNCTIONALITY ==========
    
    /**
     * Rollback a memory to a previous version
     * @param memoryId The memory to rollback
     * @param targetVersionNumber The version to rollback to
     * @param rollbackReason Reason for the rollback
     * @return CompletableFuture<Memory> The restored memory
     */
    public CompletableFuture<Memory> rollbackToVersion(String memoryId, int targetVersionNumber, String rollbackReason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Starting rollback for memory " + memoryId + " to version " + targetVersionNumber);
                
                // Get the target version
                VersionedMemory targetVersion = versionedMemoryDao.getVersionByNumber(memoryId, targetVersionNumber);
                if (targetVersion == null) {
                    throw new IllegalArgumentException("Version " + targetVersionNumber + " not found for memory " + memoryId);
                }
                
                // Convert to Memory object
                Memory restoredMemory = targetVersion.toMemory();
                restoredMemory.setTimestamp(System.currentTimeMillis()); // Update timestamp to now
                
                // Create a new version entry for the rollback
                Integer latestVersion = versionedMemoryDao.getLatestVersionNumber(memoryId);
                int rollbackVersionNumber = (latestVersion != null ? latestVersion : 0) + 1;
                
                VersionedMemory rollbackVersion = VersionedMemory.createNewVersion(
                    restoredMemory,
                    rollbackVersionNumber,
                    "Rollback to version " + targetVersionNumber + ": " + rollbackReason,
                    VersionedMemory.EditSource.ROLLBACK,
                    1.0f, // High confidence for rollback
                    "Restored from version " + targetVersionNumber + " due to: " + rollbackReason
                );
                
                // Update the main memory
                memoryStore.updateMemory(restoredMemory);
                
                // Save the rollback version
                versionedMemoryDao.insertVersion(rollbackVersion);
                versionedMemoryDao.setCurrentVersion(memoryId, rollbackVersion.getVersionId());
                
                // Log the rollback
                String logEntry = formatRollbackLog(memoryId, targetVersionNumber, rollbackVersionNumber, rollbackReason);
                writeToAuditLog(logEntry);
                
                Log.d(TAG, "Successfully rolled back memory " + memoryId + " to version " + targetVersionNumber);
                return restoredMemory;
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to rollback memory", e);
                throw new RuntimeException("Failed to rollback memory", e);
            }
        });
    }
    
    /**
     * Rollback to the original version of a memory
     * @param memoryId The memory to rollback
     * @param rollbackReason Reason for the rollback
     * @return CompletableFuture<Memory> The restored original memory
     */
    public CompletableFuture<Memory> rollbackToOriginal(String memoryId, String rollbackReason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                VersionedMemory originalVersion = versionedMemoryDao.getOriginalVersion(memoryId);
                if (originalVersion == null) {
                    throw new IllegalArgumentException("Original version not found for memory " + memoryId);
                }
                
                return rollbackToVersion(memoryId, originalVersion.getVersionNumber(), 
                                       "Rollback to original: " + rollbackReason).get();
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to rollback to original", e);
                throw new RuntimeException("Failed to rollback to original", e);
            }
        });
    }
    
    // ========== VERSION HISTORY MANAGEMENT ==========
    
    /**
     * Get the complete version history for a memory
     * @param memoryId The memory ID
     * @return CompletableFuture<List<VersionedMemory>> All versions
     */
    public CompletableFuture<List<VersionedMemory>> getVersionHistory(String memoryId) {
        return CompletableFuture.supplyAsync(() -> versionedMemoryDao.getVersionsForMemory(memoryId));
    }
    
    /**
     * Get recent version history for a memory
     * @param memoryId The memory ID
     * @param limit Maximum number of versions to return
     * @return CompletableFuture<List<VersionedMemory>> Recent versions
     */
    public CompletableFuture<List<VersionedMemory>> getRecentVersionHistory(String memoryId, int limit) {
        return CompletableFuture.supplyAsync(() -> versionedMemoryDao.getRecentVersionsForMemory(memoryId, limit));
    }
    
    /**
     * Create a backup of the current version
     * @param memoryId The memory to backup
     * @param backupReason Reason for creating backup
     * @return CompletableFuture<VersionedMemory> The backup version
     */
    public CompletableFuture<VersionedMemory> createBackup(String memoryId, String backupReason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Memory currentMemory = memoryStore.getMemory(memoryId);
                if (currentMemory == null) {
                    throw new IllegalArgumentException("Memory not found: " + memoryId);
                }
                
                // Get next version number
                Integer latestVersion = versionedMemoryDao.getLatestVersionNumber(memoryId);
                int backupVersionNumber = (latestVersion != null ? latestVersion : 0) + 1;
                
                // Create backup version
                VersionedMemory backupVersion = VersionedMemory.createNewVersion(
                    currentMemory,
                    backupVersionNumber,
                    "Backup: " + backupReason,
                    VersionedMemory.EditSource.SYSTEM,
                    1.0f,
                    "Manual backup created: " + backupReason
                );
                
                backupVersion.setIsBackup(true);
                
                // Save backup
                versionedMemoryDao.insertVersion(backupVersion);
                
                Log.d(TAG, "Created backup for memory " + memoryId + ": " + backupVersion.getVersionId());
                return backupVersion;
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to create backup", e);
                throw new RuntimeException("Failed to create backup", e);
            }
        });
    }
    
    /**
     * Prune old versions to save storage space
     * @param memoryId The memory to prune versions for
     * @return CompletableFuture<Integer> Number of versions deleted
     */
    public CompletableFuture<Integer> pruneOldVersions(String memoryId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int maxVersions = settingsManager.getInt("memory_max_versions_per_memory", DEFAULT_MAX_VERSIONS_PER_MEMORY);
                
                // Get current version count
                Integer versionCount = versionedMemoryDao.getVersionCount(memoryId);
                if (versionCount == null || versionCount <= maxVersions) {
                    return 0; // No pruning needed
                }
                
                // Keep the most recent versions
                versionedMemoryDao.pruneOldVersions(memoryId, maxVersions);
                
                int deletedCount = versionCount - maxVersions;
                Log.d(TAG, "Pruned " + deletedCount + " old versions for memory " + memoryId);
                
                return deletedCount;
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to prune old versions", e);
                return 0;
            }
        });
    }
    
    // ========== AUDIT TRAIL EXPORT ==========
    
    /**
     * Export audit trail to a file
     * @param startTime Start time for export (0 for all time)
     * @param endTime End time for export (0 for current time)
     * @return CompletableFuture<File> The exported audit file
     */
    public CompletableFuture<File> exportAuditTrail(long startTime, long endTime) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final long finalEndTime = (endTime == 0) ? System.currentTimeMillis() : endTime;
                
                // Get versions in time range
                List<VersionedMemory> versions = versionedMemoryDao.getVersionsInTimeRange(startTime, finalEndTime);
                
                // Create export file
                File exportDir = new File(context.getExternalFilesDir(null), "memory_exports");
                if (!exportDir.exists()) {
                    exportDir.mkdirs();
                }
                
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                File exportFile = new File(exportDir, "memory_audit_" + timestamp + ".txt");
                
                // Write audit data
                try (FileWriter writer = new FileWriter(exportFile)) {
                    writer.write("Memory Audit Trail Export\n");
                    writer.write("Generated: " + new Date() + "\n");
                    writer.write("Time Range: " + new Date(startTime) + " to " + new Date(finalEndTime) + "\n");
                    writer.write("Total Versions: " + versions.size() + "\n");
                    writer.write("=" + "=".repeat(80) + "\n\n");
                    
                    for (VersionedMemory version : versions) {
                        writer.write(formatVersionForExport(version));
                        writer.write("\n" + "-".repeat(80) + "\n\n");
                    }
                }
                
                Log.d(TAG, "Exported audit trail to: " + exportFile.getAbsolutePath());
                return exportFile;
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to export audit trail", e);
                throw new RuntimeException("Failed to export audit trail", e);
            }
        });
    }
    
    /**
     * Get audit statistics
     * @return CompletableFuture<AuditStatistics> Statistics about the audit system
     */
    public CompletableFuture<AuditStatistics> getAuditStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AuditStatistics stats = new AuditStatistics();
                
                // Basic counts
                stats.totalVersions = versionedMemoryDao.getTotalVersionCount();
                stats.memoriesWithVersions = versionedMemoryDao.getMemoriesWithMultipleVersions().size();
                
                // Recent activity
                long weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
                stats.recentAgentEdits = versionedMemoryDao.getRecentAgentEdits(100).size();
                stats.lowConfidenceEdits = versionedMemoryDao.getLowConfidenceVersions(0.5f, 50).size();
                
                // Source breakdown
                stats.userEdits = versionedMemoryDao.getVersionsByEditSource(VersionedMemory.EditSource.USER, Integer.MAX_VALUE).size();
                stats.agentEdits = versionedMemoryDao.getVersionsByEditSource(VersionedMemory.EditSource.AGENT, Integer.MAX_VALUE).size();
                stats.systemEdits = versionedMemoryDao.getVersionsByEditSource(VersionedMemory.EditSource.SYSTEM, Integer.MAX_VALUE).size();
                
                return stats;
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to get audit statistics", e);
                return new AuditStatistics(); // Return empty stats on error
            }
        });
    }
    
    // ========== HELPER METHODS ==========
    
    private String determineEditSource(MemoryOperation operation) {
        // This could be enhanced to detect the actual source
        return VersionedMemory.EditSource.AGENT; // Default to agent for now
    }
    
    private String formatOperationStartLog(MemoryOperation operation, String agentReasoning) {
        StringBuilder log = new StringBuilder();
        log.append("[").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())).append("] ");
        log.append("OPERATION_START: ").append(operation.getOperationType()).append("\n");
        log.append("Description: ").append(operation.getDescription()).append("\n");
        log.append("Confidence: ").append(operation.getConfidence()).append("\n");
        if (agentReasoning != null && !agentReasoning.trim().isEmpty()) {
            log.append("Agent Reasoning: ").append(agentReasoning).append("\n");
        }
        return log.toString();
    }
    
    private String formatOperationCompleteLog(MemoryOperation operation, boolean success, String errorMessage) {
        StringBuilder log = new StringBuilder();
        log.append("[").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())).append("] ");
        log.append("OPERATION_COMPLETE: ").append(operation.getOperationType()).append("\n");
        log.append("Success: ").append(success).append("\n");
        if (!success && errorMessage != null) {
            log.append("Error: ").append(errorMessage).append("\n");
        }
        return log.toString();
    }
    
    private String formatRollbackLog(String memoryId, int fromVersion, int toVersion, String reason) {
        StringBuilder log = new StringBuilder();
        log.append("[").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())).append("] ");
        log.append("ROLLBACK: Memory ").append(memoryId).append("\n");
        log.append("From Version: ").append(fromVersion).append("\n");
        log.append("To Version: ").append(toVersion).append("\n");
        log.append("Reason: ").append(reason).append("\n");
        return log.toString();
    }
    
    private String formatVersionForExport(VersionedMemory version) {
        StringBuilder export = new StringBuilder();
        export.append("Version ID: ").append(version.getVersionId()).append("\n");
        export.append("Memory ID: ").append(version.getMemoryId()).append("\n");
        export.append("Version Number: ").append(version.getVersionNumber()).append("\n");
        export.append("Timestamp: ").append(new Date(version.getVersionTimestamp())).append("\n");
        export.append("Edit Source: ").append(version.getEditSource()).append("\n");
        export.append("Edit Reason: ").append(version.getEditReason()).append("\n");
        export.append("Confidence: ").append(version.getEditConfidence()).append("\n");
        export.append("Flags: ");
        if (version.isOriginal()) export.append("ORIGINAL ");
        if (version.isCurrent()) export.append("CURRENT ");
        if (version.isBackup()) export.append("BACKUP ");
        export.append("\n");
        if (version.getAgentReasoning() != null) {
            export.append("Agent Reasoning: ").append(version.getAgentReasoning()).append("\n");
        }
        export.append("Content: ").append(version.getContent()).append("\n");
        return export.toString();
    }
    
    private void writeToAuditLog(String logEntry) {
        try {
            File logFile = new File(context.getFilesDir(), AUDIT_LOG_FILENAME);
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(logEntry);
                writer.write("\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to audit log", e);
        }
    }
    
    // ========== AUDIT STATISTICS CLASS ==========
    
    public static class AuditStatistics {
        public int totalVersions = 0;
        public int memoriesWithVersions = 0;
        public int userEdits = 0;
        public int agentEdits = 0;
        public int systemEdits = 0;
        public int recentAgentEdits = 0;
        public int lowConfidenceEdits = 0;
        
        @Override
        public String toString() {
            return "AuditStatistics{" +
                    "totalVersions=" + totalVersions +
                    ", memoriesWithVersions=" + memoriesWithVersions +
                    ", userEdits=" + userEdits +
                    ", agentEdits=" + agentEdits +
                    ", systemEdits=" + systemEdits +
                    ", recentAgentEdits=" + recentAgentEdits +
                    ", lowConfidenceEdits=" + lowConfidenceEdits +
                    '}';
        }
    }
}

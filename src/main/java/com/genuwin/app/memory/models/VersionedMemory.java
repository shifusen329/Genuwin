package com.genuwin.app.memory.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

/**
 * Versioned Memory - Tracks the complete edit history of a memory
 * 
 * This model stores every version of a memory, allowing for:
 * - Complete edit history tracking
 * - Rollback to previous versions
 * - Audit trail of all changes
 * - Original content preservation
 */
@Entity(tableName = "versioned_memories")
@TypeConverters(MemoryTypeConverters.class)
public class VersionedMemory {
    
    @PrimaryKey
    @NonNull
    private String versionId;
    
    private String memoryId;        // ID of the main memory this version belongs to
    private int versionNumber;      // Sequential version number (1, 2, 3, etc.)
    private String content;         // Content at this version
    private MemoryType type;        // Type at this version
    private float importance;       // Importance at this version
    private byte[] embedding;       // Embedding at this version
    private float emotionalWeight;  // Emotional weight at this version
    private String metadata;        // Metadata at this version (JSON)
    
    // Version tracking
    private long versionTimestamp;  // When this version was created
    private String editReason;      // Why this edit was made
    private String editSource;      // What caused this edit (USER, AGENT, SYSTEM)
    private float editConfidence;   // Confidence in this edit
    private String agentReasoning;  // Agent's reasoning for this edit (if applicable)
    
    // Backup flags
    private boolean isOriginal;     // True if this is the original version
    private boolean isCurrent;      // True if this is the current active version
    private boolean isBackup;       // True if this is a backup version
    
    // Constructors
    public VersionedMemory() {
        this.versionId = java.util.UUID.randomUUID().toString();
        this.versionTimestamp = System.currentTimeMillis();
        this.isOriginal = false;
        this.isCurrent = false;
        this.isBackup = false;
    }
    
    public VersionedMemory(String memoryId, int versionNumber, Memory memory) {
        this();
        this.memoryId = memoryId;
        this.versionNumber = versionNumber;
        this.content = memory.getContent();
        this.type = memory.getType();
        this.importance = memory.getImportance();
        this.embedding = memory.getEmbedding();
        this.emotionalWeight = memory.getEmotionalWeight();
        this.metadata = memory.getMetadata();
        this.isOriginal = (versionNumber == 1);
        this.isCurrent = true; // New versions start as current
    }
    
    /**
     * Create a versioned memory from an existing memory (for initial version)
     */
    public static VersionedMemory fromMemory(Memory memory, String editReason, String editSource) {
        VersionedMemory versioned = new VersionedMemory(memory.getId(), 1, memory);
        versioned.setEditReason(editReason != null ? editReason : "Initial creation");
        versioned.setEditSource(editSource != null ? editSource : "SYSTEM");
        versioned.setEditConfidence(1.0f);
        versioned.setIsOriginal(true);
        return versioned;
    }
    
    /**
     * Create a new version from an existing memory
     */
    public static VersionedMemory createNewVersion(Memory memory, int versionNumber, 
                                                  String editReason, String editSource, 
                                                  float editConfidence, String agentReasoning) {
        VersionedMemory versioned = new VersionedMemory(memory.getId(), versionNumber, memory);
        versioned.setEditReason(editReason);
        versioned.setEditSource(editSource);
        versioned.setEditConfidence(editConfidence);
        versioned.setAgentReasoning(agentReasoning);
        versioned.setIsOriginal(false);
        return versioned;
    }
    
    /**
     * Convert this versioned memory back to a regular Memory object
     */
    public Memory toMemory() {
        Memory memory = new Memory();
        memory.setId(this.memoryId);
        memory.setContent(this.content);
        memory.setType(this.type);
        memory.setImportance(this.importance);
        memory.setEmbedding(this.embedding);
        memory.setEmotionalWeight(this.emotionalWeight);
        memory.setMetadata(this.metadata);
        memory.setTimestamp(this.versionTimestamp);
        return memory;
    }
    
    /**
     * Get a summary of this version for display
     */
    public String getVersionSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Version ").append(versionNumber);
        
        if (isOriginal) {
            summary.append(" (Original)");
        }
        if (isCurrent) {
            summary.append(" (Current)");
        }
        if (isBackup) {
            summary.append(" (Backup)");
        }
        
        summary.append("\n");
        summary.append("📅 ").append(new java.util.Date(versionTimestamp)).append("\n");
        summary.append("📝 Reason: ").append(editReason != null ? editReason : "Unknown").append("\n");
        summary.append("🔧 Source: ").append(editSource != null ? editSource : "Unknown").append("\n");
        summary.append("🎯 Confidence: ").append(String.format("%.2f", editConfidence)).append("\n");
        
        if (agentReasoning != null && !agentReasoning.trim().isEmpty()) {
            summary.append("🤖 Agent Reasoning: ").append(agentReasoning).append("\n");
        }
        
        summary.append("📊 Content: ").append(
            content.length() > 100 ? content.substring(0, 97) + "..." : content
        );
        
        return summary.toString();
    }
    
    /**
     * Check if this version represents a significant change from another version
     */
    public boolean isSignificantChangeFrom(VersionedMemory other) {
        if (other == null) return true;
        
        // Different content
        if (!this.content.equals(other.content)) return true;
        
        // Different type
        if (this.type != other.type) return true;
        
        // Significant importance change (>0.2)
        if (Math.abs(this.importance - other.importance) > 0.2f) return true;
        
        // Significant emotional weight change (>0.3)
        if (Math.abs(this.emotionalWeight - other.emotionalWeight) > 0.3f) return true;
        
        return false;
    }
    
    // Getters and Setters
    public String getVersionId() {
        return versionId;
    }
    
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }
    
    public String getMemoryId() {
        return memoryId;
    }
    
    public void setMemoryId(String memoryId) {
        this.memoryId = memoryId;
    }
    
    public int getVersionNumber() {
        return versionNumber;
    }
    
    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public MemoryType getType() {
        return type;
    }
    
    public void setType(MemoryType type) {
        this.type = type;
    }
    
    public float getImportance() {
        return importance;
    }
    
    public void setImportance(float importance) {
        this.importance = importance;
    }
    
    public byte[] getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(byte[] embedding) {
        this.embedding = embedding;
    }
    
    public float getEmotionalWeight() {
        return emotionalWeight;
    }
    
    public void setEmotionalWeight(float emotionalWeight) {
        this.emotionalWeight = emotionalWeight;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    public long getVersionTimestamp() {
        return versionTimestamp;
    }
    
    public void setVersionTimestamp(long versionTimestamp) {
        this.versionTimestamp = versionTimestamp;
    }
    
    public String getEditReason() {
        return editReason;
    }
    
    public void setEditReason(String editReason) {
        this.editReason = editReason;
    }
    
    public String getEditSource() {
        return editSource;
    }
    
    public void setEditSource(String editSource) {
        this.editSource = editSource;
    }
    
    public float getEditConfidence() {
        return editConfidence;
    }
    
    public void setEditConfidence(float editConfidence) {
        this.editConfidence = editConfidence;
    }
    
    public String getAgentReasoning() {
        return agentReasoning;
    }
    
    public void setAgentReasoning(String agentReasoning) {
        this.agentReasoning = agentReasoning;
    }
    
    public boolean isOriginal() {
        return isOriginal;
    }
    
    public void setIsOriginal(boolean isOriginal) {
        this.isOriginal = isOriginal;
    }
    
    public boolean isCurrent() {
        return isCurrent;
    }
    
    public void setIsCurrent(boolean isCurrent) {
        this.isCurrent = isCurrent;
    }
    
    public boolean isBackup() {
        return isBackup;
    }
    
    public void setIsBackup(boolean isBackup) {
        this.isBackup = isBackup;
    }
    
    @Override
    public String toString() {
        return "VersionedMemory{" +
                "versionId='" + versionId + '\'' +
                ", memoryId='" + memoryId + '\'' +
                ", versionNumber=" + versionNumber +
                ", editReason='" + editReason + '\'' +
                ", editSource='" + editSource + '\'' +
                ", isOriginal=" + isOriginal +
                ", isCurrent=" + isCurrent +
                '}';
    }
    
    /**
     * Edit source types
     */
    public static class EditSource {
        public static final String USER = "USER";           // User-initiated edit
        public static final String AGENT = "AGENT";         // AI agent edit
        public static final String SYSTEM = "SYSTEM";       // System-initiated edit
        public static final String MIGRATION = "MIGRATION"; // Data migration
        public static final String ROLLBACK = "ROLLBACK";   // Rollback operation
    }
}

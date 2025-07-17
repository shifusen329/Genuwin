package com.genuwin.app.memory.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.annotation.NonNull;

import java.util.UUID;

/**
 * Memory entity representing a single memory stored in the database
 */
@Entity(tableName = "memories")
@TypeConverters({MemoryTypeConverters.class})
public class Memory {
    
    @PrimaryKey
    @NonNull
    private String id;
    
    @NonNull
    private String content;
    
    @NonNull
    private MemoryType type;
    
    private float importance;
    
    private byte[] embedding;
    
    private long timestamp;
    
    private long lastAccessed;
    
    private int accessCount;
    
    private float emotionalWeight;
    
    private String metadata; // JSON string for flexible additional data
    
    // Default constructor for Room
    public Memory() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.lastAccessed = System.currentTimeMillis();
        this.accessCount = 0;
        this.importance = 0.5f;
        this.emotionalWeight = 0.0f;
    }
    
    // Constructor with essential fields
    public Memory(@NonNull String content, @NonNull MemoryType type, float importance) {
        this();
        this.content = content;
        this.type = type;
        this.importance = Math.max(0.0f, Math.min(1.0f, importance)); // Clamp to [0,1]
    }
    
    // Constructor with all fields
    public Memory(@NonNull String id, @NonNull String content, @NonNull MemoryType type, 
                  float importance, byte[] embedding, long timestamp, long lastAccessed, 
                  int accessCount, float emotionalWeight, String metadata) {
        this.id = id;
        this.content = content;
        this.type = type;
        this.importance = Math.max(0.0f, Math.min(1.0f, importance));
        this.embedding = embedding;
        this.timestamp = timestamp;
        this.lastAccessed = lastAccessed;
        this.accessCount = accessCount;
        this.emotionalWeight = emotionalWeight;
        this.metadata = metadata;
    }
    
    // Getters and Setters
    @NonNull
    public String getId() {
        return id;
    }
    
    public void setId(@NonNull String id) {
        this.id = id;
    }
    
    @NonNull
    public String getContent() {
        return content;
    }
    
    public void setContent(@NonNull String content) {
        this.content = content;
    }
    
    @NonNull
    public MemoryType getType() {
        return type;
    }
    
    public void setType(@NonNull MemoryType type) {
        this.type = type;
    }
    
    public float getImportance() {
        return importance;
    }
    
    public void setImportance(float importance) {
        this.importance = Math.max(0.0f, Math.min(1.0f, importance));
    }
    
    public byte[] getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(byte[] embedding) {
        this.embedding = embedding;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getLastAccessed() {
        return lastAccessed;
    }
    
    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }
    
    public int getAccessCount() {
        return accessCount;
    }
    
    public void setAccessCount(int accessCount) {
        this.accessCount = accessCount;
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
    
    /**
     * Update access tracking when memory is retrieved
     */
    public void markAccessed() {
        this.lastAccessed = System.currentTimeMillis();
        this.accessCount++;
    }
    
    /**
     * Calculate age in days
     */
    public long getAgeInDays() {
        return (System.currentTimeMillis() - timestamp) / (24 * 60 * 60 * 1000);
    }
    
    /**
     * Calculate recency score (1.0 = very recent, 0.0 = very old)
     */
    public float getRecencyScore() {
        long ageInDays = getAgeInDays();
        return (float) Math.exp(-ageInDays / 30.0); // Exponential decay over 30 days
    }
    
    /**
     * Calculate overall memory score combining importance, recency, and access frequency
     */
    public float getOverallScore() {
        float recencyScore = getRecencyScore();
        float accessScore = (float) Math.log(accessCount + 1) / 10.0f; // Log scale, normalized
        
        return (importance * 0.5f) + (recencyScore * 0.3f) + (accessScore * 0.2f);
    }
    
    @Override
    public String toString() {
        return "Memory{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                ", type=" + type +
                ", importance=" + importance +
                ", timestamp=" + timestamp +
                ", accessCount=" + accessCount +
                '}';
    }
}

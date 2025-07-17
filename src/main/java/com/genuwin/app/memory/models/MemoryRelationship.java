package com.genuwin.app.memory.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.annotation.NonNull;

/**
 * Entity representing relationships between memories
 */
@Entity(
    tableName = "memory_relationships",
    foreignKeys = {
        @ForeignKey(
            entity = Memory.class,
            parentColumns = "id",
            childColumns = "fromMemoryId",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Memory.class,
            parentColumns = "id", 
            childColumns = "toMemoryId",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index("fromMemoryId"),
        @Index("toMemoryId"),
        @Index(value = {"fromMemoryId", "toMemoryId"}, unique = true)
    }
)
public class MemoryRelationship {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @NonNull
    private String fromMemoryId;
    
    @NonNull
    private String toMemoryId;
    
    @NonNull
    private RelationshipType relationshipType;
    
    private float strength; // 0.0 to 1.0
    
    private long timestamp;
    
    // Default constructor for Room
    public MemoryRelationship() {
        this.timestamp = System.currentTimeMillis();
        this.strength = 0.5f;
    }
    
    // Constructor with essential fields
    public MemoryRelationship(@NonNull String fromMemoryId, @NonNull String toMemoryId, 
                             @NonNull RelationshipType relationshipType, float strength) {
        this();
        this.fromMemoryId = fromMemoryId;
        this.toMemoryId = toMemoryId;
        this.relationshipType = relationshipType;
        this.strength = Math.max(0.0f, Math.min(1.0f, strength));
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    @NonNull
    public String getFromMemoryId() {
        return fromMemoryId;
    }
    
    public void setFromMemoryId(@NonNull String fromMemoryId) {
        this.fromMemoryId = fromMemoryId;
    }
    
    @NonNull
    public String getToMemoryId() {
        return toMemoryId;
    }
    
    public void setToMemoryId(@NonNull String toMemoryId) {
        this.toMemoryId = toMemoryId;
    }
    
    @NonNull
    public RelationshipType getRelationshipType() {
        return relationshipType;
    }
    
    public void setRelationshipType(@NonNull RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }
    
    public float getStrength() {
        return strength;
    }
    
    public void setStrength(float strength) {
        this.strength = Math.max(0.0f, Math.min(1.0f, strength));
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Check if this relationship is bidirectional
     */
    public boolean isBidirectional() {
        return relationshipType == RelationshipType.SIMILAR || 
               relationshipType == RelationshipType.RELATED_TO;
    }
    
    /**
     * Create the reverse relationship if this is bidirectional
     */
    public MemoryRelationship createReverse() {
        if (!isBidirectional()) {
            return null;
        }
        return new MemoryRelationship(toMemoryId, fromMemoryId, relationshipType, strength);
    }
    
    @Override
    public String toString() {
        return "MemoryRelationship{" +
                "id=" + id +
                ", fromMemoryId='" + fromMemoryId + '\'' +
                ", toMemoryId='" + toMemoryId + '\'' +
                ", relationshipType=" + relationshipType +
                ", strength=" + strength +
                '}';
    }
}

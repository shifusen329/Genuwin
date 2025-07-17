package com.genuwin.app.memory.models;

import androidx.room.TypeConverter;

/**
 * Type converters for Room database to handle custom types
 */
public class MemoryTypeConverters {
    
    @TypeConverter
    public static String fromMemoryType(MemoryType type) {
        return type == null ? null : type.name();
    }
    
    @TypeConverter
    public static MemoryType toMemoryType(String type) {
        return type == null ? null : MemoryType.valueOf(type);
    }
    
    @TypeConverter
    public static String fromRelationshipType(RelationshipType type) {
        return type == null ? null : type.name();
    }
    
    @TypeConverter
    public static RelationshipType toRelationshipType(String type) {
        return type == null ? null : RelationshipType.valueOf(type);
    }
}

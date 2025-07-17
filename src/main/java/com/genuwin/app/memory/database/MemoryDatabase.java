package com.genuwin.app.memory.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.genuwin.app.memory.models.Memory;
import com.genuwin.app.memory.models.MemoryRelationship;
import com.genuwin.app.memory.models.VersionedMemory;
import com.genuwin.app.memory.models.MemoryTypeConverters;

/**
 * Room database configuration for the memory system
 * Follows the project naming convention for database classes
 */
@Database(
    entities = {
        Memory.class,
        MemoryRelationship.class,
        VersionedMemory.class
    },
    version = 2,
    exportSchema = false
)
@TypeConverters({MemoryTypeConverters.class})
public abstract class MemoryDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "memory_database";
    private static volatile MemoryDatabase INSTANCE;
    
    /**
     * Get DAO for memory operations
     */
    public abstract MemoryDao memoryDao();
    
    /**
     * Get DAO for memory relationship operations
     */
    public abstract MemoryRelationshipDao memoryRelationshipDao();
    
    /**
     * Get DAO for versioned memory operations
     */
    public abstract VersionedMemoryDao versionedMemoryDao();
    
    /**
     * Get singleton instance of the database
     */
    public static MemoryDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MemoryDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        MemoryDatabase.class,
                        DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration() // For development - remove in production
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Close the database instance
     */
    public static void closeDatabase() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}

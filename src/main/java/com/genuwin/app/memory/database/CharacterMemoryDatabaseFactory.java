package com.genuwin.app.memory.database;

import android.content.Context;
import android.util.Log;

import androidx.room.Room;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Factory for managing separate memory databases for each character
 * 
 * This factory ensures that each character has their own isolated database,
 * preventing any possibility of memory leakage between characters while
 * providing optimal performance through smaller, focused databases.
 */
public class CharacterMemoryDatabaseFactory {
    private static final String TAG = "CharacterMemoryDBFactory";
    
    private static CharacterMemoryDatabaseFactory instance;
    private final Context context;
    private final Map<String, MemoryDatabase> databases;
    
    private CharacterMemoryDatabaseFactory(Context context) {
        this.context = context.getApplicationContext();
        this.databases = new ConcurrentHashMap<>();
    }
    
    public static synchronized CharacterMemoryDatabaseFactory getInstance(Context context) {
        if (instance == null) {
            instance = new CharacterMemoryDatabaseFactory(context);
        }
        return instance;
    }
    
    /**
     * Get or create a database for the specified character
     * 
     * @param characterId The unique identifier for the character
     * @return The MemoryDatabase instance for this character
     */
    public synchronized MemoryDatabase getDatabaseForCharacter(String characterId) {
        if (characterId == null || characterId.trim().isEmpty()) {
            throw new IllegalArgumentException("Character ID cannot be null or empty");
        }
        
        return databases.computeIfAbsent(characterId, id -> {
            Log.d(TAG, "Creating new memory database for character: " + id);
            
            String databaseName = "memory_" + sanitizeCharacterId(id) + ".db";
            
            return Room.databaseBuilder(context, MemoryDatabase.class, databaseName)
                .fallbackToDestructiveMigration() // For development - remove in production
                .build();
        });
    }
    
    /**
     * Close and remove a character's database
     * This should be called when a character is deleted or no longer needed
     * 
     * @param characterId The character whose database should be closed
     */
    public synchronized void closeDatabaseForCharacter(String characterId) {
        MemoryDatabase database = databases.remove(characterId);
        if (database != null) {
            Log.d(TAG, "Closing memory database for character: " + characterId);
            database.close();
        }
    }
    
    /**
     * Close all character databases
     * This should be called when the app is shutting down
     */
    public synchronized void closeAllDatabases() {
        Log.d(TAG, "Closing all character memory databases");
        
        for (Map.Entry<String, MemoryDatabase> entry : databases.entrySet()) {
            try {
                entry.getValue().close();
                Log.d(TAG, "Closed database for character: " + entry.getKey());
            } catch (Exception e) {
                Log.e(TAG, "Error closing database for character " + entry.getKey(), e);
            }
        }
        
        databases.clear();
    }
    
    /**
     * Get the number of currently open character databases
     */
    public int getOpenDatabaseCount() {
        return databases.size();
    }
    
    /**
     * Check if a database exists for the specified character
     */
    public boolean hasDatabaseForCharacter(String characterId) {
        return databases.containsKey(characterId);
    }
    
    /**
     * Get all character IDs that have open databases
     */
    public String[] getCharacterIdsWithDatabases() {
        return databases.keySet().toArray(new String[0]);
    }
    
    /**
     * Sanitize character ID to be safe for use in database filename
     */
    private String sanitizeCharacterId(String characterId) {
        // Replace any characters that might be problematic in filenames
        return characterId.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }
    
    /**
     * Get database file path for a character (for backup/export purposes)
     */
    public String getDatabasePath(String characterId) {
        String databaseName = "memory_" + sanitizeCharacterId(characterId) + ".db";
        return context.getDatabasePath(databaseName).getAbsolutePath();
    }
    
    /**
     * Delete a character's database file completely
     * WARNING: This permanently deletes all memories for the character
     */
    public boolean deleteCharacterDatabase(String characterId) {
        try {
            // Close the database first
            closeDatabaseForCharacter(characterId);
            
            // Delete the database file
            String databaseName = "memory_" + sanitizeCharacterId(characterId) + ".db";
            boolean deleted = context.deleteDatabase(databaseName);
            
            Log.d(TAG, "Database deletion for character " + characterId + ": " + 
                (deleted ? "successful" : "failed"));
            
            return deleted;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting database for character " + characterId, e);
            return false;
        }
    }
}

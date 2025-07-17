package com.genuwin.app.managers;

import android.content.Context;
import android.util.Log;

import com.genuwin.app.memory.agent.MemoryAgent;
import com.genuwin.app.memory.database.CharacterMemoryDatabaseFactory;
import com.genuwin.app.memory.database.MemoryDatabase;
import com.genuwin.app.memory.operations.CreateMemoryOperation;
import com.genuwin.app.memory.operations.MergeMemoryOperation;
import com.genuwin.app.memory.operations.MemoryOperation;
import com.genuwin.app.memory.operations.UpdateMemoryOperation;
import com.genuwin.app.memory.storage.MemoryStore;
import com.genuwin.app.memory.storage.SQLiteMemoryStore;
import com.genuwin.app.memory.models.Memory;
import com.genuwin.app.memory.models.MemoryType;
import com.genuwin.app.memory.models.MemoryRelationship;
import com.genuwin.app.memory.models.RelationshipType;
import com.genuwin.app.memory.retrieval.MemoryRetrievalManager;
import com.genuwin.app.api.services.EmbeddingService;
import com.genuwin.app.settings.SettingsManager;
import com.genuwin.app.settings.SettingsManager.Keys;
import com.genuwin.app.settings.SettingsManager.Defaults;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

/**
 * Character-aware Memory Manager for the AI Waifu app
 * 
 * This manager provides separate memory storage for each character,
 * ensuring complete isolation between character memories while
 * providing a unified interface for memory operations.
 */
public class MemoryManager {
    private static final String TAG = "MemoryManager";
    
    private static MemoryManager instance;
    private final Context context;
    private final CharacterMemoryDatabaseFactory databaseFactory;
    private final Map<String, MemoryStore> characterMemoryStores;
    private final Map<String, MemoryRetrievalManager> characterRetrievalManagers;
    private final EmbeddingService embeddingService;
    private final ExecutorService executorService;
    private final SettingsManager settingsManager;
    private final MemoryAgent memoryAgent;

    // Memory system settings
    private boolean memoryEnabled;
    private int maxMemories;
    private int retentionDays;
    private float importanceThreshold;
    
    // Initialization tracking
    private volatile boolean isInitialized = false;
    private CompletableFuture<Void> initializationFuture = null;
    
    private MemoryManager(Context context) {
        this.context = context.getApplicationContext();
        this.databaseFactory = CharacterMemoryDatabaseFactory.getInstance(context);
        this.characterMemoryStores = new ConcurrentHashMap<>();
        this.characterRetrievalManagers = new ConcurrentHashMap<>();
        this.executorService = Executors.newCachedThreadPool();
        this.settingsManager = SettingsManager.getInstance(context);
        
        // Initialize services
        this.embeddingService = new EmbeddingService(context);
        this.memoryAgent = new MemoryAgent(context);

        // Load settings
        loadMemorySettings();
        
        Log.d(TAG, "Character-aware MemoryManager initialized - enabled: " + memoryEnabled);
    }
    
    /**
     * Get singleton instance of MemoryManager
     */
    public static synchronized MemoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new MemoryManager(context);
        }
        return instance;
    }
    
    /**
     * Get singleton instance (must be initialized first)
     */
    public static MemoryManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MemoryManager must be initialized with context first");
        }
        return instance;
    }
    
    /**
     * Get or create a memory store for the specified character
     */
    private MemoryStore getMemoryStoreForCharacter(String characterId) {
        return characterMemoryStores.computeIfAbsent(characterId, id -> {
            Log.d(TAG, "Creating memory store for character: " + id);
            MemoryDatabase database = databaseFactory.getDatabaseForCharacter(id);
            return new SQLiteMemoryStore(database);
        });
    }
    
    /**
     * Initialize the memory system
     */
    public CompletableFuture<Void> initialize() {
        // Return existing initialization future if already in progress
        if (initializationFuture != null) {
            return initializationFuture;
        }
        
        // Return completed future if already initialized
        if (isInitialized) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Start initialization
        initializationFuture = CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "Initializing character-aware memory system...");
                
                // Initialize embedding service
                embeddingService.initialize().get(); // Wait for initialization
                
                isInitialized = true;
                Log.d(TAG, "Character-aware memory system initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize memory system", e);
                isInitialized = false;
                initializationFuture = null; // Allow retry
                throw new RuntimeException("Memory system initialization failed", e);
            }
        }, executorService);
        
        return initializationFuture;
    }
    
    /**
     * Check if the memory system is initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Wait for initialization to complete if in progress
     */
    public CompletableFuture<Void> waitForInitialization() {
        if (isInitialized) {
            return CompletableFuture.completedFuture(null);
        }
        if (initializationFuture != null) {
            return initializationFuture;
        }
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Save a new memory for a specific character
     */
    public CompletableFuture<String> saveMemory(String characterId, String content, MemoryType type, float importance, String metadata) {
        if (!memoryEnabled) {
            Log.d(TAG, "Memory system disabled, skipping save");
            return CompletableFuture.completedFuture(null);
        }

        return embeddingService.generateEmbedding(content).thenApplyAsync(embeddingFloat -> {
            try {
                Log.d(TAG, "Saving memory for character " + characterId + ": " + content);

                Memory memory = new Memory(content, type, importance);
                if (metadata != null) {
                    memory.setMetadata(metadata);
                }
                MemoryStore memoryStore = getMemoryStoreForCharacter(characterId);

                byte[] embedding = EmbeddingService.floatArrayToByteArray(embeddingFloat);
                memory.setEmbedding(embedding);
                memoryStore.saveMemory(memory);

                Log.d(TAG, "Memory saved for character " + characterId + " with ID: " + memory.getId());
                return memory.getId();
            } catch (Exception e) {
                Log.e(TAG, "Failed to save memory for character " + characterId, e);
                throw new RuntimeException("Failed to save memory", e);
            }
        }, executorService);
    }

    /**
     * Save a new memory for a specific character
     */
    public CompletableFuture<String> saveMemory(String characterId, String content, MemoryType type, float importance) {
        return saveMemory(characterId, content, type, importance, null);
    }
    
    /**
     * Retrieve a memory by ID for a specific character
     */
    public CompletableFuture<Memory> getMemory(String characterId, String memoryId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Retrieving memory " + memoryId + " for character: " + characterId);
                
                MemoryStore memoryStore = getMemoryStoreForCharacter(characterId);
                Memory memory = memoryStore.getMemory(memoryId);
                
                if (memory != null) {
                    memory.markAccessed();
                    memoryStore.updateMemory(memory);
                }
                
                return memory;
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve memory for character " + characterId, e);
                throw new RuntimeException("Failed to retrieve memory", e);
            }
        }, executorService);
    }
    
    /**
     * Search for memories similar to the given text for a specific character
     */
    public CompletableFuture<List<Memory>> searchSimilarMemories(String characterId, String query, int limit) {
        if (!memoryEnabled) {
            return CompletableFuture.completedFuture(List.of());
        }

        return embeddingService.generateEmbedding(query).thenApplyAsync(queryEmbeddingFloat -> {
            try {
                Log.d(TAG, "Searching for memories similar to '" + query + "' for character: " + characterId);
                
                MemoryStore memoryStore = getMemoryStoreForCharacter(characterId);
                byte[] queryEmbedding = EmbeddingService.floatArrayToByteArray(queryEmbeddingFloat);
                List<Memory> similarMemories = memoryStore.findSimilarMemories(queryEmbedding, limit);
                
                // Update access tracking for retrieved memories
                for (Memory memory : similarMemories) {
                    memory.markAccessed();
                }
                memoryStore.updateMemories(similarMemories);
                
                return similarMemories;
            } catch (Exception e) {
                Log.e(TAG, "Failed to search memories for character " + characterId, e);
                throw new RuntimeException("Failed to search memories", e);
            }
        }, executorService);
    }
    
    /**
     * Get memories by type for a specific character
     */
    public CompletableFuture<List<Memory>> getMemoriesByType(String characterId, MemoryType type, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting memories of type " + type + " for character: " + characterId);
                
                MemoryStore memoryStore = getMemoryStoreForCharacter(characterId);
                return memoryStore.getMemoriesByType(type, limit);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to get memories by type for character " + characterId, e);
                throw new RuntimeException("Failed to get memories by type", e);
            }
        }, executorService);
    }
    
    /**
     * Update an existing memory for a specific character
     */
    public CompletableFuture<Void> updateMemory(String characterId, Memory memory) {
        return embeddingService.generateEmbedding(memory.getContent()).thenAcceptAsync(newEmbeddingFloat -> {
            try {
                Log.d(TAG, "Updating memory " + memory.getId() + " for character: " + characterId);
                
                MemoryStore memoryStore = getMemoryStoreForCharacter(characterId);
                
                byte[] newEmbedding = EmbeddingService.floatArrayToByteArray(newEmbeddingFloat);
                memory.setEmbedding(newEmbedding);
                
                memoryStore.updateMemory(memory);
                
                Log.d(TAG, "Memory updated successfully for character: " + characterId);
            } catch (Exception e) {
                Log.e(TAG, "Failed to update memory for character " + characterId, e);
                throw new RuntimeException("Failed to update memory", e);
            }
        }, executorService);
    }
    
    /**
     * Delete a memory for a specific character
     */
    public CompletableFuture<Void> deleteMemory(String characterId, String memoryId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "Deleting memory " + memoryId + " for character: " + characterId);
                
                MemoryStore memoryStore = getMemoryStoreForCharacter(characterId);
                memoryStore.deleteMemory(memoryId);
                
                Log.d(TAG, "Memory deleted successfully for character: " + characterId);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete memory for character " + characterId, e);
                throw new RuntimeException("Failed to delete memory", e);
            }
        }, executorService);
    }
    
    /**
     * Create a relationship between two memories for a specific character
     */
    public CompletableFuture<Void> createRelationship(String characterId, String fromMemoryId, String toMemoryId, 
                                                     RelationshipType type, float strength) {
        return CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "Creating relationship " + fromMemoryId + " -> " + toMemoryId + " for character: " + characterId);
                
                MemoryRelationship relationship = new MemoryRelationship(
                    fromMemoryId, toMemoryId, type, strength
                );
                
                MemoryStore memoryStore = getMemoryStoreForCharacter(characterId);
                memoryStore.saveRelationship(relationship);
                
                // Create reverse relationship if bidirectional
                if (relationship.isBidirectional()) {
                    MemoryRelationship reverse = relationship.createReverse();
                    if (reverse != null) {
                        memoryStore.saveRelationship(reverse);
                    }
                }
                
                Log.d(TAG, "Relationship created successfully for character: " + characterId);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to create relationship for character " + characterId, e);
                throw new RuntimeException("Failed to create relationship", e);
            }
        }, executorService);
    }
    
    /**
     * Get memory statistics for a specific character
     */
    public CompletableFuture<MemoryStats> getMemoryStats(String characterId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting memory statistics for character: " + characterId);
                
                MemoryStore memoryStore = getMemoryStoreForCharacter(characterId);
                int totalMemories = memoryStore.getTotalMemoryCount();
                int totalRelationships = memoryStore.getTotalRelationshipCount();
                long totalStorageSize = memoryStore.getTotalStorageSize();
                
                return new MemoryStats(characterId, totalMemories, totalRelationships, totalStorageSize);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to get memory statistics for character " + characterId, e);
                throw new RuntimeException("Failed to get memory statistics", e);
            }
        }, executorService);
    }
    
    /**
     * Get memory statistics for all characters
     */
    public CompletableFuture<Map<String, MemoryStats>> getAllMemoryStats() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Getting memory statistics for all characters");
                
                Map<String, MemoryStats> allStats = new ConcurrentHashMap<>();
                String[] characterIds = databaseFactory.getCharacterIdsWithDatabases();
                
                for (String characterId : characterIds) {
                    MemoryStats stats = getMemoryStats(characterId).get();
                    allStats.put(characterId, stats);
                }
                
                return allStats;
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to get memory statistics for all characters", e);
                throw new RuntimeException("Failed to get memory statistics", e);
            }
        }, executorService);
    }
    
    /**
     * Get or create a memory retrieval manager for the specified character
     */
    private MemoryRetrievalManager getRetrievalManagerForCharacter(String characterId) {
        return characterRetrievalManagers.computeIfAbsent(characterId, id -> {
            Log.d(TAG, "Creating memory retrieval manager for character: " + id);
            MemoryStore memoryStore = getMemoryStoreForCharacter(id);
            return new MemoryRetrievalManager(context, memoryStore, embeddingService);
        });
    }
    
    /**
     * Retrieve relevant memories for conversation context using smart retrieval
     * 
     * @param characterId Character ID for memory retrieval
     * @param query User message or conversation context
     * @param conversationHistory Recent conversation history
     * @return CompletableFuture containing list of relevant memories
     */
    public CompletableFuture<List<Memory>> retrieveRelevantMemories(
            String characterId, String query, List<String> conversationHistory) {
        
        if (!memoryEnabled) {
            return CompletableFuture.completedFuture(List.of());
        }
        
        MemoryRetrievalManager retrievalManager = getRetrievalManagerForCharacter(characterId);
        return retrievalManager.retrieveRelevantMemories(query, conversationHistory, characterId);
    }
    
    /**
     * Perform semantic search for memories
     * 
     * @param characterId Character ID for memory search
     * @param searchText Text to search for
     * @param maxResults Maximum number of results
     * @return CompletableFuture containing search results
     */
    public CompletableFuture<List<Memory>> semanticSearch(
            String characterId, String searchText, int maxResults) {
        
        if (!memoryEnabled) {
            return CompletableFuture.completedFuture(List.of());
        }
        
        MemoryRetrievalManager retrievalManager = getRetrievalManagerForCharacter(characterId);
        return retrievalManager.semanticSearch(searchText, characterId, maxResults);
    }
    
    /**
     * Check if memory retrieval should be triggered for a given message
     * 
     * @param userMessage The user's message
     * @param conversationHistory Recent conversation history
     * @return true if memory retrieval is recommended
     */
    public boolean shouldTriggerMemoryRetrieval(String userMessage, List<String> conversationHistory) {
        if (!memoryEnabled) {
            return false;
        }
        
        // Use any character's retrieval manager for the logic (it's character-agnostic)
        if (characterRetrievalManagers.isEmpty()) {
            // Create a temporary manager for the check
            MemoryRetrievalManager tempManager = new MemoryRetrievalManager(context, null, embeddingService);
            boolean result = tempManager.shouldTriggerMemoryRetrieval(userMessage, conversationHistory);
            tempManager.cleanup();
            return result;
        } else {
            // Use existing manager
            MemoryRetrievalManager manager = characterRetrievalManagers.values().iterator().next();
            return manager.shouldTriggerMemoryRetrieval(userMessage, conversationHistory);
        }
    }
    
    /**
     * Analyze conversation context to determine memory relevance
     * 
     * @param conversationHistory Recent conversation history
     * @return Context analysis result
     */
    public MemoryRetrievalManager.ContextAnalysis analyzeConversationContext(List<String> conversationHistory) {
        if (!memoryEnabled) {
            return new MemoryRetrievalManager.ContextAnalysis(false, List.of(), 0.0f);
        }
        
        // Use any character's retrieval manager for the logic (it's character-agnostic)
        if (characterRetrievalManagers.isEmpty()) {
            // Create a temporary manager for the analysis
            MemoryRetrievalManager tempManager = new MemoryRetrievalManager(context, null, embeddingService);
            MemoryRetrievalManager.ContextAnalysis result = tempManager.analyzeConversationContext(conversationHistory);
            tempManager.cleanup();
            return result;
        } else {
            // Use existing manager
            MemoryRetrievalManager manager = characterRetrievalManagers.values().iterator().next();
            return manager.analyzeConversationContext(conversationHistory);
        }
    }
    
    /**
     * Delete all memories for a specific character
     * WARNING: This permanently deletes all character data
     */
    public CompletableFuture<Boolean> deleteCharacterMemories(String characterId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.w(TAG, "Deleting ALL memories for character: " + characterId);
                
                // Remove from active stores
                characterMemoryStores.remove(characterId);
                
                // Delete the database file
                boolean deleted = databaseFactory.deleteCharacterDatabase(characterId);
                
                Log.w(TAG, "Character memory deletion for " + characterId + ": " + 
                    (deleted ? "successful" : "failed"));
                
                return deleted;
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete character memories for " + characterId, e);
                throw new RuntimeException("Failed to delete character memories", e);
            }
        }, executorService);
    }
    
    /**
     * Retrieve relevant memories for a given query using semantic search
     * This is the main method used by ConversationManager for memory context injection
     */
    public CompletableFuture<List<Memory>> retrieveRelevantMemories(String query, int maxResults) {
        try {
            // Get current character ID from CharacterManager
            String currentCharacterId = CharacterManager.getInstance(context).getCurrentCharacter().modelDirectory;
            MemoryRetrievalManager retrievalManager = getRetrievalManagerForCharacter(currentCharacterId);
            return retrievalManager.retrieveRelevantMemories(query, new ArrayList<>(), currentCharacterId);
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving relevant memories: " + e.getMessage(), e);
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
    }
    
    /**
     * Retrieve memory relationships for a list of memories
     * Used for enhanced context formatting with relationship information
     */
    public Map<String, List<MemoryRelationship>> getMemoryRelationships(List<Memory> memories) {
        Map<String, List<MemoryRelationship>> relationships = new HashMap<>();
        
        try {
            // Get current character ID from CharacterManager
            String currentCharacterId = CharacterManager.getInstance(context).getCurrentCharacter().modelDirectory;
            MemoryStore memoryStore = getMemoryStoreForCharacter(currentCharacterId);
            
            for (Memory memory : memories) {
                List<MemoryRelationship> memoryRelationships = memoryStore.getRelationshipsFor(memory.getId());
                if (memoryRelationships != null && !memoryRelationships.isEmpty()) {
                    relationships.put(memory.getId(), memoryRelationships);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving memory relationships: " + e.getMessage(), e);
        }
        
        return relationships;
    }
    
    /**
     * Process conversation for memory extraction and storage
     * This is called by ConversationManager after AI responses
     */
    public void processConversation(String userMessage, String aiResponse, MemoryProcessingCallback callback) {
        if (!isMemoryEnabled()) {
            if (callback != null) callback.onMemoryProcessed(new ArrayList<>());
            return;
        }

        executorService.execute(() -> {
            try {
                String characterId = CharacterManager.getInstance(context).getCurrentCharacter().modelDirectory;

                // 1. Retrieve relevant memories for context
                List<Memory> relevantMemories = retrieveRelevantMemories(userMessage, 10).get();

                // 2. Analyze conversation with MemoryAgent
                List<MemoryOperation> operations = memoryAgent.analyzeConversation(userMessage, aiResponse, relevantMemories).get();

                // 3. Execute operations
                List<Memory> processedMemories = new ArrayList<>();
                List<CompletableFuture<Memory>> executionFutures = new ArrayList<>();

                for (MemoryOperation op : operations) {
                    if (op.isValid()) {
                        executionFutures.add(executeOperation(characterId, op));
                    }
                }

                CompletableFuture.allOf(executionFutures.toArray(new CompletableFuture[0])).join();

                for (CompletableFuture<Memory> future : executionFutures) {
                    Memory result = future.get();
                    if (result != null) {
                        processedMemories.add(result);
                    }
                }

                if (callback != null) {
                    callback.onMemoryProcessed(processedMemories);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error processing conversation for memory: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onMemoryProcessingError(e.getMessage());
                }
            }
        });
    }

    private CompletableFuture<Memory> executeOperation(String characterId, MemoryOperation op) {
        switch (op.getOperationType()) {
            case CREATE: {
                CreateMemoryOperation createOp = (CreateMemoryOperation) op;
                return saveMemory(characterId, createOp.getContent(), createOp.getType(), createOp.getImportance(), createOp.getMetadata())
                        .thenCompose(memoryId -> {
                            if (memoryId != null) {
                                return getMemory(characterId, memoryId);
                            }
                            return CompletableFuture.completedFuture(null);
                        });
            }
            case UPDATE: {
                UpdateMemoryOperation updateOp = (UpdateMemoryOperation) op;
                return getMemory(characterId, updateOp.getMemoryId()).thenCompose(memoryToUpdate -> {
                    if (memoryToUpdate != null) {
                        if (updateOp.hasContentUpdate()) memoryToUpdate.setContent(updateOp.getNewContent());
                        if (updateOp.hasTypeUpdate()) memoryToUpdate.setType(updateOp.getNewType());
                        if (updateOp.hasImportanceUpdate()) memoryToUpdate.setImportance(updateOp.getNewImportance());
                        if (updateOp.hasMetadataUpdate()) memoryToUpdate.setMetadata(updateOp.getNewMetadata());
                        return updateMemory(characterId, memoryToUpdate).thenApply(v -> memoryToUpdate);
                    }
                    return CompletableFuture.completedFuture(null);
                });
            }
            case MERGE: {
                MergeMemoryOperation mergeOp = (MergeMemoryOperation) op;
                return saveMemory(characterId, mergeOp.getMergedContent(), mergeOp.getMergedType(), mergeOp.getMergedImportance(), mergeOp.getMergedMetadata())
                        .thenCompose(newMemoryId -> {
                            if (newMemoryId != null) {
                                if (mergeOp.shouldDeleteSourceMemories()) {
                                    List<CompletableFuture<Void>> deleteFutures = new ArrayList<>();
                                    for (String sourceId : mergeOp.getSourceMemoryIds()) {
                                        deleteFutures.add(deleteMemory(characterId, sourceId));
                                    }
                                    return CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0]))
                                            .thenCompose(v -> getMemory(characterId, newMemoryId));
                                } else {
                                    return getMemory(characterId, newMemoryId);
                                }
                            }
                            return CompletableFuture.completedFuture(null);
                        });
            }
            default:
                return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Callback interface for memory processing operations
     */
    public interface MemoryProcessingCallback {
        void onMemoryProcessed(List<Memory> extractedMemories);
        void onMemoryProcessingError(String error);
    }
    
    /**
     * Load memory settings from SettingsManager
     */
    private void loadMemorySettings() {
        memoryEnabled = settingsManager.getBoolean(Keys.MEMORY_ENABLED, Defaults.MEMORY_ENABLED);
        maxMemories = settingsManager.getInt("memory_max_entries", 10000);
        retentionDays = settingsManager.getInt("memory_retention_days", 365);
        importanceThreshold = settingsManager.getFloat("memory_importance_threshold", 0.3f);
    }
    
    /**
     * Check if memory system is enabled
     */
    public boolean isMemoryEnabled() {
        return memoryEnabled;
    }
    
    /**
     * Enable or disable memory system
     */
    public void setMemoryEnabled(boolean enabled) {
        this.memoryEnabled = enabled;
        settingsManager.setMemoryEnabled(enabled);
        Log.d(TAG, "Memory system " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up MemoryManager");
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        // Close all character databases
        databaseFactory.closeAllDatabases();
        characterMemoryStores.clear();
        
        Log.d(TAG, "MemoryManager cleanup complete");
    }
    
    /**
     * Character-aware statistics class for memory system
     */
    public static class MemoryStats {
        public final String characterId;
        public final int totalMemories;
        public final int totalRelationships;
        public final long totalStorageSize;
        
        public MemoryStats(String characterId, int totalMemories, int totalRelationships, long totalStorageSize) {
            this.characterId = characterId;
            this.totalMemories = totalMemories;
            this.totalRelationships = totalRelationships;
            this.totalStorageSize = totalStorageSize;
        }
        
        @Override
        public String toString() {
            return "MemoryStats{" +
                    "characterId='" + characterId + '\'' +
                    ", totalMemories=" + totalMemories +
                    ", totalRelationships=" + totalRelationships +
                    ", totalStorageSize=" + totalStorageSize +
                    '}';
        }
    }

    /**
     * Export all memories for a character to a JSON string.
     * @param characterId The ID of the character whose memories to export.
     * @return A CompletableFuture that will complete with the JSON string of memories.
     */
    public CompletableFuture<String> exportMemories(String characterId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Exporting memories for character: " + characterId);
                MemoryStore memoryStore = getMemoryStoreForCharacter(characterId);
                List<Memory> memories = memoryStore.getAllMemories();
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                return gson.toJson(memories);
            } catch (Exception e) {
                Log.e(TAG, "Failed to export memories for character " + characterId, e);
                throw new RuntimeException("Failed to export memories", e);
            }
        }, executorService);
    }

    /**
     * Import memories from a JSON string, replacing all existing memories for a character.
     * @param characterId The ID of the character to import memories for.
     * @param jsonData The JSON string containing the memories.
     * @return A CompletableFuture that will complete when the import is finished.
     */
    public CompletableFuture<Void> importMemories(String characterId, String jsonData) {
        return CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "Importing memories for character: " + characterId);
                MemoryStore memoryStore = getMemoryStoreForCharacter(characterId);
                Gson gson = new Gson();
                Type memoryListType = new TypeToken<ArrayList<Memory>>(){}.getType();
                List<Memory> memories = gson.fromJson(jsonData, memoryListType);

                if (memories != null) {
                    memoryStore.clearAllData();
                    memoryStore.saveMemories(memories);
                    Log.d(TAG, "Imported " + memories.size() + " memories for character " + characterId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to import memories for character " + characterId, e);
                throw new RuntimeException("Failed to import memories", e);
            }
        }, executorService);
    }

    /**
     * Merge memories from a JSON string with existing memories for a character.
     * @param characterId The ID of the character to merge memories for.
     * @param jsonData The JSON string containing the memories to merge.
     * @return A CompletableFuture that will complete when the merge is finished.
     */
    public CompletableFuture<Void> mergeMemories(String characterId, String jsonData) {
        return CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "Merging memories for character: " + characterId);
                MemoryStore memoryStore = getMemoryStoreForCharacter(characterId);
                Gson gson = new Gson();
                Type memoryListType = new TypeToken<ArrayList<Memory>>(){}.getType();
                List<Memory> memoriesToMerge = gson.fromJson(jsonData, memoryListType);

                if (memoriesToMerge != null) {
                    for (Memory memory : memoriesToMerge) {
                        Memory existing = memoryStore.getMemory(memory.getId());
                        if (existing != null) {
                            memoryStore.updateMemory(memory);
                        } else {
                            memoryStore.saveMemory(memory);
                        }
                    }
                    Log.d(TAG, "Merged " + memoriesToMerge.size() + " memories for character " + characterId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to merge memories for character " + characterId, e);
                throw new RuntimeException("Failed to merge memories", e);
            }
        }, executorService);
    }
}

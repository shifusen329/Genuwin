package com.genuwin.app.memory.retrieval;

import android.content.Context;
import android.util.Log;

import com.genuwin.app.api.services.EmbeddingService;
import com.genuwin.app.memory.models.Memory;
import com.genuwin.app.memory.models.MemoryType;
import com.genuwin.app.memory.storage.MemoryStore;
import com.genuwin.app.settings.SettingsManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MemoryRetrievalManager - Intelligent memory retrieval system
 * 
 * This manager handles semantic search, context-aware memory selection,
 * importance-based filtering, and recency weighting for memory retrieval.
 */
public class MemoryRetrievalManager {
    private static final String TAG = "MemoryRetrievalManager";
    
    private Context context;
    private MemoryStore memoryStore;
    private EmbeddingService embeddingService;
    private SettingsManager settingsManager;
    private ExecutorService executorService;
    
    // Retrieval parameters
    private static final int MAX_MEMORIES_PER_RETRIEVAL = 10;
    private static final float MIN_SIMILARITY_THRESHOLD = 0.3f;
    private static final long RECENCY_BOOST_HOURS = 24; // Boost memories from last 24 hours
    private static final float RECENCY_BOOST_FACTOR = 1.5f;
    private static final float IMPORTANCE_WEIGHT = 0.4f;
    private static final float SIMILARITY_WEIGHT = 0.4f;
    private static final float RECENCY_WEIGHT = 0.2f;
    
    public MemoryRetrievalManager(Context context, MemoryStore memoryStore, EmbeddingService embeddingService) {
        this.context = context;
        this.memoryStore = memoryStore;
        this.embeddingService = embeddingService;
        this.settingsManager = SettingsManager.getInstance(context);
        this.executorService = Executors.newSingleThreadExecutor();
        
        Log.d(TAG, "MemoryRetrievalManager initialized");
    }
    
    /**
     * Retrieve relevant memories for a given query with context awareness
     * 
     * @param query The search query (user message or conversation context)
     * @param conversationHistory Recent conversation history for context
     * @param characterId Current character ID for character-specific memories
     * @return CompletableFuture containing list of relevant memories
     */
    public CompletableFuture<List<Memory>> retrieveRelevantMemories(
            String query, List<String> conversationHistory, String characterId) {
        
        return embeddingService.generateEmbedding(query).thenCompose(queryEmbedding -> {
            if (queryEmbedding == null) {
                Log.w(TAG, "Failed to generate embedding for query");
                return CompletableFuture.completedFuture(new ArrayList<>());
            }

            return CompletableFuture.supplyAsync(() -> {
                try {
                    Log.d(TAG, "Retrieving relevant memories for query: " + query.substring(0, Math.min(50, query.length())) + "...");
                    
                    // Step 2: Get candidate memories from storage
                    List<Memory> candidateMemories = getCandidateMemories(characterId);
                    if (candidateMemories.isEmpty()) {
                        Log.d(TAG, "No candidate memories found");
                        return new ArrayList<Memory>();
                    }
                    
                    // Step 3: Calculate similarity scores
                    List<ScoredMemory> scoredMemories = calculateSimilarityScores(
                        candidateMemories, queryEmbedding, conversationHistory);
                    
                    // Step 4: Apply importance and recency weighting
                    List<ScoredMemory> weightedMemories = applyWeighting(scoredMemories);
                    
                    // Step 5: Filter by minimum threshold and select top memories
                    List<Memory> relevantMemories = selectTopMemories(weightedMemories);
                    
                    Log.d(TAG, "Retrieved " + relevantMemories.size() + " relevant memories");
                    return relevantMemories;
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error retrieving relevant memories", e);
                    return new ArrayList<Memory>();
                }
            }, executorService);
        });
    }
    
    /**
     * Perform semantic search for memories based on content similarity
     * 
     * @param searchText The text to search for
     * @param characterId Character ID for filtering
     * @param maxResults Maximum number of results to return
     * @return CompletableFuture containing search results
     */
    public CompletableFuture<List<Memory>> semanticSearch(
            String searchText, String characterId, int maxResults) {
        
        return embeddingService.generateEmbedding(searchText).thenCompose(searchEmbedding -> {
            if (searchEmbedding == null) {
                return CompletableFuture.completedFuture(new ArrayList<>());
            }

            return CompletableFuture.supplyAsync(() -> {
                try {
                    Log.d(TAG, "Performing semantic search for: " + searchText);
                    
                    // Get all memories for character
                    List<Memory> allMemories = memoryStore.getAllMemories();
                    
                    // Calculate similarities and sort
                    List<ScoredMemory> scoredMemories = new ArrayList<>();
                    for (Memory memory : allMemories) {
                        if (memory.getEmbedding() != null) {
                            float[] memoryEmbedding = EmbeddingService.byteArrayToFloatArray(memory.getEmbedding());
                            float similarity = calculateCosineSimilarity(searchEmbedding, memoryEmbedding);
                            if (similarity >= MIN_SIMILARITY_THRESHOLD) {
                                scoredMemories.add(new ScoredMemory(memory, similarity));
                            }
                        }
                    }
                    
                    // Sort by similarity and return top results
                    Collections.sort(scoredMemories, (a, b) -> Float.compare(b.score, a.score));
                    
                    List<Memory> results = new ArrayList<>();
                    for (int i = 0; i < Math.min(maxResults, scoredMemories.size()); i++) {
                        results.add(scoredMemories.get(i).memory);
                    }
                    
                    Log.d(TAG, "Semantic search returned " + results.size() + " results");
                    return results;
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error in semantic search", e);
                    return new ArrayList<Memory>();
                }
            }, executorService);
        });
    }
    
    /**
     * Get candidate memories from storage with basic filtering
     */
    private List<Memory> getCandidateMemories(String characterId) {
        try {
            // Get all memories for the character
            List<Memory> allMemories = memoryStore.getAllMemories();
            
            // Filter by importance threshold
            float importanceThreshold = settingsManager.getMemoryImportanceThreshold();
            List<Memory> candidates = new ArrayList<>();
            
            for (Memory memory : allMemories) {
                if (memory.getImportance() >= importanceThreshold) {
                    candidates.add(memory);
                }
            }
            
            Log.d(TAG, "Found " + candidates.size() + " candidate memories (filtered from " + allMemories.size() + ")");
            return candidates;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting candidate memories", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Calculate similarity scores for candidate memories
     */
    private List<ScoredMemory> calculateSimilarityScores(
            List<Memory> candidates, float[] queryEmbedding, List<String> conversationHistory) {
        
        List<ScoredMemory> scoredMemories = new ArrayList<>();
        
        for (Memory memory : candidates) {
            if (memory.getEmbedding() != null) {
                // Calculate base similarity
                float[] memoryEmbedding = EmbeddingService.byteArrayToFloatArray(memory.getEmbedding());
                float similarity = calculateCosineSimilarity(queryEmbedding, memoryEmbedding);
                
                // Apply context boost if memory relates to recent conversation
                float contextBoost = calculateContextBoost(memory, conversationHistory);
                float adjustedSimilarity = similarity + contextBoost;
                
                if (adjustedSimilarity >= MIN_SIMILARITY_THRESHOLD) {
                    scoredMemories.add(new ScoredMemory(memory, adjustedSimilarity));
                }
            }
        }
        
        return scoredMemories;
    }
    
    /**
     * Apply importance and recency weighting to scored memories
     */
    private List<ScoredMemory> applyWeighting(List<ScoredMemory> scoredMemories) {
        long currentTime = System.currentTimeMillis();
        
        for (ScoredMemory scoredMemory : scoredMemories) {
            Memory memory = scoredMemory.memory;
            
            // Calculate recency factor
            long memoryAge = currentTime - memory.getTimestamp();
            float recencyFactor = calculateRecencyFactor(memoryAge);
            
            // Calculate weighted score
            float weightedScore = 
                (scoredMemory.score * SIMILARITY_WEIGHT) +
                (memory.getImportance() * IMPORTANCE_WEIGHT) +
                (recencyFactor * RECENCY_WEIGHT);
            
            scoredMemory.score = weightedScore;
        }
        
        return scoredMemories;
    }
    
    /**
     * Select top memories based on final scores
     */
    private List<Memory> selectTopMemories(List<ScoredMemory> weightedMemories) {
        // Sort by weighted score
        Collections.sort(weightedMemories, (a, b) -> Float.compare(b.score, a.score));
        
        // Select top memories with diversity
        List<Memory> selectedMemories = new ArrayList<>();
        List<MemoryType> usedTypes = new ArrayList<>();
        
        for (ScoredMemory scoredMemory : weightedMemories) {
            if (selectedMemories.size() >= MAX_MEMORIES_PER_RETRIEVAL) {
                break;
            }
            
            Memory memory = scoredMemory.memory;
            
            // Ensure diversity by limiting memories of the same type
            long typeCount = usedTypes.stream().filter(type -> type == memory.getType()).count();
            if (typeCount < 3) { // Max 3 memories of same type
                selectedMemories.add(memory);
                usedTypes.add(memory.getType());
            }
        }
        
        return selectedMemories;
    }
    
    /**
     * Calculate cosine similarity between two embeddings
     */
    private float calculateCosineSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1.length != embedding2.length) {
            return 0.0f;
        }
        
        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;
        
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }
        
        if (norm1 == 0.0f || norm2 == 0.0f) {
            return 0.0f;
        }
        
        return dotProduct / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * Calculate context boost based on conversation history
     */
    private float calculateContextBoost(Memory memory, List<String> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return 0.0f;
        }
        
        float maxBoost = 0.0f;
        String memoryContent = memory.getContent().toLowerCase();
        
        // Check if memory content relates to recent conversation
        for (String historyItem : conversationHistory) {
            String historyLower = historyItem.toLowerCase();
            
            // Simple keyword matching for context boost
            String[] memoryWords = memoryContent.split("\\s+");
            String[] historyWords = historyLower.split("\\s+");
            
            int matches = 0;
            for (String memoryWord : memoryWords) {
                if (memoryWord.length() > 3) { // Only consider words longer than 3 chars
                    for (String historyWord : historyWords) {
                        if (memoryWord.equals(historyWord)) {
                            matches++;
                        }
                    }
                }
            }
            
            if (matches > 0) {
                float boost = Math.min(0.2f, matches * 0.05f); // Max 0.2 boost
                maxBoost = Math.max(maxBoost, boost);
            }
        }
        
        return maxBoost;
    }
    
    /**
     * Calculate recency factor for memory weighting
     */
    private float calculateRecencyFactor(long memoryAge) {
        long recentThreshold = RECENCY_BOOST_HOURS * 60 * 60 * 1000; // Convert to milliseconds
        
        if (memoryAge <= recentThreshold) {
            // Recent memories get a boost
            return RECENCY_BOOST_FACTOR;
        } else {
            // Older memories get diminishing returns
            long daysOld = memoryAge / (24 * 60 * 60 * 1000);
            return Math.max(0.1f, 1.0f / (1.0f + daysOld * 0.1f));
        }
    }
    
    /**
     * Check if memory retrieval should be triggered for a given query
     * 
     * @param userMessage The user's message
     * @param conversationHistory Recent conversation history
     * @return true if memory retrieval is recommended
     */
    public boolean shouldTriggerMemoryRetrieval(String userMessage, List<String> conversationHistory) {
        // Skip memory for very short messages
        if (userMessage.length() < 10) {
            return false;
        }
        
        // Skip memory for simple greetings
        String messageLower = userMessage.toLowerCase().trim();
        String[] simpleGreetings = {"hi", "hello", "hey", "good morning", "good evening", "good night"};
        for (String greeting : simpleGreetings) {
            if (messageLower.equals(greeting) || messageLower.startsWith(greeting + " ")) {
                return false;
            }
        }
        
        // Skip memory for simple responses
        String[] simpleResponses = {"yes", "no", "ok", "okay", "thanks", "thank you"};
        for (String response : simpleResponses) {
            if (messageLower.equals(response)) {
                return false;
            }
        }
        
        // Trigger memory for questions
        if (messageLower.contains("?") || messageLower.startsWith("what") || 
            messageLower.startsWith("how") || messageLower.startsWith("why") ||
            messageLower.startsWith("when") || messageLower.startsWith("where") ||
            messageLower.startsWith("who")) {
            return true;
        }
        
        // Trigger memory for personal references
        String[] personalKeywords = {"remember", "recall", "told you", "mentioned", "said", "talked about"};
        for (String keyword : personalKeywords) {
            if (messageLower.contains(keyword)) {
                return true;
            }
        }
        
        // Trigger memory for longer, substantive messages
        return userMessage.length() > 50;
    }
    
    /**
     * Analyze conversation context to determine memory relevance
     * 
     * @param conversationHistory Recent conversation history
     * @return Context analysis result
     */
    public ContextAnalysis analyzeConversationContext(List<String> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return new ContextAnalysis(false, new ArrayList<>(), 0.0f);
        }
        
        List<String> topics = extractTopics(conversationHistory);
        float contextComplexity = calculateContextComplexity(conversationHistory);
        boolean needsMemory = contextComplexity > 0.3f || !topics.isEmpty();
        
        return new ContextAnalysis(needsMemory, topics, contextComplexity);
    }
    
    /**
     * Extract topics from conversation history
     */
    private List<String> extractTopics(List<String> conversationHistory) {
        List<String> topics = new ArrayList<>();
        
        // Simple topic extraction based on keywords
        String[] topicKeywords = {
            "work", "job", "career", "family", "friend", "hobby", "travel", 
            "food", "movie", "book", "music", "sport", "health", "weather"
        };
        
        for (String message : conversationHistory) {
            String messageLower = message.toLowerCase();
            for (String keyword : topicKeywords) {
                if (messageLower.contains(keyword) && !topics.contains(keyword)) {
                    topics.add(keyword);
                }
            }
        }
        
        return topics;
    }
    
    /**
     * Calculate complexity of conversation context
     */
    private float calculateContextComplexity(List<String> conversationHistory) {
        if (conversationHistory.isEmpty()) {
            return 0.0f;
        }
        
        int totalWords = 0;
        int uniqueWords = 0;
        List<String> seenWords = new ArrayList<>();
        
        for (String message : conversationHistory) {
            String[] words = message.toLowerCase().split("\\s+");
            totalWords += words.length;
            
            for (String word : words) {
                if (word.length() > 3 && !seenWords.contains(word)) {
                    seenWords.add(word);
                    uniqueWords++;
                }
            }
        }
        
        if (totalWords == 0) {
            return 0.0f;
        }
        
        // Complexity based on vocabulary diversity and message length
        float vocabularyDiversity = (float) uniqueWords / totalWords;
        float averageMessageLength = (float) totalWords / conversationHistory.size();
        
        return Math.min(1.0f, vocabularyDiversity * 2.0f + (averageMessageLength / 50.0f));
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        Log.d(TAG, "MemoryRetrievalManager cleaned up");
    }
    
    /**
     * Helper class for scored memories
     */
    private static class ScoredMemory {
        final Memory memory;
        float score;
        
        ScoredMemory(Memory memory, float score) {
            this.memory = memory;
            this.score = score;
        }
    }
    
    /**
     * Context analysis result
     */
    public static class ContextAnalysis {
        public final boolean needsMemory;
        public final List<String> topics;
        public final float complexity;
        
        public ContextAnalysis(boolean needsMemory, List<String> topics, float complexity) {
            this.needsMemory = needsMemory;
            this.topics = topics;
            this.complexity = complexity;
        }
    }
}

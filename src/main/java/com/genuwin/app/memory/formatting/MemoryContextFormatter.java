package com.genuwin.app.memory.formatting;

import android.content.Context;
import android.util.Log;

import com.genuwin.app.memory.models.Memory;
import com.genuwin.app.memory.models.MemoryType;
import com.genuwin.app.memory.models.MemoryRelationship;
import com.genuwin.app.memory.models.RelationshipType;
import com.genuwin.app.settings.SettingsManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

/**
 * Advanced memory context formatter for AI consumption
 * Provides template-based formatting with timestamps, importance indicators,
 * and relationship context for optimal AI understanding
 */
public class MemoryContextFormatter {
    private static final String TAG = "MemoryContextFormatter";
    
    private final Context context;
    private final SettingsManager settingsManager;
    private final SimpleDateFormat dateFormat;
    
    // Memory formatting templates
    private static final String MEMORY_HEADER_TEMPLATE = 
        "=== RELEVANT MEMORIES FROM PREVIOUS CONVERSATIONS ===\n" +
        "Context: %d memories retrieved for query relevance\n" +
        "Instructions: Use this context naturally when relevant to the conversation\n\n";
    
    private static final String MEMORY_ITEM_TEMPLATE = 
        "[%s] %s %s\n" +
        "Content: %s\n" +
        "Context: %s | Accessed: %d times\n%s\n";
    
    private static final String RELATIONSHIP_TEMPLATE = 
        "Related: %s (%s)\n";
    
    private static final String MEMORY_FOOTER_TEMPLATE = 
        "\n=== END MEMORY CONTEXT ===\n" +
        "Note: Integrate these memories naturally into your response when relevant.";
    
    public MemoryContextFormatter(Context context) {
        this.context = context;
        this.settingsManager = SettingsManager.getInstance(context);
        this.dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
    }
    
    /**
     * Format memories for AI context injection with advanced templating
     * 
     * @param memories List of relevant memories to format
     * @param relationships Map of memory relationships for context
     * @param maxTokens Maximum tokens to use for formatting (for length balancing)
     * @return Formatted memory context string
     */
    public String formatMemoryContext(List<Memory> memories, 
                                    Map<String, List<MemoryRelationship>> relationships,
                                    int maxTokens) {
        if (memories == null || memories.isEmpty()) {
            return "";
        }
        
        try {
            StringBuilder context = new StringBuilder();
            
            // Add header with memory count
            context.append(String.format(MEMORY_HEADER_TEMPLATE, memories.size()));
            
            // Track token usage for length balancing
            int currentTokens = estimateTokens(context.toString());
            int remainingTokens = maxTokens - currentTokens - estimateTokens(MEMORY_FOOTER_TEMPLATE);
            
            // Sort memories by importance and recency for prioritization
            List<Memory> prioritizedMemories = prioritizeMemories(memories);
            
            // Format each memory with template
            for (Memory memory : prioritizedMemories) {
                String formattedMemory = formatSingleMemory(memory, relationships);
                int memoryTokens = estimateTokens(formattedMemory);
                
                // Check if we have space for this memory
                if (currentTokens + memoryTokens > remainingTokens) {
                    Log.d(TAG, "Token limit reached, truncating memory context");
                    context.append("... (additional memories truncated for length)\n");
                    break;
                }
                
                context.append(formattedMemory);
                currentTokens += memoryTokens;
            }
            
            // Add footer
            context.append(MEMORY_FOOTER_TEMPLATE);
            
            Log.d(TAG, "Formatted memory context: " + currentTokens + " estimated tokens");
            return context.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "Error formatting memory context: " + e.getMessage(), e);
            return formatFallbackContext(memories);
        }
    }
    
    /**
     * Format a single memory with full template including metadata
     */
    private String formatSingleMemory(Memory memory, Map<String, List<MemoryRelationship>> relationships) {
        // Generate importance indicator
        String importanceIndicator = getImportanceIndicator(memory.getImportance());
        
        // Generate type indicator
        String typeIndicator = getTypeIndicator(memory.getType());
        
        // Generate timestamp
        String timestamp = formatTimestamp(memory.getTimestamp());
        
        // Generate context metadata
        String contextInfo = formatContextInfo(memory);
        
        // Generate relationship context
        String relationshipContext = formatRelationshipContext(memory.getId(), relationships);
        
        return String.format(MEMORY_ITEM_TEMPLATE,
            importanceIndicator,
            typeIndicator,
            timestamp,
            memory.getContent(),
            contextInfo,
            memory.getAccessCount(),
            relationshipContext
        );
    }
    
    /**
     * Generate importance indicator based on memory importance score
     */
    private String getImportanceIndicator(float importance) {
        if (importance >= 0.8f) {
            return "★★★"; // High importance
        } else if (importance >= 0.6f) {
            return "★★☆"; // Medium importance
        } else if (importance >= 0.4f) {
            return "★☆☆"; // Low importance
        } else {
            return "☆☆☆"; // Very low importance
        }
    }
    
    /**
     * Generate type indicator for memory type
     */
    private String getTypeIndicator(MemoryType type) {
        switch (type) {
            case FACT:
                return "[FACT]";
            case PREFERENCE:
                return "[PREF]";
            case EMOTION:
                return "[EMOT]";
            case EVENT:
                return "[EVENT]";
            case RELATIONSHIP:
                return "[REL]";
            default:
                return "[INFO]";
        }
    }
    
    /**
     * Format timestamp for human-readable display
     */
    private String formatTimestamp(long timestamp) {
        try {
            Date date = new Date(timestamp);
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            
            // Show relative time for recent memories
            if (diff < 24 * 60 * 60 * 1000) { // Less than 24 hours
                long hours = diff / (60 * 60 * 1000);
                if (hours < 1) {
                    return "Recent";
                } else {
                    return hours + "h ago";
                }
            } else if (diff < 7 * 24 * 60 * 60 * 1000) { // Less than 7 days
                long days = diff / (24 * 60 * 60 * 1000);
                return days + "d ago";
            } else {
                return dateFormat.format(date);
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * Format context information including emotional weight and metadata
     */
    private String formatContextInfo(Memory memory) {
        StringBuilder context = new StringBuilder();
        
        // Add emotional weight if significant
        if (memory.getEmotionalWeight() > 0.5f) {
            context.append("Emotional");
            if (memory.getEmotionalWeight() > 0.8f) {
                context.append(" (Strong)");
            }
        } else {
            context.append("Neutral");
        }
        
        return context.toString();
    }
    
    /**
     * Format relationship context for a memory
     */
    private String formatRelationshipContext(String memoryId, Map<String, List<MemoryRelationship>> relationships) {
        if (relationships == null || !relationships.containsKey(memoryId)) {
            return "";
        }
        
        StringBuilder relationshipContext = new StringBuilder();
        List<MemoryRelationship> memoryRelationships = relationships.get(memoryId);
        
        // Limit to most important relationships to avoid clutter
        int maxRelationships = 2;
        int count = 0;
        
        for (MemoryRelationship relationship : memoryRelationships) {
            if (count >= maxRelationships) break;
            
            String relationshipType = formatRelationshipType(relationship.getRelationshipType());
            String relatedContent = getRelatedMemoryPreview(relationship.getToMemoryId());
            
            if (relatedContent != null && !relatedContent.isEmpty()) {
                relationshipContext.append(String.format(RELATIONSHIP_TEMPLATE, 
                    relatedContent, relationshipType));
                count++;
            }
        }
        
        return relationshipContext.toString();
    }
    
    /**
     * Format relationship type for display
     */
    private String formatRelationshipType(RelationshipType type) {
        switch (type) {
            case SIMILAR:
                return "similar";
            case CONTRADICTS:
                return "contradicts";
            case BUILDS_ON:
                return "builds on";
            case RELATED_TO:
                return "related";
            default:
                return "connected";
        }
    }
    
    /**
     * Get a preview of related memory content (stub - would need MemoryManager reference)
     */
    private String getRelatedMemoryPreview(String memoryId) {
        // This would need to be implemented with MemoryManager reference
        // For now, return a placeholder
        return "Related memory #" + memoryId.substring(0, Math.min(8, memoryId.length()));
    }
    
    /**
     * Prioritize memories by importance and recency
     */
    private List<Memory> prioritizeMemories(List<Memory> memories) {
        // Sort by combined score of importance and recency
        memories.sort((m1, m2) -> {
            float score1 = calculatePriorityScore(m1);
            float score2 = calculatePriorityScore(m2);
            return Float.compare(score2, score1); // Descending order
        });
        
        return memories;
    }
    
    /**
     * Calculate priority score combining importance, recency, and access frequency
     */
    private float calculatePriorityScore(Memory memory) {
        float importanceWeight = 0.5f;
        float recencyWeight = 0.3f;
        float accessWeight = 0.2f;
        
        // Normalize recency (more recent = higher score)
        long now = System.currentTimeMillis();
        long age = now - memory.getLastAccessed();
        float recencyScore = Math.max(0, 1.0f - (age / (7 * 24 * 60 * 60 * 1000f))); // 7 days max
        
        // Normalize access count (logarithmic scale)
        float accessScore = Math.min(1.0f, (float) Math.log(memory.getAccessCount() + 1) / 5.0f);
        
        return (memory.getImportance() * importanceWeight) + 
               (recencyScore * recencyWeight) + 
               (accessScore * accessWeight);
    }
    
    /**
     * Estimate token count for length balancing (rough approximation)
     */
    private int estimateTokens(String text) {
        // Rough approximation: 1 token ≈ 4 characters
        return text.length() / 4;
    }
    
    /**
     * Fallback formatting for error cases
     */
    private String formatFallbackContext(List<Memory> memories) {
        StringBuilder fallback = new StringBuilder();
        fallback.append("RELEVANT MEMORIES:\n");
        
        for (Memory memory : memories) {
            fallback.append("• ").append(memory.getContent()).append("\n");
        }
        
        fallback.append("\nUse this context naturally in your response when relevant.");
        return fallback.toString();
    }
    
    /**
     * Get maximum tokens for memory context based on settings
     */
    public int getMaxMemoryTokens() {
        // Get from settings or use default
        return settingsManager.getInt("memory_max_context_tokens", 800);
    }
}

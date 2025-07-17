package com.genuwin.app.memory.models;

/**
 * Enum representing different types of memories
 */
public enum MemoryType {
    /**
     * Factual information about the user or world
     * Example: "User works as a software engineer"
     */
    FACT,
    
    /**
     * User preferences and likes/dislikes
     * Example: "User prefers coffee over tea"
     */
    PREFERENCE,
    
    /**
     * Emotional moments and feelings
     * Example: "User felt sad when talking about their pet"
     */
    EMOTION,
    
    /**
     * Specific events and experiences
     * Example: "User went to Paris last summer"
     */
    EVENT,
    
    /**
     * Relationship information and social connections
     * Example: "User has a close relationship with their sister"
     */
    RELATIONSHIP,
    
    /**
     * Merged memories from consolidation
     * Example: Combined multiple related memories into one
     */
    MERGED,
    
    /**
     * System-generated summaries
     * Example: Summary of a long conversation
     */
    SUMMARY
}

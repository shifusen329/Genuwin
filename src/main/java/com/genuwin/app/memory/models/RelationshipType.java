package com.genuwin.app.memory.models;

/**
 * Enum representing different types of relationships between memories
 */
public enum RelationshipType {
    /**
     * Memories that are semantically similar or related
     * Example: "User likes coffee" and "User prefers hot beverages"
     */
    SIMILAR,
    
    /**
     * Memories that contradict each other
     * Example: "User likes cats" and "User is allergic to cats"
     */
    CONTRADICTS,
    
    /**
     * One memory builds upon or extends another
     * Example: "User works in tech" builds on "User is a software engineer"
     */
    BUILDS_ON,
    
    /**
     * Memories that are generally related but not specifically similar
     * Example: "User went to Paris" and "User speaks French"
     */
    RELATED_TO,
    
    /**
     * One memory is a more specific version of another
     * Example: "User likes animals" → "User has a pet dog named Max"
     */
    SPECIALIZES,
    
    /**
     * One memory is a more general version of another
     * Example: "User has a pet dog named Max" → "User likes animals"
     */
    GENERALIZES,
    
    /**
     * Memories that occurred in temporal sequence
     * Example: "User got promoted" followed by "User moved to new apartment"
     */
    FOLLOWS,
    
    /**
     * Memories that occurred before another
     * Example: "User studied computer science" preceded "User became a developer"
     */
    PRECEDES
}

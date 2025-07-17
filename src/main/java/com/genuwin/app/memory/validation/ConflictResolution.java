package com.genuwin.app.memory.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Conflict Resolution - Represents the result of conflict detection for a memory operation
 * 
 * Contains information about detected conflicts and their suggested resolutions.
 * Used by MemoryConflictResolver to provide comprehensive conflict analysis results.
 */
public class ConflictResolution {
    
    private final boolean hasConflicts;
    private final String message;
    private final List<ConflictInfo> conflicts;
    private final String errorMessage;
    
    private ConflictResolution(boolean hasConflicts, String message, List<ConflictInfo> conflicts, String errorMessage) {
        this.hasConflicts = hasConflicts;
        this.message = message;
        this.conflicts = conflicts != null ? new ArrayList<>(conflicts) : new ArrayList<>();
        this.errorMessage = errorMessage;
    }
    
    /**
     * Create a resolution indicating no conflicts were found
     * @param message Success message
     * @return ConflictResolution with no conflicts
     */
    public static ConflictResolution noConflicts(String message) {
        return new ConflictResolution(false, message, new ArrayList<>(), null);
    }
    
    /**
     * Create a resolution with detected conflicts
     * @param message Description message
     * @param conflicts List of detected conflicts
     * @return ConflictResolution with conflicts
     */
    public static ConflictResolution withConflicts(String message, List<ConflictInfo> conflicts) {
        return new ConflictResolution(true, message, conflicts, null);
    }
    
    /**
     * Create a resolution indicating an error occurred during conflict detection
     * @param errorMessage Error message
     * @return ConflictResolution indicating error
     */
    public static ConflictResolution error(String errorMessage) {
        return new ConflictResolution(false, "Conflict detection failed", new ArrayList<>(), errorMessage);
    }
    
    /**
     * Check if conflicts were detected
     * @return true if conflicts exist, false otherwise
     */
    public boolean hasConflicts() {
        return hasConflicts;
    }
    
    /**
     * Check if conflict detection completed successfully
     * @return true if no error occurred, false if there was an error
     */
    public boolean isSuccess() {
        return errorMessage == null;
    }
    
    /**
     * Check if an error occurred during conflict detection
     * @return true if an error occurred, false otherwise
     */
    public boolean isError() {
        return errorMessage != null;
    }
    
    /**
     * Get the main message
     * @return Description or success message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Get the error message (if any)
     * @return Error message or null if no error
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Get all detected conflicts
     * @return List of conflicts (may be empty)
     */
    public List<ConflictInfo> getConflicts() {
        return new ArrayList<>(conflicts);
    }
    
    /**
     * Get the number of conflicts detected
     * @return Number of conflicts
     */
    public int getConflictCount() {
        return conflicts.size();
    }
    
    /**
     * Get conflicts of a specific type
     * @param conflictType The type of conflicts to filter for
     * @return List of conflicts of the specified type
     */
    public List<ConflictInfo> getConflictsByType(MemoryConflictResolver.ConflictType conflictType) {
        List<ConflictInfo> filtered = new ArrayList<>();
        for (ConflictInfo conflict : conflicts) {
            if (conflict.getConflictType() == conflictType) {
                filtered.add(conflict);
            }
        }
        return filtered;
    }
    
    /**
     * Get conflicts by severity level
     * @param severity The severity level to filter for
     * @return List of conflicts with the specified severity
     */
    public List<ConflictInfo> getConflictsBySeverity(ConflictInfo.ConflictSeverity severity) {
        List<ConflictInfo> filtered = new ArrayList<>();
        for (ConflictInfo conflict : conflicts) {
            if (conflict.getSeverity() == severity) {
                filtered.add(conflict);
            }
        }
        return filtered;
    }
    
    /**
     * Get the highest severity level among all conflicts
     * @return Highest severity, or null if no conflicts
     */
    public ConflictInfo.ConflictSeverity getHighestSeverity() {
        if (conflicts.isEmpty()) {
            return null;
        }
        
        ConflictInfo.ConflictSeverity highest = ConflictInfo.ConflictSeverity.LOW;
        for (ConflictInfo conflict : conflicts) {
            ConflictInfo.ConflictSeverity severity = conflict.getSeverity();
            if (severity.ordinal() > highest.ordinal()) {
                highest = severity;
            }
        }
        return highest;
    }
    
    /**
     * Check if there are any high-severity conflicts
     * @return true if any conflicts are high severity
     */
    public boolean hasHighSeverityConflicts() {
        return !getConflictsBySeverity(ConflictInfo.ConflictSeverity.HIGH).isEmpty();
    }
    
    /**
     * Check if there are any medium-severity conflicts
     * @return true if any conflicts are medium severity
     */
    public boolean hasMediumSeverityConflicts() {
        return !getConflictsBySeverity(ConflictInfo.ConflictSeverity.MEDIUM).isEmpty();
    }
    
    /**
     * Check if all conflicts are low severity
     * @return true if all conflicts are low severity (or no conflicts)
     */
    public boolean hasOnlyLowSeverityConflicts() {
        if (!hasConflicts) return true;
        
        for (ConflictInfo conflict : conflicts) {
            if (conflict.getSeverity() != ConflictInfo.ConflictSeverity.LOW) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get a formatted summary of the conflict resolution
     * @return Human-readable summary including all conflicts
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (isError()) {
            summary.append("❌ ERROR: ").append(errorMessage).append("\n");
            return summary.toString();
        }
        
        if (!hasConflicts) {
            summary.append("✅ NO CONFLICTS: ").append(message).append("\n");
            return summary.toString();
        }
        
        summary.append("⚠️ CONFLICTS DETECTED: ").append(message).append("\n");
        summary.append("📊 Total Conflicts: ").append(conflicts.size()).append("\n");
        
        // Summary by severity
        int highCount = getConflictsBySeverity(ConflictInfo.ConflictSeverity.HIGH).size();
        int mediumCount = getConflictsBySeverity(ConflictInfo.ConflictSeverity.MEDIUM).size();
        int lowCount = getConflictsBySeverity(ConflictInfo.ConflictSeverity.LOW).size();
        
        if (highCount > 0) summary.append("🔴 High Severity: ").append(highCount).append("\n");
        if (mediumCount > 0) summary.append("🟡 Medium Severity: ").append(mediumCount).append("\n");
        if (lowCount > 0) summary.append("🟢 Low Severity: ").append(lowCount).append("\n");
        
        summary.append("\n📋 Detailed Conflicts:\n");
        for (int i = 0; i < conflicts.size(); i++) {
            summary.append("\n--- Conflict ").append(i + 1).append(" ---\n");
            summary.append(conflicts.get(i).getSummary());
        }
        
        return summary.toString();
    }
    
    /**
     * Get a concise summary for logging
     * @return Brief summary suitable for log messages
     */
    public String getLogSummary() {
        if (isError()) {
            return "ERROR: " + errorMessage;
        }
        
        if (!hasConflicts) {
            return "NO CONFLICTS: " + message;
        }
        
        ConflictInfo.ConflictSeverity highest = getHighestSeverity();
        return String.format("CONFLICTS: %d total (%s severity) - %s", 
                           conflicts.size(), highest, message);
    }
    
    /**
     * Get recommended action based on conflict analysis
     * @return Recommended action for handling the conflicts
     */
    public RecommendedAction getRecommendedAction() {
        if (isError()) {
            return RecommendedAction.RETRY_DETECTION;
        }
        
        if (!hasConflicts) {
            return RecommendedAction.PROCEED;
        }
        
        if (hasHighSeverityConflicts()) {
            return RecommendedAction.BLOCK_OPERATION;
        }
        
        if (hasMediumSeverityConflicts()) {
            return RecommendedAction.REQUIRE_REVIEW;
        }
        
        return RecommendedAction.PROCEED_WITH_WARNING;
    }
    
    /**
     * Combine this resolution with another conflict resolution
     * @param other Another conflict resolution to combine with
     * @return Combined conflict resolution
     */
    public ConflictResolution combine(ConflictResolution other) {
        if (other == null) {
            return this;
        }
        
        // If either has an error, the combined result has an error
        if (this.isError() || other.isError()) {
            String combinedError = this.isError() ? this.errorMessage : other.errorMessage;
            return ConflictResolution.error(combinedError);
        }
        
        // Combine conflicts
        List<ConflictInfo> combinedConflicts = new ArrayList<>(this.conflicts);
        combinedConflicts.addAll(other.conflicts);
        
        boolean combinedHasConflicts = !combinedConflicts.isEmpty();
        String combinedMessage = this.message + "; " + other.message;
        
        return new ConflictResolution(combinedHasConflicts, combinedMessage, combinedConflicts, null);
    }
    
    @Override
    public String toString() {
        return "ConflictResolution{" +
                "hasConflicts=" + hasConflicts +
                ", message='" + message + '\'' +
                ", conflicts=" + conflicts.size() +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
    
    /**
     * Recommended actions based on conflict analysis
     */
    public enum RecommendedAction {
        PROCEED,                // No conflicts, safe to proceed
        PROCEED_WITH_WARNING,   // Minor conflicts, proceed but warn user
        REQUIRE_REVIEW,         // Moderate conflicts, require user review
        BLOCK_OPERATION,        // Serious conflicts, block the operation
        RETRY_DETECTION         // Error occurred, retry conflict detection
    }
}

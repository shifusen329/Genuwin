package com.genuwin.app.memory.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation Result - Represents the outcome of a memory operation validation
 * 
 * Contains success/failure status, detailed messages, and warnings for memory operations.
 * Used by MemoryEditValidator to provide comprehensive feedback on validation attempts.
 */
public class ValidationResult {
    
    private final boolean success;
    private final String message;
    private final List<String> warnings;
    private final String errorCode;
    
    private ValidationResult(boolean success, String message, List<String> warnings, String errorCode) {
        this.success = success;
        this.message = message;
        this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
        this.errorCode = errorCode;
    }
    
    /**
     * Create a successful validation result
     * @param message Success message
     * @return ValidationResult indicating success
     */
    public static ValidationResult success(String message) {
        return new ValidationResult(true, message, new ArrayList<>(), null);
    }
    
    /**
     * Create a successful validation result with warnings
     * @param message Success message
     * @param warnings List of warning messages
     * @return ValidationResult indicating success with warnings
     */
    public static ValidationResult success(String message, List<String> warnings) {
        return new ValidationResult(true, message, warnings, null);
    }
    
    /**
     * Create a failed validation result
     * @param message Failure message
     * @return ValidationResult indicating failure
     */
    public static ValidationResult failure(String message) {
        return new ValidationResult(false, message, new ArrayList<>(), null);
    }
    
    /**
     * Create a failed validation result with error code
     * @param message Failure message
     * @param errorCode Error code for programmatic handling
     * @return ValidationResult indicating failure with error code
     */
    public static ValidationResult failure(String message, String errorCode) {
        return new ValidationResult(false, message, new ArrayList<>(), errorCode);
    }
    
    /**
     * Create a failed validation result with warnings
     * @param message Failure message
     * @param warnings List of warning messages that led to failure
     * @return ValidationResult indicating failure with context
     */
    public static ValidationResult failure(String message, List<String> warnings) {
        return new ValidationResult(false, message, warnings, null);
    }
    
    /**
     * Check if validation was successful
     * @return true if validation passed, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Check if validation failed
     * @return true if validation failed, false otherwise
     */
    public boolean isFailure() {
        return !success;
    }
    
    /**
     * Get the main validation message
     * @return Success or failure message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Get validation warnings
     * @return List of warning messages (may be empty)
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
    
    /**
     * Check if there are any warnings
     * @return true if warnings exist, false otherwise
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    /**
     * Get the error code (if any)
     * @return Error code for programmatic handling, or null if no specific code
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Add a warning to this result
     * @param warning Warning message to add
     */
    public void addWarning(String warning) {
        if (warning != null && !warning.trim().isEmpty()) {
            warnings.add(warning.trim());
        }
    }
    
    /**
     * Add multiple warnings to this result
     * @param newWarnings List of warning messages to add
     */
    public void addWarnings(List<String> newWarnings) {
        if (newWarnings != null) {
            for (String warning : newWarnings) {
                addWarning(warning);
            }
        }
    }
    
    /**
     * Get a formatted summary of the validation result
     * @return Human-readable summary including message and warnings
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append(success ? "✅ SUCCESS: " : "❌ FAILURE: ");
        summary.append(message);
        
        if (hasWarnings()) {
            summary.append("\n\n⚠️ Warnings:");
            for (int i = 0; i < warnings.size(); i++) {
                summary.append("\n").append(i + 1).append(". ").append(warnings.get(i));
            }
        }
        
        if (errorCode != null) {
            summary.append("\n\nError Code: ").append(errorCode);
        }
        
        return summary.toString();
    }
    
    /**
     * Get a concise summary for logging
     * @return Brief summary suitable for log messages
     */
    public String getLogSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(success ? "SUCCESS" : "FAILURE");
        summary.append(": ").append(message);
        
        if (hasWarnings()) {
            summary.append(" (").append(warnings.size()).append(" warnings)");
        }
        
        if (errorCode != null) {
            summary.append(" [").append(errorCode).append("]");
        }
        
        return summary.toString();
    }
    
    /**
     * Combine this result with another validation result
     * If either result is a failure, the combined result is a failure
     * Warnings from both results are merged
     * 
     * @param other Another validation result to combine with
     * @return Combined validation result
     */
    public ValidationResult combine(ValidationResult other) {
        if (other == null) {
            return this;
        }
        
        boolean combinedSuccess = this.success && other.success;
        String combinedMessage;
        List<String> combinedWarnings = new ArrayList<>(this.warnings);
        combinedWarnings.addAll(other.warnings);
        String combinedErrorCode = this.errorCode != null ? this.errorCode : other.errorCode;
        
        if (combinedSuccess) {
            combinedMessage = this.message + "; " + other.message;
        } else {
            // If either failed, use the failure message
            combinedMessage = this.success ? other.message : this.message;
        }
        
        return new ValidationResult(combinedSuccess, combinedMessage, combinedWarnings, combinedErrorCode);
    }
    
    @Override
    public String toString() {
        return "ValidationResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", warnings=" + warnings.size() +
                ", errorCode='" + errorCode + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ValidationResult that = (ValidationResult) o;
        
        if (success != that.success) return false;
        if (!message.equals(that.message)) return false;
        if (!warnings.equals(that.warnings)) return false;
        return errorCode != null ? errorCode.equals(that.errorCode) : that.errorCode == null;
    }
    
    @Override
    public int hashCode() {
        int result = (success ? 1 : 0);
        result = 31 * result + message.hashCode();
        result = 31 * result + warnings.hashCode();
        result = 31 * result + (errorCode != null ? errorCode.hashCode() : 0);
        return result;
    }
}

package com.genuwin.app.tools;

import com.google.gson.JsonObject;

/**
 * Interface for tool execution implementations
 */
public interface ToolExecutor {
    
    /**
     * Execute the tool with given parameters
     * @param parameters Tool parameters as JsonObject
     * @param callback Callback for tool execution result
     */
    void execute(JsonObject parameters, ToolExecutionCallback callback);
    
    /**
     * Get the tool definition for this executor
     * @return ToolDefinition describing this tool
     */
    ToolDefinition getToolDefinition();
    
    /**
     * Validate parameters before execution
     * @param parameters Parameters to validate
     * @return ValidationResult indicating if parameters are valid
     */
    ValidationResult validateParameters(JsonObject parameters);
    
    /**
     * Callback interface for tool execution results
     */
    interface ToolExecutionCallback {
        /**
         * Called when tool execution succeeds
         * @param result The result of the tool execution
         */
        void onSuccess(String result);
        
        /**
         * Called when tool execution fails
         * @param error Error message describing the failure
         */
        void onError(String error);
        
        /**
         * Called when tool execution requires user confirmation
         * @param message Confirmation message to show user
         * @param onConfirm Callback to execute if user confirms
         * @param onCancel Callback to execute if user cancels
         */
        void onConfirmationRequired(String message, Runnable onConfirm, Runnable onCancel);
    }
    
    /**
     * Result of parameter validation
     */
    class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

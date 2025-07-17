package com.genuwin.app.memory.agent;

import android.content.Context;
import android.util.Log;

import com.genuwin.app.managers.ApiManager;
import com.genuwin.app.memory.models.Memory;
import com.genuwin.app.memory.operations.*;
import com.genuwin.app.settings.SettingsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Memory Agent - LLM-powered intelligent memory management
 * 
 * This agent uses a separate LLM call to analyze conversations and determine
 * what memory operations should be performed. It acts as an intelligent layer
 * between the conversation system and the memory storage system.
 */
public class MemoryAgent {
    private static final String TAG = "MemoryAgent";
    
    private Context context;
    private ApiManager apiManager;
    private SettingsManager settingsManager;
    private ExecutorService executorService;
    
    // System prompts for different memory operations
    private final MemoryAgentPrompts prompts;
    
    public MemoryAgent(Context context) {
        this.context = context;
        this.apiManager = ApiManager.getInstance(context);
        this.settingsManager = SettingsManager.getInstance(context);
        this.executorService = Executors.newSingleThreadExecutor();
        this.prompts = new MemoryAgentPrompts();
        
        Log.d(TAG, "MemoryAgent initialized");
    }
    
    /**
     * Analyze a conversation turn and determine what memory operations should be performed
     * 
     * @param userMessage The user's message
     * @param assistantResponse The assistant's response
     * @param existingMemories List of relevant existing memories (for context)
     * @return List of memory operations to perform
     */
    public CompletableFuture<List<MemoryOperation>> analyzeConversation(
            String userMessage, String assistantResponse, List<Memory> existingMemories) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Analyzing conversation for memory operations");
                
                // Build the analysis prompt
                String analysisPrompt = prompts.buildAnalysisPrompt(
                    userMessage, assistantResponse, existingMemories);
                
                // Call LLM for memory analysis
                String response = callMemoryLLM(analysisPrompt);
                        Log.i(TAG, "Memory Agent LLM Raw Response: " + response);

                // Parse the response into memory operations
                List<MemoryOperation> operations = parseMemoryOperations(response);

                // Log the parsed operations
                if (!operations.isEmpty()) {
                    Log.i(TAG, "Memory Agent Analysis Results:");
                    for (MemoryOperation op : operations) {
                        Log.i(TAG, "  - " + op.getDescription());
                    }
                } else {
                    // Log exclusion rationale when no operations are performed
                    String exclusionRationale = parseExclusionRationale(response);
                    if (exclusionRationale != null && !exclusionRationale.isEmpty()) {
                        Log.i(TAG, "Memory Agent Exclusion Rationale: " + exclusionRationale);
                    } else {
                        Log.i(TAG, "Memory Agent: No operations performed (no rationale provided)");
                    }
                }
                
                Log.d(TAG, "Memory analysis complete: " + operations.size() + " operations");
                return operations;
                
            } catch (Exception e) {
                Log.e(TAG, "Error analyzing conversation for memory operations", e);
                return new ArrayList<>(); // Return empty list on error
            }
        }, executorService);
    }
    
    /**
     * Retrieve relevant memories for a given conversation context
     * 
     * @param userMessage The user's current message
     * @param conversationHistory Recent conversation history for context
     * @return List of relevant memories to include in the conversation
     */
    public CompletableFuture<List<String>> retrieveRelevantMemories(
            String userMessage, List<String> conversationHistory) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Retrieving relevant memories for conversation");
                
                // Build the retrieval prompt
                String retrievalPrompt = prompts.buildRetrievalPrompt(userMessage, conversationHistory);
                
                // Call LLM for memory retrieval guidance
                String response = callMemoryLLM(retrievalPrompt);
                
                // Parse the response into memory search queries
                List<String> searchQueries = parseSearchQueries(response);
                
                Log.d(TAG, "Memory retrieval analysis complete: " + searchQueries.size() + " queries");
                return searchQueries;
                
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving relevant memories", e);
                return new ArrayList<>(); // Return empty list on error
            }
        }, executorService);
    }
    
    /**
     * Call the LLM for memory-related operations
     * Uses GPT-4.1-nano specifically for memory operations
     */
    private String callMemoryLLM(String prompt) throws Exception {
        // Use memory-specific model (GPT-4.1-nano) and system prompt
        String systemPrompt = prompts.getMemorySystemPrompt();
        
        // Use the new method that allows specifying a specific model
        final String[] result = new String[1];
        final Exception[] error = new Exception[1];
        final Object lock = new Object();
        
        apiManager.sendMessageWithSpecificModel(
            com.genuwin.app.api.ApiConfig.DEFAULT_MEMORY_MODEL,
            prompt,
            systemPrompt,
            new ApiManager.MemoryAgentCallback() {
                @Override
                public void onResult(String response) {
                    synchronized (lock) {
                        result[0] = response;
                        lock.notify();
                    }
                }
                
                @Override
                public void onError(String errorMessage) {
                    synchronized (lock) {
                        error[0] = new Exception("Memory LLM error: " + errorMessage);
                        lock.notify();
                    }
                }
            }
        );
        
        // Wait for the async call to complete
        synchronized (lock) {
            try {
                lock.wait(30000); // 30 second timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Memory LLM call interrupted");
            }
        }
        
        if (error[0] != null) {
            throw error[0];
        }
        
        if (result[0] == null) {
            throw new Exception("Memory LLM call timed out");
        }
        
        return result[0];
    }
    
    /**
     * Parse LLM response into memory operations
     */
    private List<MemoryOperation> parseMemoryOperations(String response) {
        List<MemoryOperation> operations = new ArrayList<>();
        
        try {
            // Extract clean JSON from the response (handles markdown formatting)
            String cleanJson = extractJsonFromResponse(response);
            Log.d(TAG, "Extracted clean JSON: " + cleanJson);
            
            // The LLM should respond with a JSON array of operations
            JSONObject jsonResponse = new JSONObject(cleanJson);
            JSONArray operationsArray = jsonResponse.getJSONArray("operations");
            
            for (int i = 0; i < operationsArray.length(); i++) {
                JSONObject opJson = operationsArray.getJSONObject(i);
                MemoryOperation operation = parseMemoryOperation(opJson);
                
                if (operation != null && operation.isValid()) {
                    operations.add(operation);
                    Log.d(TAG, "Parsed valid operation: " + operation.getDescription());
                } else {
                    Log.w(TAG, "Skipped invalid operation: " + opJson.toString());
                }
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing memory operations JSON", e);
            // Try to extract operations from a more flexible format
            operations.addAll(parseMemoryOperationsFallback(response));
        }
        
        return operations;
    }
    
    /**
     * Parse a single memory operation from JSON
     */
    private MemoryOperation parseMemoryOperation(JSONObject opJson) throws JSONException {
        String type = opJson.getString("type");
        String reasoning = opJson.getString("reasoning");
        float confidence = (float) opJson.getDouble("confidence");
        
        switch (type.toUpperCase()) {
            case "CREATE":
                return parseCreateOperation(opJson, reasoning, confidence);
            case "UPDATE":
                return parseUpdateOperation(opJson, reasoning, confidence);
            case "MERGE":
                return parseMergeOperation(opJson, reasoning, confidence);
            default:
                Log.w(TAG, "Unknown operation type: " + type);
                return null;
        }
    }
    
    private CreateMemoryOperation parseCreateOperation(JSONObject opJson, String reasoning, float confidence) throws JSONException {
        String content = opJson.getString("content");
        String typeStr = opJson.getString("memoryType");
        float importance = (float) opJson.getDouble("importance");
        String metadata = opJson.optString("metadata", null);
        
        com.genuwin.app.memory.models.MemoryType memoryType = 
            com.genuwin.app.memory.models.MemoryType.valueOf(typeStr.toUpperCase());
        
        return new CreateMemoryOperation(content, memoryType, importance, reasoning, confidence, metadata);
    }
    
    private UpdateMemoryOperation parseUpdateOperation(JSONObject opJson, String reasoning, float confidence) throws JSONException {
        String memoryId = opJson.getString("memoryId");
        String newContent = opJson.optString("newContent", null);
        String typeStr = opJson.optString("newType", null);
        Float newImportance = opJson.has("newImportance") ? (float) opJson.getDouble("newImportance") : null;
        String newMetadata = opJson.optString("newMetadata", null);
        
        com.genuwin.app.memory.models.MemoryType newType = null;
        if (typeStr != null && !typeStr.isEmpty()) {
            newType = com.genuwin.app.memory.models.MemoryType.valueOf(typeStr.toUpperCase());
        }
        
        return new UpdateMemoryOperation(memoryId, newContent, newType, newImportance, reasoning, confidence, newMetadata);
    }
    
    private MergeMemoryOperation parseMergeOperation(JSONObject opJson, String reasoning, float confidence) throws JSONException {
        JSONArray sourceIds = opJson.getJSONArray("sourceMemoryIds");
        List<String> sourceMemoryIds = new ArrayList<>();
        for (int i = 0; i < sourceIds.length(); i++) {
            sourceMemoryIds.add(sourceIds.getString(i));
        }
        
        String mergedContent = opJson.getString("mergedContent");
        String typeStr = opJson.getString("mergedType");
        float mergedImportance = (float) opJson.getDouble("mergedImportance");
        String mergedMetadata = opJson.optString("mergedMetadata", null);
        boolean deleteSource = opJson.optBoolean("deleteSourceMemories", true);
        
        com.genuwin.app.memory.models.MemoryType mergedType = 
            com.genuwin.app.memory.models.MemoryType.valueOf(typeStr.toUpperCase());
        
        return new MergeMemoryOperation(sourceMemoryIds, mergedContent, mergedType, 
                                      mergedImportance, reasoning, confidence, mergedMetadata, deleteSource);
    }
    
    /**
     * Extract clean JSON from LLM response, handling markdown formatting and other wrappers
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }
        
        String trimmed = response.trim();
        
        // Handle markdown code blocks (```json ... ```)
        if (trimmed.startsWith("```json") && trimmed.endsWith("```")) {
            // Extract content between ```json and ```
            int startIndex = trimmed.indexOf("```json") + 7; // Length of "```json"
            int endIndex = trimmed.lastIndexOf("```");
            if (startIndex < endIndex) {
                trimmed = trimmed.substring(startIndex, endIndex).trim();
            }
        }
        
        // Handle generic code blocks (``` ... ```)
        else if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            // Extract content between ``` and ```
            int startIndex = trimmed.indexOf("```") + 3;
            int endIndex = trimmed.lastIndexOf("```");
            if (startIndex < endIndex) {
                trimmed = trimmed.substring(startIndex, endIndex).trim();
            }
        }
        
        // Look for JSON object in the text if it doesn't start with {
        if (!trimmed.startsWith("{")) {
            // Try to find JSON object within the text
            int jsonStart = trimmed.indexOf("{");
            if (jsonStart != -1) {
                // Find the matching closing brace
                int braceCount = 0;
                int jsonEnd = -1;
                for (int i = jsonStart; i < trimmed.length(); i++) {
                    char c = trimmed.charAt(i);
                    if (c == '{') {
                        braceCount++;
                    } else if (c == '}') {
                        braceCount--;
                        if (braceCount == 0) {
                            jsonEnd = i + 1;
                            break;
                        }
                    }
                }
                
                if (jsonEnd != -1) {
                    trimmed = trimmed.substring(jsonStart, jsonEnd);
                }
            }
        }
        
        // If still no valid JSON found, return empty operations object
        if (!trimmed.startsWith("{")) {
            Log.w(TAG, "Could not extract JSON from response, returning empty operations");
            return "{\"operations\": []}";
        }
        
        return trimmed;
    }
    
    /**
     * Fallback parsing for when JSON parsing fails
     * Implements more flexible parsing for cases where LLM doesn't return perfect JSON
     */
    private List<MemoryOperation> parseMemoryOperationsFallback(String response) {
        List<MemoryOperation> operations = new ArrayList<>();
        
        Log.d(TAG, "Attempting fallback parsing for response: " + response);
        
        try {
            // Try to extract JSON again with more aggressive cleaning
            String cleanedResponse = extractJsonFromResponse(response);
            
            // Try parsing the cleaned response
            JSONObject jsonResponse = new JSONObject(cleanedResponse);
            if (jsonResponse.has("operations")) {
                JSONArray operationsArray = jsonResponse.getJSONArray("operations");
                
                for (int i = 0; i < operationsArray.length(); i++) {
                    try {
                        JSONObject opJson = operationsArray.getJSONObject(i);
                        MemoryOperation operation = parseMemoryOperation(opJson);
                        
                        if (operation != null && operation.isValid()) {
                            operations.add(operation);
                            Log.d(TAG, "Fallback parsed valid operation: " + operation.getDescription());
                        }
                    } catch (JSONException e) {
                        Log.w(TAG, "Fallback: Skipped invalid operation at index " + i + ": " + e.getMessage());
                    }
                }
            }
            
        } catch (JSONException e) {
            Log.w(TAG, "Fallback parsing also failed: " + e.getMessage());
            
            // Last resort: try to parse simple text patterns
            operations.addAll(parseOperationsFromText(response));
        }
        
        Log.d(TAG, "Fallback parsing extracted " + operations.size() + " operations");
        return operations;
    }
    
    /**
     * Last resort: try to parse operations from plain text patterns
     */
    private List<MemoryOperation> parseOperationsFromText(String response) {
        List<MemoryOperation> operations = new ArrayList<>();
        
        // Look for common patterns like "CREATE:", "UPDATE:", etc.
        String[] lines = response.split("\n");
        for (String line : lines) {
            String trimmed = line.trim().toLowerCase();
            
            // For now, just log what we found - this could be expanded later
            if (trimmed.contains("create") || trimmed.contains("update") || 
                trimmed.contains("merge")) {
                Log.d(TAG, "Found potential operation in text: " + line);
            }
        }
        
        // For now, return empty list - this could be implemented more robustly later
        Log.d(TAG, "Text pattern parsing not fully implemented yet");
        return operations;
    }
    
    /**
     * Parse exclusion rationale from LLM response when no operations are performed
     */
    private String parseExclusionRationale(String response) {
        try {
            // Extract clean JSON from the response
            String cleanJson = extractJsonFromResponse(response);
            JSONObject jsonResponse = new JSONObject(cleanJson);
            
            // Look for exclusionRationale field
            if (jsonResponse.has("exclusionRationale")) {
                return jsonResponse.getString("exclusionRationale");
            }
        } catch (JSONException e) {
            Log.d(TAG, "Could not parse exclusion rationale from JSON, trying text fallback");
            
            // Fallback: look for rationale in text
            String[] lines = response.split("\n");
            for (String line : lines) {
                String trimmed = line.trim().toLowerCase();
                if (trimmed.contains("rationale") || trimmed.contains("reason") || 
                    trimmed.contains("because") || trimmed.contains("no operations")) {
                    return line.trim();
                }
            }
        }
        
        return null;
    }
    
    /**
     * Parse search queries from LLM response
     */
    private List<String> parseSearchQueries(String response) {
        List<String> queries = new ArrayList<>();
        
        try {
            // Extract clean JSON from the response (handles markdown formatting)
            String cleanJson = extractJsonFromResponse(response);
            Log.d(TAG, "Extracted clean JSON for search queries: " + cleanJson);
            
            JSONObject jsonResponse = new JSONObject(cleanJson);
            JSONArray queriesArray = jsonResponse.getJSONArray("searchQueries");
            
            for (int i = 0; i < queriesArray.length(); i++) {
                String query = queriesArray.getString(i);
                if (query != null && !query.trim().isEmpty()) {
                    queries.add(query.trim());
                }
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing search queries JSON", e);
            // Fallback: try to extract queries from text
            queries.addAll(parseSearchQueriesFallback(response));
        }
        
        return queries;
    }
    
    /**
     * Fallback parsing for search queries
     */
    private List<String> parseSearchQueriesFallback(String response) {
        List<String> queries = new ArrayList<>();
        
        // Simple fallback: split by lines and take non-empty lines as queries
        String[] lines = response.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                queries.add(trimmed);
            }
        }
        
        return queries;
    }
    
    /**
     * Check if memory operations are enabled in settings
     */
    public boolean isMemoryEnabled() {
        return settingsManager.isMemoryEnabled();
    }
    
    /**
     * Get the minimum importance threshold for saving memories
     */
    public float getImportanceThreshold() {
        return settingsManager.getMemoryImportanceThreshold();
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        Log.d(TAG, "MemoryAgent cleaned up");
    }
}

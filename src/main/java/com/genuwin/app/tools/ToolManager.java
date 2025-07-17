package com.genuwin.app.tools;

import android.content.Context;
import android.util.Log;

import com.genuwin.app.tools.impl.GoogleCalendarTool;
import com.genuwin.app.tools.impl.GoogleDriveTool;
import com.genuwin.app.tools.impl.HomeAssistantTool;
import com.genuwin.app.tools.impl.SearchTool;
import com.genuwin.app.tools.impl.TemperatureTool;
import com.genuwin.app.tools.impl.WeatherTool;
import com.genuwin.app.tools.impl.WikipediaTool;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Central manager for the new tools system
 * Handles tool registration, execution, and system prompt generation
 */
public class ToolManager {
    private static final String TAG = "ToolManager";
    
    private static ToolManager instance;
    private final Context context;
    private final Map<String, ToolExecutor> toolExecutors;
    private final ToolCallParser toolCallParser;
    
    // Tool execution tracking
    private int toolCallCount = 0;
    private static final int MAX_TOOL_CALLS = 3;
    private String lastToolUsed = null;
    private boolean isToolCallActive = false;
    
    private ToolManager(Context context) {
        this.context = context.getApplicationContext();
        this.toolExecutors = new HashMap<>();
        this.toolCallParser = new ToolCallParser();
        
        initializeTools();
    }
    
    public static synchronized ToolManager getInstance(Context context) {
        if (instance == null) {
            instance = new ToolManager(context);
        }
        return instance;
    }
    
    /**
     * Initialize all available tools
     */
    private void initializeTools() {
        Log.d(TAG, "Initializing tools...");

        Properties properties = new Properties();
        try (InputStream is = context.getAssets().open("config.properties")) {
            properties.load(is);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load config.properties", e);
        }

        String enabledToolsProp = properties.getProperty("enabled_tools", "");
        List<String> enabledTools = Arrays.asList(enabledToolsProp.split(","));

        if (enabledTools.contains("SearchTool")) {
            registerTool(new SearchTool(context));
        }
        if (enabledTools.contains("WeatherTool")) {
            registerTool(new WeatherTool(context));
        }
        if (enabledTools.contains("HomeAssistantTool")) {
            registerTool(new HomeAssistantTool(context));
        }
        if (enabledTools.contains("TemperatureTool")) {
            registerTool(new TemperatureTool(context));
        }
        if (enabledTools.contains("WikipediaTool")) {
            registerTool(new WikipediaTool(context));
        }
        if (enabledTools.contains("GoogleCalendarTool")) {
            registerTool(new GoogleCalendarTool(context));
        }
        if (enabledTools.contains("GoogleDriveTool")) {
            registerTool(new GoogleDriveTool(context));
        }

        Log.d(TAG, "Initialized " + toolExecutors.size() + " tools");
    }
    
    /**
     * Register a tool executor
     */
    public void registerTool(ToolExecutor executor) {
        String toolName = executor.getToolDefinition().getName();
        toolExecutors.put(toolName, executor);
        Log.d(TAG, "Registered tool: " + toolName);
    }
    
    /**
     * Generate system prompt with tool descriptions
     */
    public String generateToolSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("\n\n=== TOOL USAGE GUIDE ===\n");
        sb.append("You can use tools when needed, but respond conversationally for most requests.\n\n");
        
        sb.append("CONVERSATIONAL (No tools): Simple greetings, basic jokes, personal opinions, casual chat\n");
        sb.append("TOOLS REQUIRED: Device control, weather, calendar management, web searches for factual information\n\n");
        
        sb.append("AVAILABLE TOOLS:\n");
        for (ToolExecutor executor : toolExecutors.values()) {
            ToolDefinition def = executor.getToolDefinition();
            sb.append("- ").append(def.getSystemPromptDescription()).append("\n");
        }
        
        sb.append("\nEXAMPLES:\n");
        sb.append("'Tell me a joke' → Respond with a joke directly\n");
        sb.append("'Turn off lights' → {\"tool\": \"home_assistant_control\", \"parameters\": {\"action\": \"turn_off\", \"entity_id\": \"lights\"}}\n");
        sb.append("'What's the weather?' → {\"tool\": \"get_current_weather\", \"parameters\": {\"location\": \"current\"}}\n");
        sb.append("'Who is the current president?' → {\"tool\": \"search_wikipedia\", \"parameters\": {\"query\": \"current president of the united states\"}}\n");
        sb.append("'Search for information about climate change' → {\"tool\": \"searxng_search\", \"parameters\": {\"query\": \"climate change latest research\"}}\n");
        sb.append("'What is on my calendar this week?' → {\"tool\": \"google_calendar\", \"parameters\": {\"action\": \"list_events\", \"timeMin\": \"this_week_start\", \"timeMax\": \"this_week_end\"}}\n");
        sb.append("'Schedule a meeting for tomorrow at 2pm' → {\"tool\": \"google_calendar\", \"parameters\": {\"action\": \"create_event\", \"summary\": \"Meeting\", \"startTime\": \"...\", \"endTime\": \"...\"}}\n\n");
        
        sb.append("KEY: Use tools when they would provide better, more current, or more comprehensive information than your training data. Default to conversation for simple interactions.\n");
        
        return sb.toString();
    }
    
    /**
     * Parse and execute tool calls from LLM response
     */
    public void processResponse(String response, ToolProcessingCallback callback) {
        isToolCallActive = true;
        if (response == null || response.trim().isEmpty()) {
            callback.onNoToolCalls(response);
            isToolCallActive = false;
            return;
        }
        
        List<ToolCallParser.ParsedToolCall> toolCalls = toolCallParser.parseToolCalls(response);
        
        if (toolCalls.isEmpty()) {
            // No tool calls found, treat as regular response
            callback.onNoToolCalls(response);
            isToolCallActive = false;
            return;
        }
        
        // Check tool call limits
        toolCallCount++;
        Log.d(TAG, "Tool call count: " + toolCallCount + "/" + MAX_TOOL_CALLS);
        
        if (toolCallCount > MAX_TOOL_CALLS) {
            Log.w(TAG, "Tool call limit exceeded, forcing response");
            callback.onToolCallLimitExceeded();
            isToolCallActive = false;
            return;
        }
        
        // Process tool calls
        processToolCalls(toolCalls, response, callback);
    }
    
    /**
     * Process a list of tool calls
     */
    private void processToolCalls(List<ToolCallParser.ParsedToolCall> toolCalls, 
                                 String originalResponse, ToolProcessingCallback callback) {
        
        if (toolCalls.size() == 1) {
            // Single tool call - process directly
            ToolCallParser.ParsedToolCall toolCall = toolCalls.get(0);
            executeSingleToolCall(toolCall, originalResponse, callback);
        } else {
            // Multiple tool calls - process sequentially
            processMultipleToolCalls(toolCalls, originalResponse, callback, 0, new ArrayList<>());
        }
    }
    
    /**
     * Execute a single tool call
     */
    private void executeSingleToolCall(ToolCallParser.ParsedToolCall toolCall, 
                                     String originalResponse, ToolProcessingCallback callback) {
        
        String toolName = toolCall.getToolName();
        
        // Check for repeated tool calls
        if (toolName.equals(lastToolUsed) && toolCallCount > 1) {
            Log.w(TAG, "Repeated tool call detected: " + toolName);
            callback.onRepeatedToolCall(toolName);
            return;
        }
        
        lastToolUsed = toolName;
        
        ToolExecutor executor = toolExecutors.get(toolName);
        if (executor == null) {
            Log.w(TAG, "Unknown tool: " + toolName);
            callback.onUnknownTool(toolName);
            return;
        }
        
        // Validate parameters
        ToolExecutor.ValidationResult validation = executor.validateParameters(toolCall.getParameters());
        if (!validation.isValid()) {
            Log.w(TAG, "Invalid parameters for tool " + toolName + ": " + validation.getErrorMessage());
            callback.onInvalidParameters(toolName, validation.getErrorMessage());
            return;
        }
        
        Log.d(TAG, "Executing tool: " + toolName + " with parameters: " + toolCall.getParameters());
        
        // Execute the tool
        executor.execute(toolCall.getParameters(), new ToolExecutor.ToolExecutionCallback() {
            @Override
            public void onSuccess(String result) {
                // Remove tool call JSON from original response
                String cleanResponse = toolCallParser.removeToolCallsFromText(originalResponse);
                callback.onToolCallSuccess(toolCall.getId(), toolName, result, cleanResponse);
                isToolCallActive = false;
            }
            
            @Override
            public void onError(String error) {
                callback.onToolCallError(toolName, error);
                isToolCallActive = false;
            }
            
            @Override
            public void onConfirmationRequired(String message, Runnable onConfirm, Runnable onCancel) {
                callback.onConfirmationRequired(toolName, message, onConfirm, onCancel);
            }
        });
    }
    
    /**
     * Process multiple tool calls sequentially
     */
    private void processMultipleToolCalls(List<ToolCallParser.ParsedToolCall> toolCalls,
                                        String originalResponse, ToolProcessingCallback callback,
                                        int currentIndex, List<String> results) {
        
        if (currentIndex >= toolCalls.size()) {
            // All tool calls processed
            String cleanResponse = toolCallParser.removeToolCallsFromText(originalResponse);
            callback.onMultipleToolCallsComplete(results, cleanResponse);
            isToolCallActive = false;
            return;
        }
        
        ToolCallParser.ParsedToolCall toolCall = toolCalls.get(currentIndex);
        String toolName = toolCall.getToolName();
        
        ToolExecutor executor = toolExecutors.get(toolName);
        if (executor == null) {
            results.add("Error: Unknown tool " + toolName);
            processMultipleToolCalls(toolCalls, originalResponse, callback, currentIndex + 1, results);
            return;
        }
        
        executor.execute(toolCall.getParameters(), new ToolExecutor.ToolExecutionCallback() {
            @Override
            public void onSuccess(String result) {
                results.add(result);
                processMultipleToolCalls(toolCalls, originalResponse, callback, currentIndex + 1, results);
            }
            
            @Override
            public void onError(String error) {
                results.add("Error: " + error);
                processMultipleToolCalls(toolCalls, originalResponse, callback, currentIndex + 1, results);
            }
            
            @Override
            public void onConfirmationRequired(String message, Runnable onConfirm, Runnable onCancel) {
                // For multiple tool calls, auto-confirm for now
                // TODO: Implement proper confirmation handling for multiple tools
                onConfirm.run();
            }
        });
    }
    
    /**
     * Reset tool call tracking (call this for new user messages)
     */
    public void resetToolCallTracking() {
        toolCallCount = 0;
        lastToolUsed = null;
        isToolCallActive = false;
        Log.d(TAG, "Reset tool call tracking");
    }

    public boolean isToolCallActive() {
        return isToolCallActive;
    }
    
    /**
     * Check if response contains tool calls
     */
    public boolean containsToolCalls(String response) {
        return toolCallParser.containsToolCalls(response);
    }
    
    /**
     * Get list of available tool names
     */
    public List<String> getAvailableToolNames() {
        return new ArrayList<>(toolExecutors.keySet());
    }
    
    /**
     * Get tool definition by name
     */
    public ToolDefinition getToolDefinition(String toolName) {
        ToolExecutor executor = toolExecutors.get(toolName);
        return executor != null ? executor.getToolDefinition() : null;
    }
    
    /**
     * Get tool definitions in OpenAI API format for use with LLM function calling
     */
    public List<Map<String, Object>> getToolDefinitionsForAPI() {
        List<Map<String, Object>> toolDefinitions = new ArrayList<>();
        
        for (ToolExecutor executor : toolExecutors.values()) {
            ToolDefinition def = executor.getToolDefinition();
            
            Map<String, Object> tool = new HashMap<>();
            tool.put("type", "function");
            tool.put("name", def.getName());
            tool.put("description", def.getDescription());
            
            // Build parameters schema
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "object");
            parameters.put("additionalProperties", false);
            
            Map<String, Object> properties = new HashMap<>();
            List<String> required = new ArrayList<>();
            
            for (Map.Entry<String, ToolDefinition.ParameterDefinition> entry : def.getParameters().entrySet()) {
                String paramName = entry.getKey();
                ToolDefinition.ParameterDefinition paramDef = entry.getValue();
                
                Map<String, Object> property = new HashMap<>();
                property.put("type", paramDef.getType());
                property.put("description", paramDef.getDescription());
                
                properties.put(paramName, property);
                
                if (paramDef.isRequired()) {
                    required.add(paramName);
                }
            }
            
            parameters.put("properties", properties);
            if (!required.isEmpty()) {
                parameters.put("required", required);
            }
            
            tool.put("parameters", parameters);
            
            toolDefinitions.add(tool);
        }
        
        Log.d(TAG, "Generated " + toolDefinitions.size() + " tool definitions for API");
        return toolDefinitions;
    }
    
    /**
     * Callback interface for tool processing results
     */
    public interface ToolProcessingCallback {
        /**
         * Called when no tool calls are found in the response
         */
        void onNoToolCalls(String response);
        
        /**
         * Called when a single tool call succeeds
         */
        void onToolCallSuccess(String toolCallId, String toolName, String result, String cleanResponse);
        
        /**
         * Called when a tool call fails
         */
        void onToolCallError(String toolName, String error);
        
        /**
         * Called when multiple tool calls complete
         */
        void onMultipleToolCallsComplete(List<String> results, String cleanResponse);
        
        /**
         * Called when tool call limit is exceeded
         */
        void onToolCallLimitExceeded();
        
        /**
         * Called when a repeated tool call is detected
         */
        void onRepeatedToolCall(String toolName);
        
        /**
         * Called when an unknown tool is requested
         */
        void onUnknownTool(String toolName);
        
        /**
         * Called when tool parameters are invalid
         */
        void onInvalidParameters(String toolName, String error);
        
        /**
         * Called when tool execution requires user confirmation
         */
        void onConfirmationRequired(String toolName, String message, Runnable onConfirm, Runnable onCancel);
    }
}

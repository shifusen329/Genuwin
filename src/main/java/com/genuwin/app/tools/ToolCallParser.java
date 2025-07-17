package com.genuwin.app.tools;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses tool calls from LLM response text using JSON format
 */
public class ToolCallParser {
    private static final String TAG = "ToolCallParser";
    
    // JSON tool call pattern: {"tool": "tool_name", "parameters": {...}}
    private static final Pattern JSON_TOOL_PATTERN = Pattern.compile(
        "\\{\\s*\"tool\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"parameters\"\\s*:\\s*\\{[^}]*\\}\\s*\\}",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Alternative pattern for natural format: {"name": "tool_name", "arguments": "..." or {...}}
    private static final Pattern ALT_JSON_PATTERN = Pattern.compile(
        "\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"arguments\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Function call pattern: {"function": "name", "args": {...}}
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "\\{\\s*\"function\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"args\"\\s*:\\s*\\{[^}]*\\}\\s*\\}",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Fallback pattern for natural language tool calls
    private static final Pattern NATURAL_PATTERN = Pattern.compile(
        "(?:I need to|Let me|I'll)\\s+(?:use|call|execute)\\s+(?:the\\s+)?(\\w+)\\s+(?:tool|function)\\s+(?:with|to)\\s+(.+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private final Gson gson;
    
    public ToolCallParser() {
        this.gson = new Gson();
    }
    
    /**
     * Parse tool calls from LLM response text
     */
    public List<ParsedToolCall> parseToolCalls(String responseText) {
        List<ParsedToolCall> toolCalls = new ArrayList<>();
        
        if (responseText == null || responseText.trim().isEmpty()) {
            return toolCalls;
        }
        
        // Try JSON format first
        toolCalls.addAll(parseJsonToolCalls(responseText));
        
        // If no JSON tool calls found, try natural language
        if (toolCalls.isEmpty()) {
            toolCalls.addAll(parseNaturalLanguageToolCalls(responseText));
        }
        
        return toolCalls;
    }
    
    /**
     * Parse JSON-formatted tool calls
     */
    private List<ParsedToolCall> parseJsonToolCalls(String text) {
        List<ParsedToolCall> toolCalls = new ArrayList<>();
        
        // Primary JSON format: {"tool": "name", "parameters": {...}}
        Matcher matcher = JSON_TOOL_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                String jsonStr = matcher.group(0);
                JsonObject jsonObj = JsonParser.parseString(jsonStr).getAsJsonObject();
                
                String toolName = jsonObj.get("tool").getAsString();
                JsonObject parameters = jsonObj.getAsJsonObject("parameters");
                
                ParsedToolCall toolCall = new ParsedToolCall(
                    generateToolCallId(),
                    toolName,
                    parameters,
                    jsonStr
                );
                
                toolCalls.add(toolCall);
                
            } catch (JsonSyntaxException e) {
                Log.w(TAG, "Failed to parse JSON tool call: " + matcher.group(0), e);
            }
        }
        
        // Alternative JSON format: {"name": "tool_name", "arguments": "..."}
        if (toolCalls.isEmpty()) {
            Matcher altMatcher = ALT_JSON_PATTERN.matcher(text);
            while (altMatcher.find()) {
                try {
                    String toolName = altMatcher.group(1);
                    String argumentsStr = altMatcher.group(2);
                    
                    // Map tool names to our actual tool names
                    String mappedToolName = mapToolName(toolName);
                    
                    // Parse the arguments string into parameters
                    JsonObject parameters = parseArgumentsString(argumentsStr, mappedToolName);
                    
                    ParsedToolCall toolCall = new ParsedToolCall(
                        generateToolCallId(),
                        mappedToolName,
                        parameters,
                        altMatcher.group(0)
                    );
                    
                    toolCalls.add(toolCall);
                    
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse natural format tool call: " + altMatcher.group(0), e);
                }
            }
        }
        
        // Function call format: {"function": "name", "args": {...}}
        if (toolCalls.isEmpty()) {
            Matcher funcMatcher = FUNCTION_PATTERN.matcher(text);
            while (funcMatcher.find()) {
                try {
                    String jsonStr = funcMatcher.group(0);
                    JsonObject jsonObj = JsonParser.parseString(jsonStr).getAsJsonObject();
                    
                    String toolName = jsonObj.get("function").getAsString();
                    JsonObject parameters = jsonObj.getAsJsonObject("args");
                    
                    // Map tool names to our actual tool names
                    String mappedToolName = mapToolName(toolName);
                    
                    ParsedToolCall toolCall = new ParsedToolCall(
                        generateToolCallId(),
                        mappedToolName,
                        parameters,
                        jsonStr
                    );
                    
                    toolCalls.add(toolCall);
                    
                } catch (JsonSyntaxException e) {
                    Log.w(TAG, "Failed to parse function call: " + funcMatcher.group(0), e);
                }
            }
        }
        
        return toolCalls;
    }
    
    /**
     * Parse natural language tool calls as fallback
     */
    private List<ParsedToolCall> parseNaturalLanguageToolCalls(String text) {
        List<ParsedToolCall> toolCalls = new ArrayList<>();
        
        Matcher matcher = NATURAL_PATTERN.matcher(text);
        while (matcher.find()) {
            String toolName = matcher.group(1);
            String argsText = matcher.group(2);
            
            // Try to extract parameters from natural language
            JsonObject parameters = parseNaturalLanguageArgs(argsText);
            
            ParsedToolCall toolCall = new ParsedToolCall(
                generateToolCallId(),
                toolName,
                parameters,
                matcher.group(0)
            );
            
            toolCalls.add(toolCall);
        }
        
        return toolCalls;
    }
    
    /**
     * Extract parameters from natural language text
     */
    private JsonObject parseNaturalLanguageArgs(String argsText) {
        JsonObject parameters = new JsonObject();
        
        // Simple heuristics for common patterns
        if (argsText.contains("search for") || argsText.contains("query")) {
            // Extract search query
            String query = argsText.replaceAll(".*(?:search for|query)\\s+[\"']?([^\"']+)[\"']?.*", "$1");
            parameters.addProperty("query", query.trim());
        } else if (argsText.contains("turn on") || argsText.contains("turn off")) {
            // Extract entity for home assistant
            String entity = argsText.replaceAll(".*(?:turn (?:on|off))\\s+(?:the\\s+)?([^\\s]+).*", "$1");
            parameters.addProperty("entity_id", entity.trim());
        } else if (argsText.contains("temperature") && argsText.matches(".*\\d+.*")) {
            // Extract temperature setting
            String tempStr = argsText.replaceAll(".*?(\\d+).*", "$1");
            try {
                int temperature = Integer.parseInt(tempStr);
                parameters.addProperty("temperature", temperature);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Failed to parse temperature: " + tempStr);
            }
        } else {
            // Default: use the whole text as a query parameter
            parameters.addProperty("query", argsText.trim());
        }
        
        return parameters;
    }
    
    /**
     * Parse arguments string into JsonObject parameters with tool context
     */
    private JsonObject parseArgumentsString(String argumentsStr, String toolName) {
        JsonObject parameters = new JsonObject();
        
        // Remove outer quotes if present
        // Remove outer quotes if present
        argumentsStr = argumentsStr.replaceAll("^['\"]|['\"]$", "");
        
        // CRITICAL FIX: Unescape JSON quotes before parsing
        // CRITICAL FIX: Unescape JSON quotes before parsing
        argumentsStr = argumentsStr.replace("\\\"", "\"");
        
        // Try to parse as JSON first (for OpenAI function call format)
        // Try to parse as JSON first (for OpenAI function call format)
        if (argumentsStr.startsWith("{") && argumentsStr.endsWith("}")) {
            try {
                JsonObject jsonArgs = JsonParser.parseString(argumentsStr).getAsJsonObject();
                
                // Copy all properties directly without tool-specific mutations
                // The OpenAI function calls already have the correct parameter names and values
                for (String key : jsonArgs.keySet()) {
                    parameters.add(key, jsonArgs.get(key));
                }
                
                return parameters;
                
            } catch (JsonSyntaxException e) {
                // Fall through to string parsing
            }
        }
        
        // Handle legacy string argument patterns (fallback for non-JSON formats)
        if (argumentsStr.contains("room:")) {
            // Extract room parameter: "room: bedroom" -> {"entity_id": "bedroom_lights", "action": "turn_on"}
            String room = argumentsStr.replaceAll(".*room:\\s*([^,\\s]+).*", "$1").trim();
            parameters.addProperty("entity_id", room + "_lights");
            parameters.addProperty("action", "turn_on");
        } else if (argumentsStr.contains("entity:")) {
            // Extract entity parameter: "entity: bedroom_lights"
            String entity = argumentsStr.replaceAll(".*entity:\\s*([^,\\s]+).*", "$1").trim();
            parameters.addProperty("entity_id", entity);
            parameters.addProperty("action", "turn_on");
        } else if (argumentsStr.contains("query:")) {
            // Extract search query: "query: weather today"
            String query = argumentsStr.replaceAll(".*query:\\s*(.+)", "$1").trim();
            parameters.addProperty("query", query);
        } else {
            // Default behavior based on tool type (only for non-JSON legacy formats)
            if ("get_current_weather".equals(toolName)) {
                // For weather tool, treat as location
                parameters.addProperty("location", argumentsStr.trim());
            } else if ("searxng_search".equals(toolName)) {
                // For search tool, treat as query
                parameters.addProperty("query", argumentsStr.trim());
            } else if ("home_assistant_control".equals(toolName)) {
                // For HA tools, treat as entity_id with default action
                parameters.addProperty("entity_id", argumentsStr.trim());
                parameters.addProperty("action", "turn_on");
            } else {
                // Generic fallback
                parameters.addProperty("value", argumentsStr.trim());
            }
        }
        
        return parameters;
    }
    
    /**
     * Map AI-generated tool names to our actual tool names
     */
    private String mapToolName(String aiToolName) {
        if (aiToolName == null) return "unknown";
        
        String lowerName = aiToolName.toLowerCase().trim();
        
        if (lowerName.equals("search_gdrive")) {
            return "search_gdrive";
        }

        // Map various light control names to home assistant tool
        if (lowerName.contains("light") || lowerName.contains("turn_on") || lowerName.contains("turn_off") ||
            lowerName.contains("switch") || lowerName.contains("device")) {
            return "home_assistant_control";
        }
        
        // Map search-related names to search tool
        if (lowerName.contains("search") || lowerName.contains("query") || lowerName.contains("find")) {
            return "searxng_search";
        }
        
        // Map temperature-related names to temperature tool
        if (lowerName.contains("temperature") || lowerName.contains("thermostat") || lowerName.contains("climate")) {
            return "set_temperature";
        }
        
        // Default: return as-is and let the tool manager handle unknown tools
        return aiToolName;
    }
    
    /**
     * Generate a unique tool call ID
     */
    private String generateToolCallId() {
        return "tool_call_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    /**
     * Check if text contains potential tool calls
     */
    public boolean containsToolCalls(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        return JSON_TOOL_PATTERN.matcher(text).find() || 
               ALT_JSON_PATTERN.matcher(text).find() ||
               FUNCTION_PATTERN.matcher(text).find() ||
               NATURAL_PATTERN.matcher(text).find();
    }
    
    /**
     * Remove tool call JSON from response text to get clean response
     */
    public String removeToolCallsFromText(String text) {
        if (text == null) return null;
        
        String cleaned = text;
        
        // Remove JSON tool calls
        cleaned = JSON_TOOL_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = ALT_JSON_PATTERN.matcher(cleaned).replaceAll("");
        
        // Clean up extra whitespace
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }
    
    /**
     * Represents a parsed tool call
     */
    public static class ParsedToolCall {
        private final String id;
        private final String toolName;
        private final JsonObject parameters;
        private final String originalText;
        
        public ParsedToolCall(String id, String toolName, JsonObject parameters, String originalText) {
            this.id = id;
            this.toolName = toolName;
            this.parameters = parameters;
            this.originalText = originalText;
        }
        
        public String getId() {
            return id;
        }
        
        public String getToolName() {
            return toolName;
        }
        
        public JsonObject getParameters() {
            return parameters;
        }
        
        public String getOriginalText() {
            return originalText;
        }
        
        public String getParameterAsString(String key) {
            if (parameters.has(key)) {
                return parameters.get(key).getAsString();
            }
            return null;
        }
        
        public Integer getParameterAsInt(String key) {
            if (parameters.has(key)) {
                try {
                    return parameters.get(key).getAsInt();
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        }
        
        public Double getParameterAsDouble(String key) {
            if (parameters.has(key)) {
                try {
                    return parameters.get(key).getAsDouble();
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        }
        
        @Override
        public String toString() {
            return "ParsedToolCall{" +
                    "id='" + id + '\'' +
                    ", toolName='" + toolName + '\'' +
                    ", parameters=" + parameters +
                    '}';
        }
    }
}

package com.genuwin.app.tools.impl;

import android.content.Context;
import android.util.Log;

import com.genuwin.app.searxng.SearxngManager;
import com.genuwin.app.tools.ToolDefinition;
import com.genuwin.app.tools.ToolExecutor;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Weather tool implementation using SearXNG for weather searches
 */
public class WeatherTool implements ToolExecutor {
    private static final String TAG = "WeatherTool";
    
    private final Context context;
    private final SearxngManager searxngManager;
    private final ToolDefinition toolDefinition;
    
    public WeatherTool(Context context) {
        this.context = context;
        this.searxngManager = new SearxngManager(context);
        this.toolDefinition = createToolDefinition();
    }
    
    private ToolDefinition createToolDefinition() {
        Map<String, ToolDefinition.ParameterDefinition> parameters = new HashMap<>();
        
        parameters.put("location", new ToolDefinition.ParameterDefinition(
            "string", 
            "Location to get weather for (city name, city,country, or coordinates)", 
            true
        ));
        
        return new ToolDefinition(
            "get_current_weather",
            "Get current weather information for a specified location. Use this when users ask about weather conditions, temperature, or forecast.",
            parameters,
            "weather",
            false
        );
    }
    
    @Override
    public void execute(JsonObject parameters, ToolExecutionCallback callback) {
        Log.d(TAG, "Executing weather search with parameters: " + parameters);
        
        String location = parameters.get("location").getAsString();
        
        // Create a weather-specific search query
        String weatherQuery = "current weather " + location + " temperature conditions";
        
        // Create search parameters for SearXNG
        JsonObject searchParams = new JsonObject();
        searchParams.addProperty("query", weatherQuery);
        searchParams.addProperty("categories", "general");
        searchParams.addProperty("language", "en");
        searchParams.addProperty("limit", 10);
        
        Log.d(TAG, "Searching for weather: " + weatherQuery);
        
        searxngManager.search(searchParams, new SearxngManager.SearxngCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Weather search completed successfully");
                // Format the response to be more weather-focused
                String formattedResponse = formatWeatherResponse(location, response);
                callback.onSuccess(formattedResponse);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Weather search failed: " + error);
                callback.onError("Weather search failed: " + error);
            }
        });
    }
    
    private String formatWeatherResponse(String location, String searchResponse) {
        StringBuilder formatted = new StringBuilder();
        
        // Provide natural language instruction to the AI
        formatted.append("I found current weather information for ").append(location).append(". ");
        formatted.append("Based on the search results below, please provide a natural, conversational summary of the current weather conditions. ");
        formatted.append("Do not generate mock data - use only the information from these real weather sources:\n\n");
        
        // Add the search results
        if (searchResponse.contains("SEARXNG_RESULTS:")) {
            formatted.append(searchResponse);
        } else {
            formatted.append(searchResponse);
        }
        
        formatted.append("\n\nPlease summarize this weather information in a natural, friendly way without generating any JSON or mock data.");
        
        return formatted.toString();
    }
    
    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }
    
    @Override
    public ValidationResult validateParameters(JsonObject parameters) {
        if (parameters == null) {
            return ValidationResult.invalid("Parameters cannot be null");
        }
        
        if (!parameters.has("location")) {
            return ValidationResult.invalid("Missing required parameter: location");
        }
        
        String location = parameters.get("location").getAsString();
        if (location == null || location.trim().isEmpty()) {
            return ValidationResult.invalid("Location cannot be empty");
        }
        
        return ValidationResult.valid();
    }
}

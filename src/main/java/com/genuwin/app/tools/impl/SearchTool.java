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
 * Search tool implementation using SearXNG
 */
public class SearchTool implements ToolExecutor {
    private static final String TAG = "SearchTool";
    
    private final Context context;
    private final SearxngManager searxngManager;
    private final ToolDefinition toolDefinition;
    
    public SearchTool(Context context) {
        this.context = context;
        this.searxngManager = new SearxngManager(context);
        this.toolDefinition = createToolDefinition();
    }
    
    private ToolDefinition createToolDefinition() {
        Map<String, ToolDefinition.ParameterDefinition> parameters = new HashMap<>();
        
        parameters.put("query", new ToolDefinition.ParameterDefinition(
            "string", 
            "Search query for current information or news", 
            true
        ));
        
        return new ToolDefinition(
            "searxng_search",
            "Search the web for factual information, current events, news, research, or any topic that requires up-to-date or comprehensive information from the internet. Use when the user asks questions that would benefit from web search results.",
            parameters,
            "search",
            false
        );
    }
    
    @Override
    public void execute(JsonObject parameters, ToolExecutionCallback callback) {
        Log.d(TAG, "Executing search with parameters: " + parameters);
        
        String query = parameters.get("query").getAsString();
        
        searxngManager.search(parameters, new SearxngManager.SearxngCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Search completed successfully");
                callback.onSuccess(response);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Search failed: " + error);
                callback.onError("Search failed: " + error);
            }
        });
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
        
        if (!parameters.has("query")) {
            return ValidationResult.invalid("Missing required parameter: query");
        }
        
        String query = parameters.get("query").getAsString();
        if (query == null || query.trim().isEmpty()) {
            return ValidationResult.invalid("Query cannot be empty");
        }
        
        return ValidationResult.valid();
    }
}

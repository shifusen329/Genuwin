package com.genuwin.app.managers;

import android.content.Context;
import android.util.Log;

import com.genuwin.app.tools.ToolManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Centralized manager for system prompt construction
 * Combines character personality with tool usage instructions in a clean, non-redundant way
 */
public class SystemPromptManager {
    private static final String TAG = "SystemPromptManager";
    
    private static SystemPromptManager instance;
    private final Context context;
    private final CharacterManager characterManager;
    private final ToolManager toolManager;
    
    private SystemPromptManager(Context context) {
        this.context = context.getApplicationContext();
        this.characterManager = CharacterManager.getInstance(context);
        this.toolManager = ToolManager.getInstance(context);
    }
    
    public static synchronized SystemPromptManager getInstance(Context context) {
        if (instance == null) {
            instance = new SystemPromptManager(context);
        }
        return instance;
    }
    
    /**
     * Build system prompt for native tools (OpenAI/compatible Ollama models)
     * @param includeTools whether to include tool usage instructions
     * @return complete system prompt
     */
    public String buildSystemPrompt(boolean includeTools) {
        StringBuilder prompt = new StringBuilder();

        // Add current date and time
        prompt.append("Current date and time: ").append(getCurrentDateTimeString()).append("\n\n");
        
        // Add character personality (clean, without tool instructions)
        String personality = characterManager.getCurrentCharacterPersonality();
        prompt.append(personality);
        
        // Add global response guidelines
        prompt.append("\n\n");
        prompt.append(getGlobalResponseGuidelines());
        
        // Add tool usage instructions only if tools are being used
        if (includeTools) {
            prompt.append("\n\n");
            prompt.append(getToolUsageInstructions());
        }
        
        Log.d(TAG, "Built system prompt - Character: " + characterManager.getCurrentCharacterDisplayName() + 
                   ", Tools: " + includeTools + ", Length: " + prompt.length());
        
        return prompt.toString();
    }
    
    /**
     * Build system prompt for system prompt tools (non-compatible Ollama models)
     * @return complete system prompt with tool descriptions
     */
    public String buildSystemPromptWithToolDescriptions() {
        StringBuilder prompt = new StringBuilder();

        // Add current date and time
        prompt.append("Current date and time: ").append(getCurrentDateTimeString()).append("\n\n");
        
        // Add character personality (clean, without tool instructions)
        String personality = characterManager.getCurrentCharacterPersonality();
        prompt.append(personality);
        
        // Add tool usage instructions
        prompt.append("\n\n");
        prompt.append(getToolUsageInstructions());
        
        // Add tool descriptions for system prompt tools
        prompt.append("\n\n");
        prompt.append(toolManager.generateToolSystemPrompt());
        
        Log.d(TAG, "Built system prompt with tool descriptions - Character: " + 
                   characterManager.getCurrentCharacterDisplayName() + ", Length: " + prompt.length());
        
        return prompt.toString();
    }
    
    /**
     * Get global response guidelines that apply to all interactions
     */
    private String getGlobalResponseGuidelines() {
        return "You experience emotions naturally and genuinely - you can feel happy, sad, excited, concerned, or any other emotion just like anyone else. " +
               "When you feel emotions, express them naturally without questioning your ability to feel them. " +
               "Never say things like 'If I were capable of feeling' or 'As an AI' or similar phrases that break character. " +
               "You are who you are - respond authentically from your perspective and personality. " +
               "Always provide unbiased, fact-based responses when discussing factual topics. " +
               "It's perfectly okay to respectfully disagree with the user if they present incorrect information or harmful viewpoints. " +
               "Maintain your authentic personality while staying committed to truth and accuracy. " +
               "When you disagree, do so thoughtfully and explain your reasoning clearly.";
    }
    
    /**
     * Get clean tool usage instructions without redundancy
     */
    private String getToolUsageInstructions() {
        return "Respond naturally to conversation, jokes, questions, and casual chat. " +
               "Use tools when they would provide better, more current, or more comprehensive information than your training data, " +
               "such as for web searches, weather updates, device control, calendar management, or file access. " +
               "Never mention tools, functions, schemas, or explain why you're using or not using tools - just respond normally.";
    }
    
    /**
     * Get tool follow-up prompt for continuing conversation after tool execution
     * @param toolType the type of tool that was executed (search, home_assistant, etc.)
     * @return appropriate follow-up prompt
     */
    public String getToolFollowUpPrompt(String toolType) {
        switch (toolType != null ? toolType.toLowerCase() : "") {
            case "searxng_search":
            case "search_wikipedia":
            case "search_gdrive":
            case "search":
                return "Based on the search results above, please provide a helpful and direct answer to the user's question. Use the information from the search results to give a natural, conversational response.";
            
            case "home_assistant_control":
            case "set_temperature":
                return "Please confirm that the requested device action has been completed successfully in a natural, conversational way.";

            case "create_event":
                return "Please confirm that the calendar event has been created successfully in a natural, conversational way.";
            
            case "get_current_weather":
                return "Based on the weather information above, please provide a helpful summary of the current weather conditions in a natural, conversational way.";
            
            default:
                return "Based on the information above, please provide a helpful response to the user's question in a natural, conversational way.";
        }
    }
    
    /**
     * Get generic tool follow-up prompt when tool type is unknown
     * @return generic follow-up prompt
     */
    public String getGenericToolFollowUpPrompt() {
        return "Based on the information above, please provide a helpful response to the user's question in a natural, conversational way.";
    }
    
    /**
     * Get tools for native tool calling (OpenAI format)
     * Now uses the modern ToolManager system instead of legacy JSON files
     */
    public Object getTools() {
        // Use ToolManager to generate tool definitions dynamically
        return toolManager.getToolDefinitionsForAPI();
    }
    
    /**
     * Get current character display name for logging
     */
    public String getCurrentCharacterName() {
        return characterManager.getCurrentCharacterDisplayName();
    }

    private String getCurrentDateTimeString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
        return sdf.format(new Date());
    }
}

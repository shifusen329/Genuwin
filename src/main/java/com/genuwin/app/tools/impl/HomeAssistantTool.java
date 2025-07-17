package com.genuwin.app.tools.impl;

import android.content.Context;
import android.util.Log;

import com.genuwin.app.homeassistant.EntityManager;
import com.genuwin.app.homeassistant.HomeAssistantManager;
import com.genuwin.app.tools.ToolDefinition;
import com.genuwin.app.tools.ToolExecutor;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Home Assistant tool implementation for device control
 */
public class HomeAssistantTool implements ToolExecutor {
    private static final String TAG = "HomeAssistantTool";
    
    private final Context context;
    private final EntityManager entityManager;
    private final HomeAssistantManager homeAssistantManager;
    private final ToolDefinition toolDefinition;
    
    public HomeAssistantTool(Context context) {
        this.context = context;
        this.entityManager = new EntityManager(context);
        this.homeAssistantManager = new HomeAssistantManager(context);
        this.toolDefinition = createToolDefinition();
    }
    
    private ToolDefinition createToolDefinition() {
        Map<String, ToolDefinition.ParameterDefinition> parameters = new HashMap<>();
        
        parameters.put("action", new ToolDefinition.ParameterDefinition(
            "string", 
            "Action to perform: turn_on, turn_off", 
            true
        ));
        
        parameters.put("entity_id", new ToolDefinition.ParameterDefinition(
            "string", 
            "Device to control (e.g., living_room_lights, bedroom_fan)", 
            true
        ));
        
        return new ToolDefinition(
            "home_assistant_control",
            "Control home devices like lights, fans, switches. Use turn_on or turn_off actions with the device name.",
            parameters,
            "home_automation",
            false
        );
    }
    
    @Override
    public void execute(JsonObject parameters, ToolExecutionCallback callback) {
        Log.d(TAG, "Executing home assistant control with parameters: " + parameters);
        
        String action = parameters.get("action").getAsString();
        String entityId = parameters.get("entity_id").getAsString();
        
        // Get the actual Home Assistant entity IDs
        List<String> haEntityIds = entityManager.getEntityIds(entityId);
        if (haEntityIds == null || haEntityIds.isEmpty()) {
            callback.onError("Unknown device: " + entityId + ". Available devices: " + 
                String.join(", ", getAvailableDevices()));
            return;
        }
        
        try {
            // Execute action on all matching entities
            for (String haEntityId : haEntityIds) {
                String[] parts = haEntityId.split("\\.");
                if (parts.length == 2) {
                    String domain = parts[0];
                    homeAssistantManager.callService(domain, action, haEntityId);
                } else {
                    Log.w(TAG, "Invalid entity ID format: " + haEntityId);
                }
            }
            
            String friendlyAction = action.replace("_", " ");
            String friendlyEntity = entityId.replace("_", " ");
            String result = "Successfully " + friendlyAction + " the " + friendlyEntity;
            
            Log.d(TAG, "Home assistant action completed: " + result);
            callback.onSuccess(result);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute home assistant action", e);
            callback.onError("Failed to " + action + " " + entityId + ": " + e.getMessage());
        }
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
        
        if (!parameters.has("action")) {
            return ValidationResult.invalid("Missing required parameter: action");
        }
        
        if (!parameters.has("entity_id")) {
            return ValidationResult.invalid("Missing required parameter: entity_id");
        }
        
        String action = parameters.get("action").getAsString();
        if (action == null || action.trim().isEmpty()) {
            return ValidationResult.invalid("Action cannot be empty");
        }
        
        // Validate action
        if (!isValidAction(action)) {
            return ValidationResult.invalid("Invalid action: " + action + ". Valid actions: turn_on, turn_off");
        }
        
        String entityId = parameters.get("entity_id").getAsString();
        if (entityId == null || entityId.trim().isEmpty()) {
            return ValidationResult.invalid("Entity ID cannot be empty");
        }
        
        // Check if entity exists
        List<String> haEntityIds = entityManager.getEntityIds(entityId);
        if (haEntityIds == null || haEntityIds.isEmpty()) {
            return ValidationResult.invalid("Unknown device: " + entityId + ". Available devices: " + 
                String.join(", ", getAvailableDevices()));
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Check if action is valid
     */
    private boolean isValidAction(String action) {
        return "turn_on".equals(action) || "turn_off".equals(action);
    }
    
    /**
     * Get list of available devices for error messages
     */
    private List<String> getAvailableDevices() {
        return entityManager.getAllEntityNames();
    }
}

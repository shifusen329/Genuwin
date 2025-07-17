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
 * Temperature control tool implementation for thermostats and climate devices
 */
public class TemperatureTool implements ToolExecutor {
    private static final String TAG = "TemperatureTool";
    
    private final Context context;
    private final EntityManager entityManager;
    private final HomeAssistantManager homeAssistantManager;
    private final ToolDefinition toolDefinition;
    
    public TemperatureTool(Context context) {
        this.context = context;
        this.entityManager = new EntityManager(context);
        this.homeAssistantManager = new HomeAssistantManager(context);
        this.toolDefinition = createToolDefinition();
    }
    
    private ToolDefinition createToolDefinition() {
        Map<String, ToolDefinition.ParameterDefinition> parameters = new HashMap<>();
        
        parameters.put("entity_id", new ToolDefinition.ParameterDefinition(
            "string", 
            "Thermostat or climate device to control (e.g., thermostat, living_room_thermostat)", 
            true
        ));
        
        parameters.put("temperature", new ToolDefinition.ParameterDefinition(
            "number", 
            "Target temperature to set (in degrees)", 
            true
        ));
        
        return new ToolDefinition(
            "set_temperature",
            "Set the temperature for a thermostat or climate device. Specify the device and target temperature.",
            parameters,
            "home_automation",
            false
        );
    }
    
    @Override
    public void execute(JsonObject parameters, ToolExecutionCallback callback) {
        Log.d(TAG, "Executing temperature control with parameters: " + parameters);
        
        String entityId = parameters.get("entity_id").getAsString();
        double temperature = parameters.get("temperature").getAsDouble();
        
        // Get the actual Home Assistant entity IDs
        List<String> haEntityIds = entityManager.getEntityIds(entityId);
        if (haEntityIds == null || haEntityIds.isEmpty()) {
            callback.onError("Unknown thermostat device: " + entityId + ". Available devices: " + 
                String.join(", ", getAvailableThermostats()));
            return;
        }
        
        try {
            // Set temperature on all matching entities
            for (String haEntityId : haEntityIds) {
                homeAssistantManager.setTemperature(haEntityId, temperature);
            }
            
            String friendlyEntity = entityId.replace("_", " ");
            String result = "Successfully set the " + friendlyEntity + " to " + temperature + " degrees";
            
            Log.d(TAG, "Temperature control completed: " + result);
            callback.onSuccess(result);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to set temperature", e);
            callback.onError("Failed to set temperature for " + entityId + ": " + e.getMessage());
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
        
        if (!parameters.has("entity_id")) {
            return ValidationResult.invalid("Missing required parameter: entity_id");
        }
        
        if (!parameters.has("temperature")) {
            return ValidationResult.invalid("Missing required parameter: temperature");
        }
        
        String entityId = parameters.get("entity_id").getAsString();
        if (entityId == null || entityId.trim().isEmpty()) {
            return ValidationResult.invalid("Entity ID cannot be empty");
        }
        
        // Validate temperature
        double temperature;
        try {
            temperature = parameters.get("temperature").getAsDouble();
        } catch (Exception e) {
            return ValidationResult.invalid("Temperature must be a valid number");
        }
        
        // Check temperature range (reasonable limits)
        if (temperature < 40 || temperature > 90) {
            return ValidationResult.invalid("Temperature must be between 40 and 90 degrees (got " + temperature + ")");
        }
        
        // Check if entity exists
        List<String> haEntityIds = entityManager.getEntityIds(entityId);
        if (haEntityIds == null || haEntityIds.isEmpty()) {
            return ValidationResult.invalid("Unknown thermostat device: " + entityId + ". Available devices: " + 
                String.join(", ", getAvailableThermostats()));
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Get list of available thermostats for error messages
     */
    private List<String> getAvailableThermostats() {
        // This would ideally come from EntityManager, but for now return common names
        // In a real implementation, EntityManager should provide this method
        return List.of("thermostat", "living_room_thermostat", "bedroom_thermostat", "main_thermostat");
    }
}

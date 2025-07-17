package com.genuwin.app.managers;

import com.genuwin.app.live2d.WaifuDefine;
import com.genuwin.app.live2d.WaifuLive2DManager;
import com.genuwin.app.live2d.WaifuModel;
import com.genuwin.app.live2d.WaifuPal;
import com.live2d.sdk.cubism.framework.id.CubismId;
import com.live2d.sdk.cubism.framework.id.CubismIdManager;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Manages voice-activated parameter control for Live2D models
 * Handles voice commands that toggle or modify character parameters
 */
public class VoiceParameterManager {
    private static VoiceParameterManager instance;
    
    // Parameter state storage
    private final Map<String, Float> parameterStates = new HashMap<>();
    private final Map<String, Float> defaultParameterValues = new HashMap<>();
    
    // Voice command patterns and their corresponding actions
    private final Map<Pattern, ParameterAction> voiceCommands = new HashMap<>();
    
    // Parameter action interface
    public interface ParameterAction {
        void execute(String matchedText);
    }
    
    // Parameter configuration for different characters
    private static class ParameterConfig {
        final String parameterId;
        final float defaultValue;
        final float toggleValue;
        
        ParameterConfig(String parameterId, float defaultValue, float toggleValue) {
            this.parameterId = parameterId;
            this.defaultValue = defaultValue;
            this.toggleValue = toggleValue;
        }
    }
    
    public static VoiceParameterManager getInstance() {
        if (instance == null) {
            instance = new VoiceParameterManager();
        }
        return instance;
    }
    
    public static void releaseInstance() {
        instance = null;
    }
    
    private VoiceParameterManager() {
        initializeVoiceCommands();
        initializeDefaultParameters();
        
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("VoiceParameterManager initialized");
        }
    }
    
    /**
     * Initialize voice command patterns and their actions
     */
    private void initializeVoiceCommands() {
        // Clothing control commands
        voiceCommands.put(
            Pattern.compile("(?i).*take\\s+off\\s+(your\\s+)?top.*"),
            matchedText -> {
                toggleParameter("Param4", 0.0f);  // Remove top
                toggleParameter("ParamCheek", 1.0f);  // Set cheek parameter
            }
        );
        
        voiceCommands.put(
            Pattern.compile("(?i).*put\\s+(your\\s+)?top\\s+back\\s+on.*"),
            matchedText -> toggleParameter("Param4", 10.0f)
        );
        
        voiceCommands.put(
            Pattern.compile("(?i).*put\\s+on\\s+(your\\s+)?top.*"),
            matchedText -> toggleParameter("Param4", 10.0f)
        );
        
        // Bottom clothing control
        voiceCommands.put(
            Pattern.compile("(?i).*take\\s+off\\s+(your\\s+)?bottom.*"),
            matchedText -> toggleParameter("Param5", 0.0f)
        );
        
        voiceCommands.put(
            Pattern.compile("(?i).*put\\s+(your\\s+)?bottom\\s+back\\s+on.*"),
            matchedText -> toggleParameter("Param5", 10.0f)
        );
        
        // Reset command
        voiceCommands.put(
            Pattern.compile("(?i).*reset\\s+(clothing|outfit|parameters).*"),
            matchedText -> resetAllParameters()
        );
        
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("VoiceParameterManager: Initialized " + voiceCommands.size() + " voice commands");
        }
    }
    
    /**
     * Initialize default parameter values for different characters
     */
    private void initializeDefaultParameters() {
        // Ivy character defaults
        defaultParameterValues.put("Param4", 10.0f); // Top clothing - visible by default
        defaultParameterValues.put("Param5", 10.0f); // Bottom clothing - visible by default
        defaultParameterValues.put("ParamCheek", 0.0f); // Cheek parameter - default state
        
        // Initialize current states to defaults
        parameterStates.putAll(defaultParameterValues);
        
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("VoiceParameterManager: Initialized default parameters: " + defaultParameterValues.toString());
        }
    }
    
    /**
     * Process voice input text for parameter commands
     * @param voiceText The transcribed voice input
     * @return true if a command was processed, false otherwise
     */
    public boolean processVoiceCommand(String voiceText) {
        if (voiceText == null || voiceText.trim().isEmpty()) {
            return false;
        }
        
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("VoiceParameterManager: Processing voice text: \"" + voiceText + "\"");
        }
        
        // Check each voice command pattern
        for (Map.Entry<Pattern, ParameterAction> entry : voiceCommands.entrySet()) {
            Pattern pattern = entry.getKey();
            ParameterAction action = entry.getValue();
            
            if (pattern.matcher(voiceText).matches()) {
                if (WaifuDefine.DEBUG_LOG_ENABLE) {
                    WaifuPal.printLog("VoiceParameterManager: Matched command pattern, executing action");
                }
                
                try {
                    action.execute(voiceText);
                    return true;
                } catch (Exception e) {
                    if (WaifuDefine.DEBUG_LOG_ENABLE) {
                        WaifuPal.printLog("VoiceParameterManager: Error executing voice command: " + e.getMessage());
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Toggle a parameter to a specific value
     */
    private void toggleParameter(String parameterId, float targetValue) {
        try {
            WaifuLive2DManager live2DManager = WaifuLive2DManager.getInstance();
            WaifuModel model = live2DManager.getWaifuModel();
            
            if (model == null || model.getModel() == null) {
                if (WaifuDefine.DEBUG_LOG_ENABLE) {
                    WaifuPal.printLog("VoiceParameterManager: No active model available for parameter control");
                }
                return;
            }
            
            // Get the parameter ID
            CubismIdManager idManager = com.live2d.sdk.cubism.framework.CubismFramework.getIdManager();
            CubismId paramId = idManager.getId(parameterId);
            
            // Set the parameter value
            model.getModel().setParameterValue(paramId, targetValue);
            
            // Update our state tracking
            parameterStates.put(parameterId, targetValue);
            
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("VoiceParameterManager: Set parameter " + parameterId + " to " + targetValue);
            }
            
        } catch (Exception e) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("VoiceParameterManager: Error setting parameter " + parameterId + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Reset all parameters to their default values
     */
    private void resetAllParameters() {
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("VoiceParameterManager: Resetting all parameters to defaults");
        }
        
        for (Map.Entry<String, Float> entry : defaultParameterValues.entrySet()) {
            toggleParameter(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Apply default parameter values to the current model
     * Called when a model is loaded or switched
     */
    public void applyDefaultParameters() {
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("VoiceParameterManager: Applying default parameters to model");
        }
        
        // Apply defaults with a small delay to ensure model is ready
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.postDelayed(() -> {
            for (Map.Entry<String, Float> entry : defaultParameterValues.entrySet()) {
                toggleParameter(entry.getKey(), entry.getValue());
            }
        }, 100);
    }
    
    /**
     * Get current parameter state
     */
    public float getParameterState(String parameterId) {
        return parameterStates.getOrDefault(parameterId, 0.0f);
    }
    
    /**
     * Check if voice parameter control is enabled
     */
    public boolean isVoiceParameterControlEnabled() {
        // For now, always enabled. Could be made configurable via settings
        return true;
    }
    
    /**
     * Get a list of available voice commands for help/documentation
     */
    public String[] getAvailableCommands() {
        return new String[] {
            "Take off your top",
            "Put your top back on",
            "Take off your bottom", 
            "Put your bottom back on",
            "Reset clothing"
        };
    }
}

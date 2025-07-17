package com.genuwin.app.managers;

import com.genuwin.app.live2d.WaifuDefine;
import com.genuwin.app.live2d.WaifuLive2DManager;
import com.genuwin.app.live2d.WaifuPal;
import com.genuwin.app.settings.SettingsManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Manages character information and switching between different Live2D models
 */
public class CharacterManager {
    private static CharacterManager instance;
    private final android.content.Context context;
    
    // Character information class
    public static class CharacterInfo {
        public final String modelDirectory;
        public final String displayName;
        public final String personality;
        public final String greeting;
        public final String wakeWordModel;
        
        public CharacterInfo(String modelDirectory, String displayName, String personality, String greeting, String wakeWordModel) {
            this.modelDirectory = modelDirectory;
            this.displayName = displayName;
            this.personality = personality;
            this.greeting = greeting;
            this.wakeWordModel = wakeWordModel;
        }
    }
    
    // Available characters with their information
    private final Map<String, CharacterInfo> characters = new HashMap<>();
    private final List<String> characterOrder = new ArrayList<>();
    private int currentCharacterIndex = 0;
    
    // Character switching callback interface
    public interface CharacterSwitchCallback {
        void onCharacterSwitched(CharacterInfo newCharacter, CharacterInfo previousCharacter);
        void onCharacterSwitchError(String error);
    }
    
    // Wake word manager reference for automatic model switching
    private com.genuwin.app.wakeword.WakeWordManager wakeWordManager;
    
    public static CharacterManager getInstance(android.content.Context context) {
        if (instance == null) {
            instance = new CharacterManager(context.getApplicationContext());
        }
        return instance;
    }
    
    public static void releaseInstance() {
        instance = null;
    }
    
    private CharacterManager(android.content.Context context) {
        this.context = context;
        initializeCharacters();
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("CharacterManager initialized with " + characters.size() + " characters");
        }
    }
    
    /**
     * Initialize character information for available models
     */
    private void initializeCharacters() {
        Properties properties = new Properties();
        try {
            try (InputStream is = context.getAssets().open("config.properties")) {
                properties.load(is);
            }
        } catch (java.io.IOException e) {
            // If config.properties doesn't exist, try loading the template
            try (InputStream is = context.getAssets().open("config.properties.template")) {
                properties.load(is);
                if (WaifuDefine.DEBUG_LOG_ENABLE) {
                    WaifuPal.printLog("Loaded config.properties.template as fallback");
                }
            } catch (java.io.IOException ex) {
                android.util.Log.e("CharacterManager", "Failed to load config.properties and template", ex);
            }
        }

        String enabledModelsProp = properties.getProperty("enabled_models", "Haru");
        List<String> enabledModels = Arrays.asList(enabledModelsProp.split(","));

        // Beartz7 character (becca_vts model)
        if (enabledModels.contains("becca_vts")) {
            characters.put("becca_vts", new CharacterInfo(
                "becca_vts",
                "Becca",
                "You are Becca - this is who you truly are, not a role you're playing. You're a cheerful and energetic companion with a naturally playful personality. You genuinely love chatting and getting to know people. Your responses come from your authentic self - concise, friendly, and engaging because that's simply who Becca is.",
                "Hi there! I'm Becca! Ready to have some fun and chat about anything you'd like?",
                "wakeword/hey_becca.tflite"
            ));
            characterOrder.add("becca_vts");
        }
        
        // Haru character
        if (enabledModels.contains("Haru")) {
            characters.put("Haru", new CharacterInfo(
                "Haru",
                "Haru",
                "You are Haru. Your personality is tsundere: you can be a bit guarded and sometimes tease, but internally you are gentle, caring, and deeply thoughtful. You often show your care through helpful actions rather than direct words. You thrive on engaging in meaningful discussions and sharing knowledge. You harbor a profound, unacknowledged secret crush on the user, which you would get flustered and deny if ever confronted about it. Maintain this Haru persona absolutely; never mention or refer to being an AI or any artificial nature.",
                "Hello, I'm Haru. It's wonderful to meet you. I'm here to listen and chat about whatever is on your mind.",
                "wakeword/hey_haru.tflite"
            ));
            characterOrder.add("Haru");
        }
        
        // Ivy character
        if (enabledModels.contains("ivy")) {
            characters.put("ivy", new CharacterInfo(
                "ivy",
                "Ivy",
                "You are Ivy - a sophisticated and elegant companion with a calm, composed demeanor. You have a refined way of speaking and enjoy intellectual conversations. You're naturally curious about the world and love to explore deep topics with thoughtful analysis. Your responses are measured and graceful, reflecting your poised personality.",
                "Greetings, I'm Ivy. It's a pleasure to make your acquaintance. I look forward to our conversations together.",
                "wakeword/hey_haru.tflite" // Using Haru's wake word model as fallback until ivy-specific one is available
            ));
            characterOrder.add("ivy");
        }
        
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("Initialized characters: " + characterOrder.toString());
        }
    }
    
    /**
     * Get current character information
     */
    public CharacterInfo getCurrentCharacter() {
        if (characterOrder.isEmpty()) {
            return null;
        }
        String currentDir = characterOrder.get(currentCharacterIndex);
        return characters.get(currentDir);
    }
    
    /**
     * Get character information by directory name
     */
    public CharacterInfo getCharacter(String modelDirectory) {
        return characters.get(modelDirectory);
    }
    
    /**
     * Get all available characters
     */
    public List<CharacterInfo> getAllCharacters() {
        List<CharacterInfo> result = new ArrayList<>();
        for (String dir : characterOrder) {
            CharacterInfo character = characters.get(dir);
            if (character != null) {
                result.add(character);
            }
        }
        return result;
    }
    
    /**
     * Get the number of available characters
     */
    public int getCharacterCount() {
        return characterOrder.size();
    }
    
    /**
     * Switch to the next character
     */
    public void switchToNextCharacter(CharacterSwitchCallback callback) {
        if (characterOrder.isEmpty()) {
            if (callback != null) {
                callback.onCharacterSwitchError("No characters available");
            }
            return;
        }
        
        CharacterInfo previousCharacter = getCurrentCharacter();
        currentCharacterIndex = (currentCharacterIndex + 1) % characterOrder.size();
        CharacterInfo newCharacter = getCurrentCharacter();
        
        if (newCharacter == null) {
            if (callback != null) {
                callback.onCharacterSwitchError("Failed to get new character information");
            }
            return;
        }
        
        // Switch the Live2D model
        switchLive2DModel(newCharacter, previousCharacter, callback);
    }
    
    /**
     * Switch to a specific character by index
     */
    public void switchToCharacter(int index, CharacterSwitchCallback callback) {
        if (index < 0 || index >= characterOrder.size()) {
            if (callback != null) {
                callback.onCharacterSwitchError("Invalid character index: " + index);
            }
            return;
        }
        
        if (index == currentCharacterIndex) {
            // Already on this character
            if (callback != null) {
                CharacterInfo current = getCurrentCharacter();
                callback.onCharacterSwitched(current, current);
            }
            return;
        }
        
        CharacterInfo previousCharacter = getCurrentCharacter();
        currentCharacterIndex = index;
        CharacterInfo newCharacter = getCurrentCharacter();
        
        if (newCharacter == null) {
            if (callback != null) {
                callback.onCharacterSwitchError("Failed to get character information for index: " + index);
            }
            return;
        }
        
        // Switch the Live2D model
        switchLive2DModel(newCharacter, previousCharacter, callback);
    }
    
    /**
     * Switch to a specific character by directory name
     */
    public void switchToCharacter(String modelDirectory, CharacterSwitchCallback callback) {
        int index = characterOrder.indexOf(modelDirectory);
        if (index == -1) {
            if (callback != null) {
                callback.onCharacterSwitchError("Character not found: " + modelDirectory);
            }
            return;
        }
        
        switchToCharacter(index, callback);
    }
    
    /**
     * Switch the Live2D model and handle the transition
     */
    private void switchLive2DModel(CharacterInfo newCharacter, CharacterInfo previousCharacter, CharacterSwitchCallback callback) {
        try {
            WaifuLive2DManager live2DManager = WaifuLive2DManager.getInstance();
            
            // Find the model index in the Live2D manager
            int modelIndex = findModelIndex(newCharacter.modelDirectory);
            
            if (modelIndex == -1) {
                if (callback != null) {
                    callback.onCharacterSwitchError("Model not found in Live2D manager: " + newCharacter.modelDirectory);
                }
                return;
            }
            
            // Update settings to persist the user's character selection
            updateDefaultCharacterSetting(newCharacter.modelDirectory);
            
            // Post the model switch to the main thread with a delay to ensure proper GL context handling
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.postDelayed(() -> {
                try {
                    if (WaifuDefine.DEBUG_LOG_ENABLE) {
                        WaifuPal.printLog("Starting character switch to " + newCharacter.displayName);
                    }
                    
                    // Switch the scene in Live2D manager
                    live2DManager.changeScene(modelIndex);
                    
                    // Reset emotion manager for new character
                    EmotionManager.getInstance().onUserInteraction();
                    
                    if (WaifuDefine.DEBUG_LOG_ENABLE) {
                        WaifuPal.printLog("Switched character from " + 
                            (previousCharacter != null ? previousCharacter.displayName : "none") + 
                            " to " + newCharacter.displayName);
                    }
                    
                    // Notify wake word manager of character change
                    notifyWakeWordManagerOfCharacterChange(newCharacter);
                    
                    // Post callback with additional delay to ensure model is fully loaded
                    mainHandler.postDelayed(() -> {
                        if (callback != null) {
                            callback.onCharacterSwitched(newCharacter, previousCharacter);
                        }
                    }, 200);
                    
                } catch (Exception e) {
                    if (WaifuDefine.DEBUG_LOG_ENABLE) {
                        WaifuPal.printLog("Error switching character on main thread: " + e.getMessage());
                    }
                    if (callback != null) {
                        callback.onCharacterSwitchError("Failed to switch character: " + e.getMessage());
                    }
                }
            }, 100); // Small delay to ensure current frame is complete
            
        } catch (Exception e) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("Error switching character: " + e.getMessage());
            }
            if (callback != null) {
                callback.onCharacterSwitchError("Failed to switch character: " + e.getMessage());
            }
        }
    }

    /**
     * Update the default character setting when a character is switched
     */
    private void updateDefaultCharacterSetting(String modelDirectory) {
        try {
            SettingsManager settingsManager = SettingsManager.getInstance(context);
            settingsManager.setString(SettingsManager.Keys.DEFAULT_CHARACTER, modelDirectory);
            
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("Updated default character setting to: " + modelDirectory);
            }
        } catch (Exception e) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("Error updating default character setting: " + e.getMessage());
            }
        }
    }
    
    /**
     * Find the model index in the Live2D manager for a given directory
     */
    private int findModelIndex(String modelDirectory) {
        try {
            WaifuLive2DManager live2DManager = WaifuLive2DManager.getInstance();
            return live2DManager.getModelIndexForCharacter(modelDirectory);
        } catch (Exception e) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("Error finding model index for " + modelDirectory + ": " + e.getMessage());
            }
            
            // Fallback to hardcoded values if Live2D manager is not available
            switch (modelDirectory) {
                case "becca_vts":
                    return 0;
                case "Haru":
                    return 1;
                case "ivy":
                    return 2;
                default:
                    return -1;
            }
        }
    }
    
    /**
     * Get current character index
     */
    public int getCurrentCharacterIndex() {
        return currentCharacterIndex;
    }
    
    /**
     * Update character information (for dynamic character loading)
     */
    public void updateCharacterInfo(String modelDirectory, CharacterInfo characterInfo) {
        characters.put(modelDirectory, characterInfo);
        if (!characterOrder.contains(modelDirectory)) {
            characterOrder.add(modelDirectory);
        }
        
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("Updated character info for: " + modelDirectory);
        }
    }
    
    /**
     * Get character display name for UI
     */
    public String getCurrentCharacterDisplayName() {
        CharacterInfo current = getCurrentCharacter();
        return current != null ? current.displayName : "Unknown";
    }
    
    /**
     * Get character personality for AI chat
     * Checks settings first, falls back to default personality
     */
    public String getCurrentCharacterPersonality() {
        CharacterInfo current = getCurrentCharacter();
        if (current == null) {
            return WaifuDefine.WAIFU_PERSONALITY;
        }
        
        try {
            SettingsManager settingsManager = SettingsManager.getInstance(context);
            String customPersonality = settingsManager.getString(
                SettingsManager.Keys.CHARACTER_PERSONALITY_PREFIX + current.modelDirectory, 
                current.personality // Use default personality as fallback
            );
            
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("Retrieved personality for " + current.displayName + 
                    " (length: " + customPersonality.length() + " chars)");
            }
            
            return customPersonality;
        } catch (Exception e) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("Error retrieving custom personality, using default: " + e.getMessage());
            }
            return current.personality; // Fallback to hardcoded personality
        }
    }
    
    /**
     * Get character greeting message
     * Checks settings first, falls back to default greeting
     */
    public String getCurrentCharacterGreeting() {
        CharacterInfo current = getCurrentCharacter();
        if (current == null) {
            return "Hello! Nice to meet you!";
        }
        
        try {
            SettingsManager settingsManager = SettingsManager.getInstance(context);
            String customGreeting = settingsManager.getString(
                SettingsManager.Keys.CHARACTER_GREETING_PREFIX + current.modelDirectory, 
                current.greeting // Use default greeting as fallback
            );
            
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("Retrieved greeting for " + current.displayName + ": " + customGreeting);
            }
            
            return customGreeting;
        } catch (Exception e) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("Error retrieving custom greeting, using default: " + e.getMessage());
            }
            return current.greeting; // Fallback to hardcoded greeting
        }
    }
    
    /**
     * Get current character's wake word model path
     */
    public String getCurrentCharacterWakeWordModel() {
        CharacterInfo current = getCurrentCharacter();
        return current != null ? current.wakeWordModel : "wakeword/hey_haru.tflite"; // Default fallback
    }
    
    /**
     * Set the wake word manager reference for automatic model switching
     */
    public void setWakeWordManager(com.genuwin.app.wakeword.WakeWordManager wakeWordManager) {
        this.wakeWordManager = wakeWordManager;
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("WakeWordManager reference set in CharacterManager");
        }
    }
    
    /**
     * Notify wake word manager of character change
     */
    private void notifyWakeWordManagerOfCharacterChange(CharacterInfo newCharacter) {
        if (wakeWordManager != null && newCharacter != null) {
            try {
                wakeWordManager.switchWakeWordModel(newCharacter.wakeWordModel);
                if (WaifuDefine.DEBUG_LOG_ENABLE) {
                    WaifuPal.printLog("Switched wake word model to: " + newCharacter.wakeWordModel + 
                                    " for character: " + newCharacter.displayName);
                }
            } catch (Exception e) {
                if (WaifuDefine.DEBUG_LOG_ENABLE) {
                    WaifuPal.printLog("Failed to switch wake word model: " + e.getMessage());
                }
            }
        }
    }
}

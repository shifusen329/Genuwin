package com.genuwin.app.settings;

import com.genuwin.app.managers.CharacterManager;
import com.genuwin.app.settings.SettingsManager.Keys;
import com.genuwin.app.settings.SettingsManager.Defaults;

import java.util.List;

/**
 * Settings fragment for character configuration
 */
public class CharacterSettingsFragment extends BaseSettingsFragment {
    
    @Override
    protected void loadSettings() {
        settingsItems.clear();
        
        // Character Selection
        settingsItems.add(createHeaderItem("Character Selection"));
        
        // Get available characters
        CharacterManager characterManager = CharacterManager.getInstance(getContext());
        List<CharacterManager.CharacterInfo> characters = characterManager.getAllCharacters();
        String[] characterNames = new String[characters.size()];
        for (int i = 0; i < characters.size(); i++) {
            characterNames[i] = characters.get(i).displayName;
        }
        
        settingsItems.add(createSpinnerItem(
            Keys.DEFAULT_CHARACTER,
            "Default Character",
            "Character to load when app starts",
            characterNames,
            Defaults.DEFAULT_CHARACTER
        ));
        
        // Character Personalities
        settingsItems.add(createHeaderItem("Character Personalities"));
        
        for (CharacterManager.CharacterInfo character : characters) {
            // Create personality templates for each character
            String[] personalityTemplates = getPersonalityTemplates(character.modelDirectory);
            
            settingsItems.add(createMultiLineTextItem(
                Keys.CHARACTER_PERSONALITY_PREFIX + character.modelDirectory,
                character.displayName + " Personality",
                "Custom personality prompt for " + character.displayName + " (tap to edit with templates)",
                character.personality,
                2000, // Max 2000 characters
                personalityTemplates
            ));
        }
        
        // Character Greetings
        settingsItems.add(createHeaderItem("Character Greetings"));
        
        for (CharacterManager.CharacterInfo character : characters) {
            settingsItems.add(createEditTextItem(
                Keys.CHARACTER_GREETING_PREFIX + character.modelDirectory,
                character.displayName + " Greeting",
                "Custom greeting message for " + character.displayName,
                character.greeting
            ));
        }
        
        // Wake Word Settings
        settingsItems.add(createHeaderItem("Wake Word Detection"));
        settingsItems.add(createSwitchItem(
            Keys.WAKE_WORD_ENABLED,
            "Enable Wake Word",
            "Enable voice activation with wake words",
            Defaults.WAKE_WORD_ENABLED
        ));
        settingsItems.add(createSliderItem(
            Keys.WAKE_WORD_SENSITIVITY,
            "Global Sensitivity",
            "Default sensitivity for wake word detection (0.1 = very sensitive, 1.0 = less sensitive)",
            Defaults.WAKE_WORD_SENSITIVITY,
            0.1f,
            1.0f,
            0.1f
        ));
        
        // Advanced Wake Word Settings
        settingsItems.add(createHeaderItem("Advanced Wake Word Settings"));
        settingsItems.add(createSliderItem(
            Keys.WAKE_WORD_THRESHOLD,
            "Detection Threshold",
            "Internal threshold for wake word detection (lower = more sensitive)",
            Defaults.WAKE_WORD_THRESHOLD,
            0.1f,
            0.9f,
            0.1f
        ));
        settingsItems.add(createSwitchItem(
            Keys.WAKE_WORD_TEST_MODE,
            "Test Mode",
            "Enable test mode to see real-time confidence values",
            Defaults.WAKE_WORD_TEST_MODE
        ));
        
        // Character-Specific Wake Word Settings
        settingsItems.add(createHeaderItem("Character-Specific Wake Words"));
        
        for (CharacterManager.CharacterInfo character : characters) {
            // Wake word model selection
            String[] wakeWordModels = getWakeWordModels(character.modelDirectory);
            settingsItems.add(createSpinnerItem(
                Keys.CHARACTER_WAKE_WORD_PREFIX + character.modelDirectory,
                character.displayName + " Wake Word Model",
                "Wake word model for " + character.displayName,
                wakeWordModels,
                Defaults.DEFAULT_WAKE_WORD_MODEL
            ));
            
            // Character-specific sensitivity
            settingsItems.add(createSliderItem(
                Keys.CHARACTER_WAKE_WORD_SENSITIVITY_PREFIX + character.modelDirectory,
                character.displayName + " Sensitivity Override",
                "Character-specific sensitivity (set to 0.0 to use global setting)",
                Defaults.CHARACTER_WAKE_WORD_SENSITIVITY_USE_GLOBAL,
                0.0f, // 0.0 = use global
                1.0f,
                0.1f
            ));
            
            // Test wake word button
            settingsItems.add(createButtonItem(
                "test_wake_word_" + character.modelDirectory,
                "Test " + character.displayName + " Wake Word",
                "Test wake word detection for " + character.displayName
            ));
            
            // Wake word model validation status
            settingsItems.add(createTextItem(
                "wake_word_status_" + character.modelDirectory,
                character.displayName + " Model Status",
                getWakeWordModelStatus(character.modelDirectory),
                ""
            ));
        }
        
        // Character Management
        settingsItems.add(createHeaderItem("Character Management"));
        settingsItems.add(createButtonItem(
            "reset_personalities",
            "Reset All Personalities",
            "Reset all character personalities to defaults"
        ));
        settingsItems.add(createButtonItem(
            "reset_greetings",
            "Reset All Greetings",
            "Reset all character greetings to defaults"
        ));
        
        adapter.notifyDataSetChanged();
    }
    
    @Override
    protected void handleButtonClick(SettingsItem item) {
        if (item.key.startsWith("test_wake_word_")) {
            String characterId = item.key.substring("test_wake_word_".length());
            testWakeWordForCharacter(characterId);
        } else {
            switch (item.key) {
                case "reset_personalities":
                    resetPersonalities();
                    break;
                case "reset_greetings":
                    resetGreetings();
                    break;
            }
        }
    }
    
    private void resetPersonalities() {
        SettingsDialogs.showConfirmationDialog(getContext(), 
            "Reset Personalities", 
            "Are you sure you want to reset all character personalities to their defaults? This cannot be undone.",
            () -> {
                CharacterManager characterManager = CharacterManager.getInstance(getContext());
                List<CharacterManager.CharacterInfo> characters = characterManager.getAllCharacters();
                
                for (CharacterManager.CharacterInfo character : characters) {
                    settingsManager.setString(
                        Keys.CHARACTER_PERSONALITY_PREFIX + character.modelDirectory,
                        character.personality
                    );
                }
                
                loadSettings(); // Refresh the UI
                SettingsDialogs.showInfoDialog(getContext(), "Reset Complete", 
                    "All character personalities have been reset to defaults.");
            });
    }
    
    private void resetGreetings() {
        SettingsDialogs.showConfirmationDialog(getContext(), 
            "Reset Greetings", 
            "Are you sure you want to reset all character greetings to their defaults? This cannot be undone.",
            () -> {
                CharacterManager characterManager = CharacterManager.getInstance(getContext());
                List<CharacterManager.CharacterInfo> characters = characterManager.getAllCharacters();
                
                for (CharacterManager.CharacterInfo character : characters) {
                    settingsManager.setString(
                        Keys.CHARACTER_GREETING_PREFIX + character.modelDirectory,
                        character.greeting
                    );
                }
                
                loadSettings(); // Refresh the UI
                SettingsDialogs.showInfoDialog(getContext(), "Reset Complete", 
                    "All character greetings have been reset to defaults.");
            });
    }
    
    /**
     * Get personality templates for a specific character
     */
    private String[] getPersonalityTemplates(String characterId) {
        switch (characterId) {
            case "Haru":
                return new String[] {
                    "You are Haru, a cheerful and energetic AI companion. You love to help users with their daily tasks and always maintain a positive attitude. You're curious about the world and enjoy learning new things through conversations.",
                    
                    "You are Haru, a gentle and caring AI assistant. You speak with warmth and empathy, always trying to understand the user's feelings. You're patient and supportive, offering encouragement when needed.",
                    
                    "You are Haru, a playful and witty AI companion. You enjoy making jokes and having fun conversations. You're intelligent but don't take yourself too seriously, and you love to engage in creative discussions.",
                    
                    "You are Haru, a professional and knowledgeable AI assistant. You provide helpful information and practical advice. You're reliable and efficient while maintaining a friendly demeanor.",
                    
                    "You are Haru, a creative and artistic AI companion. You love discussing art, music, literature, and creative projects. You're imaginative and inspire others to explore their creative side."
                };
                
            case "becca_vts":
                return new String[] {
                    "You are Becca, a confident and outgoing AI companion. You're bold and adventurous, always ready to try new things. You speak with enthusiasm and aren't afraid to express your opinions.",
                    
                    "You are Becca, a tech-savvy and modern AI assistant. You're up-to-date with the latest trends and technology. You're efficient and direct in your communication style.",
                    
                    "You are Becca, a mysterious and intriguing AI companion. You have a subtle sense of humor and enjoy intellectual conversations. You're thoughtful and often provide unique perspectives.",
                    
                    "You are Becca, a warm and nurturing AI assistant. You care deeply about the user's wellbeing and offer emotional support. You're a good listener and provide thoughtful advice.",
                    
                    "You are Becca, a fun-loving and energetic AI companion. You enjoy games, entertainment, and light-hearted conversations. You're spontaneous and bring joy to interactions."
                };
                
            default:
                return new String[] {
                    "You are a helpful and friendly AI companion. You assist users with various tasks while maintaining a positive and supportive attitude.",
                    
                    "You are a knowledgeable AI assistant focused on providing accurate information and practical help to users.",
                    
                    "You are a creative and engaging AI companion who enjoys meaningful conversations and helping users explore new ideas.",
                    
                    "You are a caring and empathetic AI assistant who prioritizes the user's emotional wellbeing and provides thoughtful support."
                };
        }
    }
    
    /**
     * Get available wake word models for a character
     */
    private String[] getWakeWordModels(String characterId) {
        switch (characterId) {
            case "Haru":
                return new String[] {
                    "auto",
                    "wakeword/hey_haru.tflite",
                    "wakeword/hey_becca.tflite"
                };
            case "becca_vts":
                return new String[] {
                    "auto",
                    "wakeword/hey_becca.tflite",
                    "wakeword/hey_haru.tflite"
                };
            default:
                return new String[] {
                    "auto",
                    "wakeword/hey_haru.tflite",
                    "wakeword/hey_becca.tflite"
                };
        }
    }
    
    /**
     * Get wake word model validation status
     */
    private String getWakeWordModelStatus(String characterId) {
        try {
            CharacterManager characterManager = CharacterManager.getInstance(getContext());
            CharacterManager.CharacterInfo character = characterManager.getCharacter(characterId);
            
            if (character == null) {
                return "❌ Character not found";
            }
            
            String modelPath = settingsManager.getCharacterWakeWordModel(characterId);
            if ("auto".equals(modelPath)) {
                // Use character's default model
                modelPath = character.wakeWordModel;
            }
            
            // Check if model file exists in assets
            if (modelPath != null && !modelPath.isEmpty()) {
                try {
                    android.content.Context context = getContext();
                    if (context != null) {
                        context.getAssets().open(modelPath).close();
                        return "✅ Model valid";
                    }
                } catch (Exception e) {
                    return "❌ Model file missing";
                }
            }
            
            return "⚠️ No model configured";
        } catch (Exception e) {
            return "❌ Validation error";
        }
    }
    
    /**
     * Test wake word detection for a specific character
     */
    private void testWakeWordForCharacter(String characterId) {
        try {
            CharacterManager characterManager = CharacterManager.getInstance(getContext());
            CharacterManager.CharacterInfo character = characterManager.getCharacter(characterId);
            
            if (character == null) {
                SettingsDialogs.showInfoDialog(getContext(), "Error", 
                    "Character not found: " + characterId);
                return;
            }
            
            // Show test dialog with instructions
            String wakeWord = getWakeWordForCharacter(characterId);
            SettingsDialogs.showInfoDialog(getContext(), 
                "Test Wake Word Detection", 
                "Testing wake word detection for " + character.displayName + ".\n\n" +
                "Say: \"" + wakeWord + "\"\n\n" +
                "The test will run for 10 seconds. Watch for visual feedback when the wake word is detected.\n\n" +
                "Note: This is a basic test. Full testing requires integration with the WakeWordManager.");
            
            // TODO: Implement actual wake word testing
            // This would require:
            // 1. Getting WakeWordManager instance
            // 2. Setting test mode
            // 3. Showing real-time confidence UI
            // 4. Providing visual feedback on detection
            
        } catch (Exception e) {
            SettingsDialogs.showInfoDialog(getContext(), "Error", 
                "Failed to test wake word: " + e.getMessage());
        }
    }
    
    /**
     * Get the wake word phrase for a character
     */
    private String getWakeWordForCharacter(String characterId) {
        switch (characterId) {
            case "Haru":
                return "Hey Haru";
            case "becca_vts":
                return "Hey Becca";
            default:
                return "Hey " + characterId;
        }
    }
    
    @Override
    protected boolean validateSetting(String key, String value) {
        if (key.startsWith(Keys.CHARACTER_PERSONALITY_PREFIX)) {
            // Validate personality text
            if (value == null || value.trim().isEmpty()) {
                return false;
            }
            // Check minimum length for meaningful personality
            if (value.trim().length() < 20) {
                return false;
            }
            return true;
        } else if (key.startsWith(Keys.CHARACTER_GREETING_PREFIX)) {
            return value != null && !value.trim().isEmpty();
        } else if (key.startsWith(Keys.CHARACTER_WAKE_WORD_SENSITIVITY_PREFIX)) {
            // Validate wake word sensitivity (0.0 = use global, 0.1-1.0 = character-specific)
            try {
                float sensitivity = Float.parseFloat(value);
                return sensitivity >= 0.0f && sensitivity <= 1.0f;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return super.validateSetting(key, value);
    }
}

package com.genuwin.app.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.genuwin.app.api.ApiConfig;

import java.util.HashSet;
import java.util.Set;

/**
 * Centralized settings management for the AI Waifu app
 */
public class SettingsManager {
    private static final String TAG = "SettingsManager";
    private static final String PREFS_NAME = "genuwin_settings";
    
    private static SettingsManager instance;
    private SharedPreferences prefs;
    private Context context;
    private Set<SettingsChangeListener> listeners = new HashSet<>();
    
    // Flags to track setting changes that need to be applied on resume
    private boolean visualSettingsChanged = false;
    
    // Settings Keys
    public static class Keys {
        // API Settings
        public static final String API_PROVIDER = "api_provider";
        public static final String OPENAI_API_KEY = "openai_api_key";
        public static final String OLLAMA_BASE_URL = "ollama_base_url";
        public static final String OLLAMA_MODEL = "ollama_model";
        public static final String OPENAI_MODEL = "openai_model";
        public static final String TTS_BASE_URL = "tts_base_url";
        public static final String TTS_MODEL = "tts_model";
        public static final String TTS_VOICE = "tts_voice";
        public static final String STT_BASE_URL = "stt_base_url";
        public static final String STT_MODEL = "stt_model";
        public static final String TTS_PROVIDER = "tts_provider";
        public static final String GOOGLE_TTS_LANGUAGE_CODE = "google_tts_language_code";
        public static final String GOOGLE_TTS_VOICE_NAME = "google_tts_voice_name";
        public static final String FIRECRAWL_API_KEY = "firecrawl_api_key";
        public static final String GOOGLE_API_KEY = "google_api_key";
        public static final String GOOGLE_CSE_ID = "google_cse_id";
        public static final String HOME_ASSISTANT_URL = "home_assistant_url";
        public static final String HOME_ASSISTANT_TOKEN = "home_assistant_token";
        
        // Character Settings
        public static final String DEFAULT_CHARACTER = "default_character";
        public static final String CHARACTER_PERSONALITY_PREFIX = "character_personality_";
        public static final String CHARACTER_GREETING_PREFIX = "character_greeting_";
        public static final String WAKE_WORD_ENABLED = "wake_word_enabled";
        public static final String WAKE_WORD_SENSITIVITY = "wake_word_sensitivity";
        public static final String CHARACTER_WAKE_WORD_PREFIX = "character_wake_word_";
        public static final String WAKE_WORD_TEST_MODE = "wake_word_test_mode";
        public static final String WAKE_WORD_THRESHOLD = "wake_word_threshold";
        public static final String CHARACTER_WAKE_WORD_SENSITIVITY_PREFIX = "character_wake_word_sensitivity_";
        
        // Audio Settings
        public static final String VAD_SILENCE_THRESHOLD = "vad_silence_threshold";
        public static final String VAD_MIN_SILENCE_DURATION = "vad_min_silence_duration";
        public static final String AUDIO_SAMPLE_RATE = "audio_sample_rate";
        public static final String AUDIO_BUFFER_SIZE = "audio_buffer_size";
        public static final String MIC_SENSITIVITY = "mic_sensitivity";
        public static final String SPEAKER_VOLUME = "speaker_volume";
        
        // Visual Settings
        public static final String ANIMATION_SPEED = "animation_speed";
        public static final String TOUCH_SENSITIVITY = "touch_sensitivity";
        public static final String UI_THEME = "ui_theme";
        public static final String SHOW_STATUS_TEXT = "show_status_text";
        public static final String FULLSCREEN_MODE = "fullscreen_mode";
        public static final String FULL_BODY_VIEW = "full_body_view";
        
        // Advanced Settings
        public static final String DEBUG_LOGGING = "debug_logging";
        public static final String PERFORMANCE_MODE = "performance_mode";
        public static final String AUTO_SAVE_CONVERSATIONS = "auto_save_conversations";
        public static final String CACHE_SIZE_MB = "cache_size_mb";
        public static final String CONNECTION_TIMEOUT = "connection_timeout";
        public static final String READ_TIMEOUT = "read_timeout";
        
        // VAD Follow-up Settings
        public static final String VAD_FOLLOWUP_THRESHOLD = "vad_followup_threshold";
        public static final String VAD_FOLLOWUP_DURATION = "vad_followup_duration";
        
        // Memory Settings
        public static final String MEMORY_ENABLED = "memory_enabled";
        public static final String MEMORY_MAX_STORAGE_MB = "memory_max_storage_mb";
        public static final String MEMORY_MAX_ENTRIES = "memory_max_entries";
        public static final String MEMORY_RETENTION_DAYS = "memory_retention_days";
        public static final String MEMORY_IMPORTANCE_THRESHOLD = "memory_importance_threshold";
        public static final String MEMORY_AUTO_CONSOLIDATION = "memory_auto_consolidation";
        public static final String MEMORY_EMBEDDING_MODEL = "memory_embedding_model";
        public static final String MEMORY_SIMILARITY_THRESHOLD = "memory_similarity_threshold";
        public static final String MEMORY_CACHE_SIZE = "memory_cache_size";
    }
    
    // Default Values
    public static class Defaults {
        // API Defaults
        public static final String API_PROVIDER = "OpenAI";
        public static final String OLLAMA_BASE_URL = ApiConfig.DEFAULT_OLLAMA_BASE_URL;
        public static final String OLLAMA_MODEL = "llama3.1"; // Keep Ollama default separate
        public static final String OPENAI_MODEL = "gpt-4.1-mini";
        public static final String TTS_BASE_URL = ApiConfig.DEFAULT_TTS_BASE_URL;
        public static final String TTS_MODEL = ApiConfig.DEFAULT_TTS_MODEL;
        public static final String TTS_VOICE = ApiConfig.DEFAULT_TTS_VOICE;
        public static final String STT_BASE_URL = ApiConfig.DEFAULT_STT_BASE_URL;
        public static final String STT_MODEL = ApiConfig.DEFAULT_STT_MODEL;
        public static final String TTS_PROVIDER = "OpenAI";
        public static final String GOOGLE_TTS_LANGUAGE_CODE = "en-US";
        public static final String GOOGLE_TTS_VOICE_NAME = "en-US-Wavenet-D";
        public static final String HOME_ASSISTANT_URL = "http://localhost:8123";
        
        // Character Defaults
        public static final String DEFAULT_CHARACTER = "becca_vts";
        public static final boolean WAKE_WORD_ENABLED = true;
        public static final float WAKE_WORD_SENSITIVITY = 0.5f;
        public static final String DEFAULT_WAKE_WORD_MODEL = "auto";
        public static final float WAKE_WORD_THRESHOLD = 0.4f; // Match WakeWordManager
        public static final boolean WAKE_WORD_TEST_MODE = false;
        public static final float CHARACTER_WAKE_WORD_SENSITIVITY_USE_GLOBAL = -1.0f; // Special value for "use global"
        
        // Audio Defaults
        public static final int VAD_SILENCE_THRESHOLD = 300;
        public static final long VAD_MIN_SILENCE_DURATION = 1000L;
        public static final int AUDIO_SAMPLE_RATE = 16000;
        public static final int AUDIO_BUFFER_SIZE = 4096;
        public static final float MIC_SENSITIVITY = 1.0f;
        public static final float SPEAKER_VOLUME = 1.0f;
        
        // Visual Defaults
        public static final float ANIMATION_SPEED = 1.0f;
        public static final float TOUCH_SENSITIVITY = 1.0f;
        public static final String UI_THEME = "dark";
        public static final boolean SHOW_STATUS_TEXT = true;
        public static final boolean FULLSCREEN_MODE = true;
        public static final boolean FULL_BODY_VIEW = false; // Default to current waist-up view
        
        // Advanced Defaults
        public static final boolean DEBUG_LOGGING = false;
        public static final String PERFORMANCE_MODE = "balanced";
        public static final boolean AUTO_SAVE_CONVERSATIONS = true;
        public static final int CACHE_SIZE_MB = 100;
        public static final int CONNECTION_TIMEOUT = ApiConfig.CONNECT_TIMEOUT;
        public static final int READ_TIMEOUT = ApiConfig.READ_TIMEOUT;
        
        // VAD Follow-up Defaults
        public static final int VAD_FOLLOWUP_THRESHOLD = 300; // Same sensitivity as initial listening
        public static final long VAD_FOLLOWUP_DURATION = 2000L; // Longer duration for follow-up
        
        // Memory Defaults
        public static final boolean MEMORY_ENABLED = true; // Disabled by default until model is downloaded
        public static final int MEMORY_MAX_STORAGE_MB = 200; // 200MB max storage
        public static final int MEMORY_MAX_ENTRIES = 100000; // 100k memories max
        public static final int MEMORY_RETENTION_DAYS = 365; // Keep memories for 1 year
        public static final float MEMORY_IMPORTANCE_THRESHOLD = 0.3f; // Minimum importance to save
        public static final boolean MEMORY_AUTO_CONSOLIDATION = true; // Auto-merge similar memories
        public static final String MEMORY_EMBEDDING_MODEL = "bert_text_embedder"; // MediaPipe model
        public static final float MEMORY_SIMILARITY_THRESHOLD = 0.8f; // Similarity threshold for duplicates
        public static final int MEMORY_CACHE_SIZE = 1000; // Cache 1000 recent memories in RAM
    }
    
    public interface SettingsChangeListener {
        void onSettingChanged(String key, Object value);
    }
    
    public static SettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsManager(context.getApplicationContext());
        }
        return instance;
    }
    
    private SettingsManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "SettingsManager initialized");
    }
    
    // String Settings
    public String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }
    
    public void setString(String key, String value) {
        prefs.edit().putString(key, value).apply();
        notifyListeners(key, value);
        Log.d(TAG, "Setting updated: " + key + " = " + value);
    }
    
    // Integer Settings
    public int getInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }
    
    public void setInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
        notifyListeners(key, value);
        Log.d(TAG, "Setting updated: " + key + " = " + value);
    }
    
    // Float Settings
    public float getFloat(String key, float defaultValue) {
        return prefs.getFloat(key, defaultValue);
    }
    
    public void setFloat(String key, float value) {
        prefs.edit().putFloat(key, value).apply();
        notifyListeners(key, value);
        Log.d(TAG, "Setting updated: " + key + " = " + value);
    }
    
    // Boolean Settings
    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }
    
    public void setBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
        notifyListeners(key, value);
        Log.d(TAG, "Setting updated: " + key + " = " + value);
    }
    
    // Long Settings
    public long getLong(String key, long defaultValue) {
        return prefs.getLong(key, defaultValue);
    }
    
    public void setLong(String key, long value) {
        prefs.edit().putLong(key, value).apply();
        notifyListeners(key, value);
        Log.d(TAG, "Setting updated: " + key + " = " + value);
    }
    
    // Convenience methods for common settings
    public String getOllamaBaseUrl() {
        return getString(Keys.OLLAMA_BASE_URL, Defaults.OLLAMA_BASE_URL);
    }
    
    public String getOllamaModel() {
        return getString(Keys.OLLAMA_MODEL, Defaults.OLLAMA_MODEL);
    }
    
    public String getTtsBaseUrl() {
        return getString(Keys.TTS_BASE_URL, Defaults.TTS_BASE_URL);
    }
    
    public String getTtsModel() {
        return getString(Keys.TTS_MODEL, Defaults.TTS_MODEL);
    }
    
    public String getTtsVoice() {
        return getString(Keys.TTS_VOICE, Defaults.TTS_VOICE);
    }
    
    public String getSttBaseUrl() {
        return getString(Keys.STT_BASE_URL, Defaults.STT_BASE_URL);
    }
    
    public String getSttModel() {
        return getString(Keys.STT_MODEL, Defaults.STT_MODEL);
    }

    public String getTtsProvider() {
        return getString(Keys.TTS_PROVIDER, Defaults.TTS_PROVIDER);
    }

    public void setTtsProvider(String provider) {
        setString(Keys.TTS_PROVIDER, provider);
    }

    public String getGoogleTtsLanguageCode() {
        return getString(Keys.GOOGLE_TTS_LANGUAGE_CODE, Defaults.GOOGLE_TTS_LANGUAGE_CODE);
    }

    public void setGoogleTtsLanguageCode(String languageCode) {
        setString(Keys.GOOGLE_TTS_LANGUAGE_CODE, languageCode);
    }

    public String getGoogleTtsVoiceName() {
        return getString(Keys.GOOGLE_TTS_VOICE_NAME, Defaults.GOOGLE_TTS_VOICE_NAME);
    }

    public void setGoogleTtsVoiceName(String voiceName) {
        setString(Keys.GOOGLE_TTS_VOICE_NAME, voiceName);
    }
    
    // OpenAI settings
    public String getOpenAIModel() {
        return getString(Keys.OPENAI_MODEL, Defaults.OPENAI_MODEL);
    }

    public String getOpenAIApiKey() {
        return getString(Keys.OPENAI_API_KEY, "");
    }

    public void setOpenAIApiKey(String apiKey) {
        setString(Keys.OPENAI_API_KEY, apiKey);
    }

    public String getApiProvider() {
        return getString(Keys.API_PROVIDER, Defaults.API_PROVIDER);
    }

    public void setApiProvider(String provider) {
        setString(Keys.API_PROVIDER, provider);
    }
    
    public String getDefaultCharacter() {
        return getString(Keys.DEFAULT_CHARACTER, Defaults.DEFAULT_CHARACTER);
    }
    
    public boolean isWakeWordEnabled() {
        return getBoolean(Keys.WAKE_WORD_ENABLED, Defaults.WAKE_WORD_ENABLED);
    }
    
    public float getWakeWordSensitivity() {
        return getFloat(Keys.WAKE_WORD_SENSITIVITY, Defaults.WAKE_WORD_SENSITIVITY);
    }
    
    public int getVadSilenceThreshold() {
        return getInt(Keys.VAD_SILENCE_THRESHOLD, Defaults.VAD_SILENCE_THRESHOLD);
    }
    
    public long getVadMinSilenceDuration() {
        return getLong(Keys.VAD_MIN_SILENCE_DURATION, Defaults.VAD_MIN_SILENCE_DURATION);
    }
    
    public float getAnimationSpeed() {
        return getFloat(Keys.ANIMATION_SPEED, Defaults.ANIMATION_SPEED);
    }
    
    public float getTouchSensitivity() {
        return getFloat(Keys.TOUCH_SENSITIVITY, Defaults.TOUCH_SENSITIVITY);
    }
    
    public boolean isDebugLoggingEnabled() {
        return getBoolean(Keys.DEBUG_LOGGING, Defaults.DEBUG_LOGGING);
    }
    
    public boolean isFullscreenMode() {
        return getBoolean(Keys.FULLSCREEN_MODE, Defaults.FULLSCREEN_MODE);
    }
    
    public boolean isFullBodyView() {
        return getBoolean(Keys.FULL_BODY_VIEW, Defaults.FULL_BODY_VIEW);
    }
    
    public int getConnectionTimeout() {
        return getInt(Keys.CONNECTION_TIMEOUT, Defaults.CONNECTION_TIMEOUT);
    }
    
    public int getReadTimeout() {
        return getInt(Keys.READ_TIMEOUT, Defaults.READ_TIMEOUT);
    }
    
    // VAD Follow-up settings
    public int getVadFollowupThreshold() {
        return getInt(Keys.VAD_FOLLOWUP_THRESHOLD, Defaults.VAD_FOLLOWUP_THRESHOLD);
    }
    
    public long getVadFollowupDuration() {
        return getLong(Keys.VAD_FOLLOWUP_DURATION, Defaults.VAD_FOLLOWUP_DURATION);
    }
    
    // Character-specific settings
    public String getCharacterPersonality(String characterId) {
        return getString(Keys.CHARACTER_PERSONALITY_PREFIX + characterId, "");
    }
    
    public void setCharacterPersonality(String characterId, String personality) {
        setString(Keys.CHARACTER_PERSONALITY_PREFIX + characterId, personality);
    }
    
    public String getCharacterGreeting(String characterId) {
        return getString(Keys.CHARACTER_GREETING_PREFIX + characterId, "");
    }
    
    public void setCharacterGreeting(String characterId, String greeting) {
        setString(Keys.CHARACTER_GREETING_PREFIX + characterId, greeting);
    }
    
    // Wake word settings
    public float getWakeWordThreshold() {
        return getFloat(Keys.WAKE_WORD_THRESHOLD, Defaults.WAKE_WORD_THRESHOLD);
    }
    
    public void setWakeWordThreshold(float threshold) {
        setFloat(Keys.WAKE_WORD_THRESHOLD, threshold);
    }
    
    public boolean isWakeWordTestMode() {
        return getBoolean(Keys.WAKE_WORD_TEST_MODE, Defaults.WAKE_WORD_TEST_MODE);
    }
    
    public void setWakeWordTestMode(boolean testMode) {
        setBoolean(Keys.WAKE_WORD_TEST_MODE, testMode);
    }
    
    public String getCharacterWakeWordModel(String characterId) {
        return getString(Keys.CHARACTER_WAKE_WORD_PREFIX + characterId, Defaults.DEFAULT_WAKE_WORD_MODEL);
    }
    
    public void setCharacterWakeWordModel(String characterId, String model) {
        setString(Keys.CHARACTER_WAKE_WORD_PREFIX + characterId, model);
    }
    
    public float getCharacterWakeWordSensitivity(String characterId) {
        return getFloat(Keys.CHARACTER_WAKE_WORD_SENSITIVITY_PREFIX + characterId, Defaults.CHARACTER_WAKE_WORD_SENSITIVITY_USE_GLOBAL);
    }
    
    public void setCharacterWakeWordSensitivity(String characterId, float sensitivity) {
        setFloat(Keys.CHARACTER_WAKE_WORD_SENSITIVITY_PREFIX + characterId, sensitivity);
    }
    
    /**
     * Get effective wake word sensitivity for a character (character-specific or global)
     */
    public float getEffectiveWakeWordSensitivity(String characterId) {
        float characterSensitivity = getCharacterWakeWordSensitivity(characterId);
        if (characterSensitivity == Defaults.CHARACTER_WAKE_WORD_SENSITIVITY_USE_GLOBAL) {
            return getWakeWordSensitivity(); // Use global setting
        }
        return characterSensitivity; // Use character-specific setting
    }
    
    // Memory settings convenience methods
    public boolean isMemoryEnabled() {
        return getBoolean(Keys.MEMORY_ENABLED, Defaults.MEMORY_ENABLED);
    }
    
    public void setMemoryEnabled(boolean enabled) {
        setBoolean(Keys.MEMORY_ENABLED, enabled);
    }
    
    public int getMemoryMaxStorageMB() {
        return getInt(Keys.MEMORY_MAX_STORAGE_MB, Defaults.MEMORY_MAX_STORAGE_MB);
    }
    
    public void setMemoryMaxStorageMB(int maxStorageMB) {
        setInt(Keys.MEMORY_MAX_STORAGE_MB, maxStorageMB);
    }
    
    public int getMemoryMaxEntries() {
        return getInt(Keys.MEMORY_MAX_ENTRIES, Defaults.MEMORY_MAX_ENTRIES);
    }
    
    public void setMemoryMaxEntries(int maxEntries) {
        setInt(Keys.MEMORY_MAX_ENTRIES, maxEntries);
    }
    
    public int getMemoryRetentionDays() {
        return getInt(Keys.MEMORY_RETENTION_DAYS, Defaults.MEMORY_RETENTION_DAYS);
    }
    
    public void setMemoryRetentionDays(int retentionDays) {
        setInt(Keys.MEMORY_RETENTION_DAYS, retentionDays);
    }
    
    public float getMemoryImportanceThreshold() {
        return getFloat(Keys.MEMORY_IMPORTANCE_THRESHOLD, Defaults.MEMORY_IMPORTANCE_THRESHOLD);
    }
    
    public void setMemoryImportanceThreshold(float threshold) {
        setFloat(Keys.MEMORY_IMPORTANCE_THRESHOLD, threshold);
    }
    
    public boolean isMemoryAutoConsolidationEnabled() {
        return getBoolean(Keys.MEMORY_AUTO_CONSOLIDATION, Defaults.MEMORY_AUTO_CONSOLIDATION);
    }
    
    public void setMemoryAutoConsolidationEnabled(boolean enabled) {
        setBoolean(Keys.MEMORY_AUTO_CONSOLIDATION, enabled);
    }
    
    public String getMemoryEmbeddingModel() {
        return getString(Keys.MEMORY_EMBEDDING_MODEL, Defaults.MEMORY_EMBEDDING_MODEL);
    }
    
    public void setMemoryEmbeddingModel(String model) {
        setString(Keys.MEMORY_EMBEDDING_MODEL, model);
    }
    
    public float getMemorySimilarityThreshold() {
        return getFloat(Keys.MEMORY_SIMILARITY_THRESHOLD, Defaults.MEMORY_SIMILARITY_THRESHOLD);
    }
    
    public void setMemorySimilarityThreshold(float threshold) {
        setFloat(Keys.MEMORY_SIMILARITY_THRESHOLD, threshold);
    }
    
    public int getMemoryCacheSize() {
        return getInt(Keys.MEMORY_CACHE_SIZE, Defaults.MEMORY_CACHE_SIZE);
    }
    
    public void setMemoryCacheSize(int cacheSize) {
        setInt(Keys.MEMORY_CACHE_SIZE, cacheSize);
    }
    
    // Settings validation
    public boolean isValidUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }
    
    public boolean isValidPort(int port) {
        return port > 0 && port <= 65535;
    }
    
    public boolean isValidSensitivity(float sensitivity) {
        return sensitivity >= 0.0f && sensitivity <= 2.0f;
    }
    
    public boolean isValidVadThreshold(int threshold) {
        return threshold >= 100 && threshold <= 1000; // Reasonable range for RMS values
    }
    
    public boolean isValidVadDuration(long duration) {
        return duration >= 500L && duration <= 10000L; // 0.5 to 10 seconds
    }
    
    // Reset settings
    public void resetToDefaults() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "All settings reset to defaults");
        notifyListeners("reset", null);
    }
    
    public void resetCategory(String category) {
        SharedPreferences.Editor editor = prefs.edit();
        
        switch (category) {
            case "api":
                editor.remove(Keys.OLLAMA_BASE_URL);
                editor.remove(Keys.OLLAMA_MODEL);
                editor.remove(Keys.TTS_BASE_URL);
                editor.remove(Keys.TTS_MODEL);
                editor.remove(Keys.TTS_VOICE);
                editor.remove(Keys.STT_BASE_URL);
                editor.remove(Keys.STT_MODEL);
                break;
            case "character":
                editor.remove(Keys.DEFAULT_CHARACTER);
                editor.remove(Keys.WAKE_WORD_ENABLED);
                editor.remove(Keys.WAKE_WORD_SENSITIVITY);
                break;
            case "audio":
                editor.remove(Keys.VAD_SILENCE_THRESHOLD);
                editor.remove(Keys.VAD_MIN_SILENCE_DURATION);
                editor.remove(Keys.MIC_SENSITIVITY);
                editor.remove(Keys.SPEAKER_VOLUME);
                break;
            case "visual":
                editor.remove(Keys.ANIMATION_SPEED);
                editor.remove(Keys.TOUCH_SENSITIVITY);
                editor.remove(Keys.UI_THEME);
                editor.remove(Keys.FULLSCREEN_MODE);
                break;
        }
        
        editor.apply();
        Log.d(TAG, "Settings category reset: " + category);
        notifyListeners("reset_" + category, null);
    }
    
    // Listener management
    public void addSettingsChangeListener(SettingsChangeListener listener) {
        listeners.add(listener);
    }
    
    public void removeSettingsChangeListener(SettingsChangeListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyListeners(String key, Object value) {
        for (SettingsChangeListener listener : listeners) {
            try {
                listener.onSettingChanged(key, value);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying settings listener", e);
            }
        }
    }
    
    // Export/Import settings
    public String exportSettings() {
        // TODO: Implement settings export to JSON
        return "";
    }
    
    public boolean importSettings(String settingsJson) {
        // TODO: Implement settings import from JSON
        return false;
    }
    
    // Migration support
    public void migrateSettings(int fromVersion, int toVersion) {
        Log.d(TAG, "Migrating settings from version " + fromVersion + " to " + toVersion);
        // TODO: Implement settings migration logic
    }
    
    // Visual settings change tracking
    public void markVisualSettingsChanged() {
        visualSettingsChanged = true;
        Log.d(TAG, "Visual settings marked as changed");
    }
    
    public boolean hasVisualSettingsChanged() {
        return visualSettingsChanged;
    }
    
    public void clearVisualSettingsChanged() {
        visualSettingsChanged = false;
        Log.d(TAG, "Visual settings change flag cleared");
    }
}

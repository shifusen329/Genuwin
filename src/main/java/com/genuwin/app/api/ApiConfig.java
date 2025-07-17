package com.genuwin.app.api;

import android.content.Context;
import com.genuwin.app.settings.SettingsManager;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration class for API endpoints
 */
public class ApiConfig {
    // Default API endpoints - can be overridden by settings
    public static final String DEFAULT_OLLAMA_BASE_URL = "http://10.0.2.2:11434";
    public static final String OLLAMA_CHAT_ENDPOINT = "/api/chat";
    public static final String OLLAMA_GENERATE_ENDPOINT = "/api/generate";
    
    // Default TTS API endpoint
    public static final String DEFAULT_TTS_BASE_URL = "https://api.openai.com/v1";
    public static final String TTS_ENDPOINT = "/audio/speech";
    
    // Default STT API endpoint
    public static final String DEFAULT_STT_BASE_URL = "https://api.openai.com/v1";
    public static final String STT_ENDPOINT = "/audio/transcriptions";

    // Default Firecrawl API endpoint
    public static final String DEFAULT_FIRECRAWL_BASE_URL = "https://api.firecrawl.dev/";
    
    // Default Google Custom Search API endpoint
    public static final String DEFAULT_GOOGLE_SEARCH_BASE_URL = "https://www.googleapis.com/customsearch/v1/";
    
    // Default OpenAI API endpoint
    public static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/";
    public static final String OPENAI_RESPONSES_ENDPOINT = "/v1/responses";
    
    // Get configurable endpoints from settings
    public static String getOllamaBaseUrl(Context context) {
        SettingsManager settingsManager = SettingsManager.getInstance(context);
        return settingsManager.getOllamaBaseUrl();
    }
    
    public static String getTtsBaseUrl(Context context) {
        SettingsManager settingsManager = SettingsManager.getInstance(context);
        return settingsManager.getTtsBaseUrl();
    }
    
    public static String getSttBaseUrl(Context context) {
        SettingsManager settingsManager = SettingsManager.getInstance(context);
        return settingsManager.getSttBaseUrl();
    }
    
    public static String getOpenAIBaseUrl(Context context) {
        return DEFAULT_OPENAI_BASE_URL; // OpenAI base URL is typically fixed
    }
    
    // Default model names
    public static final String DEFAULT_LLM_MODEL = "gpt-4.1-mini";
    public static final String DEFAULT_OPENAI_MODEL = "gpt-4.1-mini";
    public static final String DEFAULT_MEMORY_MODEL = "gpt-4.1-nano";
    public static final String DEFAULT_TTS_MODEL = "kokoro";
    public static final String DEFAULT_TTS_VOICE = "af_v0";
    public static final String DEFAULT_STT_MODEL = "whisper-v2";
    
    // Request timeouts
    public static final int CONNECT_TIMEOUT = 30; // seconds
    public static final int READ_TIMEOUT = 60; // seconds
    public static final int WRITE_TIMEOUT = 60; // seconds

    public static String getFirecrawlApiKey(Context context) {
        Properties properties = new Properties();
        try {
            InputStream inputStream = context.getAssets().open("config.properties");
            properties.load(inputStream);
            return properties.getProperty("FIRECRAWL_API_KEY");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static String getGoogleApiKey(Context context) {
        Properties properties = new Properties();
        try {
            InputStream inputStream = context.getAssets().open("config.properties");
            properties.load(inputStream);
            return properties.getProperty("GOOGLE_API_KEY");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static String getGoogleCseId(Context context) {
        Properties properties = new Properties();
        try {
            InputStream inputStream = context.getAssets().open("config.properties");
            properties.load(inputStream);
            return properties.getProperty("GOOGLE_CSE_ID");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static String getOpenAIApiKey(Context context) {
        // First, try to get the API key from SettingsManager
        SettingsManager settingsManager = SettingsManager.getInstance(context);
        String userApiKey = settingsManager.getOpenAIApiKey();
        
        if (userApiKey != null && !userApiKey.trim().isEmpty()) {
            return userApiKey;
        }
        
        // Fallback to config.properties if not found in settings
        Properties properties = new Properties();
        try {
            InputStream inputStream = context.getAssets().open("config.properties");
            properties.load(inputStream);
            return properties.getProperty("OPENAI_API_KEY");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static String getAudioApiKey(Context context) {
        Properties properties = new Properties();
        try {
            InputStream inputStream = context.getAssets().open("config.properties");
            properties.load(inputStream);
            return properties.getProperty("AUDIO_API_KEY");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

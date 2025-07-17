package com.genuwin.app.settings;

import com.genuwin.app.settings.SettingsManager.Keys;
import com.genuwin.app.settings.SettingsManager.Defaults;

/**
 * Settings fragment for API configuration
 */
public class ApiSettingsFragment extends BaseSettingsFragment {
    
    @Override
    protected void loadSettings() {
        settingsItems.clear();
        
        // API Provider
        settingsItems.add(createHeaderItem("API Provider"));
        settingsItems.add(createSpinnerItem(
            Keys.API_PROVIDER,
            "API Provider",
            "Select the API provider for LLM services",
            new String[]{"OpenAI", "Ollama"},
            Defaults.API_PROVIDER
        ));

        // Conditional settings based on provider
        String currentProvider = settingsManager.getApiProvider();
        if ("Ollama".equals(currentProvider)) {
            // Ollama Settings
            settingsItems.add(createHeaderItem("Ollama LLM"));
            settingsItems.add(createEditTextItem(
                Keys.OLLAMA_BASE_URL,
                "Ollama Server URL",
                "Base URL for Ollama API server",
                Defaults.OLLAMA_BASE_URL
            ));
            settingsItems.add(createSpinnerItem(
                Keys.OLLAMA_MODEL,
                "Ollama Model",
                "Language model to use for conversations",
                new String[]{"llama3.1", "llama3.2", "llama2", "codellama", "mistral", "phi3"},
                Defaults.OLLAMA_MODEL
            ));
        } else {
            // OpenAI Settings
            settingsItems.add(createHeaderItem("OpenAI LLM"));
            settingsItems.add(createEditTextItem(
                Keys.OPENAI_API_KEY,
                "OpenAI API Key",
                "Your personal OpenAI API key",
                ""
            ));
            settingsItems.add(createSpinnerItem(
                Keys.OPENAI_MODEL,
                "OpenAI Model",
                "Language model to use for conversations",
                new String[]{"gpt-4.1-mini", "gpt-4o", "gpt-4-turbo", "gpt-3.5-turbo"},
                Defaults.OPENAI_MODEL
            ));
        }
        
        // TTS Settings
        settingsItems.add(createHeaderItem("Text-to-Speech"));
        settingsItems.add(createSpinnerItem(
            Keys.TTS_PROVIDER,
            "TTS Provider",
            "Select the Text-to-Speech provider",
            new String[]{"OpenAI", "Google"},
            Defaults.TTS_PROVIDER
        ));

        String ttsProvider = settingsManager.getTtsProvider();
        if ("Google".equals(ttsProvider)) {
            settingsItems.add(createEditTextItem(
                Keys.GOOGLE_TTS_LANGUAGE_CODE,
                "Google TTS Language Code",
                "e.g., en-US, en-GB, es-ES",
                Defaults.GOOGLE_TTS_LANGUAGE_CODE
            ));
            settingsItems.add(createEditTextItem(
                Keys.GOOGLE_TTS_VOICE_NAME,
                "Google TTS Voice Name",
                "e.g., en-US-Wavenet-D",
                Defaults.GOOGLE_TTS_VOICE_NAME
            ));
        } else {
            settingsItems.add(createEditTextItem(
                Keys.TTS_BASE_URL,
                "TTS Server URL",
                "Base URL for TTS API server",
                Defaults.TTS_BASE_URL
            ));
            settingsItems.add(createSpinnerItem(
                Keys.TTS_MODEL,
                "TTS Model",
                "Text-to-speech model to use",
                new String[]{"tts-1", "tts-1-hd", "kokoro", "af_heart"},
                Defaults.TTS_MODEL
            ));
            settingsItems.add(createSpinnerItem(
                Keys.TTS_VOICE,
                "TTS Voice",
                "Voice to use for speech synthesis",
                new String[]{"af_heart", "af_bella", "af_sarah", "af_nicole", "alloy", "echo", "fable", "onyx", "nova", "shimmer"},
                Defaults.TTS_VOICE
            ));
        }
        
        // STT Settings
        settingsItems.add(createHeaderItem("Speech-to-Text"));
        settingsItems.add(createEditTextItem(
            Keys.STT_BASE_URL,
            "STT Server URL",
            "Base URL for STT API server",
            Defaults.STT_BASE_URL
        ));
        settingsItems.add(createSpinnerItem(
            Keys.STT_MODEL,
            "STT Model",
            "Speech-to-text model to use",
            new String[]{"whisper-1", "kokoro", "faster-whisper"},
            Defaults.STT_MODEL
        ));
        
        // External APIs
        settingsItems.add(createHeaderItem("External APIs"));
        settingsItems.add(createEditTextItem(
            Keys.FIRECRAWL_API_KEY,
            "Firecrawl API Key",
            "API key for Firecrawl web scraping service",
            ""
        ));
        settingsItems.add(createEditTextItem(
            Keys.GOOGLE_API_KEY,
            "Google API Key",
            "API key for Google Custom Search",
            ""
        ));
        settingsItems.add(createEditTextItem(
            Keys.GOOGLE_CSE_ID,
            "Google CSE ID",
            "Custom Search Engine ID for Google Search",
            ""
        ));
        
        // Home Assistant
        settingsItems.add(createHeaderItem("Home Assistant"));
        settingsItems.add(createEditTextItem(
            Keys.HOME_ASSISTANT_URL,
            "Home Assistant URL",
            "Base URL for Home Assistant instance",
            Defaults.HOME_ASSISTANT_URL
        ));
        settingsItems.add(createEditTextItem(
            Keys.HOME_ASSISTANT_TOKEN,
            "Home Assistant Token",
            "Long-lived access token for Home Assistant",
            ""
        ));
        
        // Connection Settings
        settingsItems.add(createHeaderItem("Connection"));
        settingsItems.add(createSliderItem(
            Keys.CONNECTION_TIMEOUT,
            "Connection Timeout",
            "Timeout for API connections (seconds)",
            Defaults.CONNECTION_TIMEOUT,
            5.0f,
            120.0f,
            5.0f
        ));
        settingsItems.add(createSliderItem(
            Keys.READ_TIMEOUT,
            "Read Timeout",
            "Timeout for reading API responses (seconds)",
            Defaults.READ_TIMEOUT,
            10.0f,
            300.0f,
            10.0f
        ));
        
        // Test Connection Button
        settingsItems.add(createButtonItem(
            "test_connection",
            "Test Connections",
            "Test connectivity to all configured services"
        ));
        
        adapter.notifyDataSetChanged();
    }
    
    @Override
    protected void handleButtonClick(SettingsItem item) {
        if ("test_connection".equals(item.key)) {
            testConnections();
        }
    }

    @Override
    protected void onSettingChangedCustom(String key, Object value) {
        // Reload settings to show/hide provider-specific options
        if (Keys.API_PROVIDER.equals(key) || Keys.TTS_PROVIDER.equals(key)) {
            android.util.Log.d("ApiSettingsFragment", "Reloading settings for key: " + key);
            loadSettings();
            adapter.notifyDataSetChanged();
        }
    }
    
    private void testConnections() {
        // TODO: Implement connection testing
        SettingsDialogs.showInfoDialog(getContext(), "Connection Test", 
            "Connection testing will be implemented in a future update.");
    }
    
    @Override
    protected boolean validateSetting(String key, String value) {
        switch (key) {
            case Keys.OLLAMA_BASE_URL:
            case Keys.TTS_BASE_URL:
            case Keys.STT_BASE_URL:
            case Keys.HOME_ASSISTANT_URL:
                return settingsManager.isValidUrl(value);
            case Keys.FIRECRAWL_API_KEY:
            case Keys.GOOGLE_API_KEY:
            case Keys.GOOGLE_CSE_ID:
            case Keys.HOME_ASSISTANT_TOKEN:
                return value != null && !value.trim().isEmpty();
            default:
                return super.validateSetting(key, value);
        }
    }
}

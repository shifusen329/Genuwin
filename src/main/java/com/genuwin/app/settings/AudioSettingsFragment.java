package com.genuwin.app.settings;

import com.genuwin.app.managers.AudioTestManager;
import com.genuwin.app.settings.SettingsManager.Keys;
import com.genuwin.app.settings.SettingsManager.Defaults;

/**
 * Settings fragment for audio configuration
 */
public class AudioSettingsFragment extends BaseSettingsFragment {
    
    private AudioTestManager audioTestManager;
    
    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioTestManager = new AudioTestManager(requireContext());
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioTestManager != null) {
            audioTestManager.release();
        }
    }
    
    @Override
    protected void loadSettings() {
        settingsItems.clear();
        
        // Voice Activity Detection
        settingsItems.add(createHeaderItem("Voice Activity Detection"));
        settingsItems.add(createSliderItem(
            Keys.VAD_SILENCE_THRESHOLD,
            "Silence Threshold",
            "Audio level threshold for detecting silence",
            Defaults.VAD_SILENCE_THRESHOLD,
            100.0f,
            5000.0f,
            100.0f
        ));
        settingsItems.add(createSliderItem(
            Keys.VAD_MIN_SILENCE_DURATION,
            "Min Silence Duration",
            "Minimum silence duration to stop recording (ms)",
            Defaults.VAD_MIN_SILENCE_DURATION,
            500.0f,
            5000.0f,
            250.0f
        ));
        
        // Audio Quality
        settingsItems.add(createHeaderItem("Audio Quality"));
        settingsItems.add(createSpinnerItem(
            Keys.AUDIO_SAMPLE_RATE,
            "Sample Rate",
            "Audio sample rate for recording",
            new String[]{"8000", "16000", "22050", "44100", "48000"},
            String.valueOf(Defaults.AUDIO_SAMPLE_RATE)
        ));
        settingsItems.add(createSpinnerItem(
            Keys.AUDIO_BUFFER_SIZE,
            "Buffer Size",
            "Audio buffer size for processing",
            new String[]{"1024", "2048", "4096", "8192"},
            String.valueOf(Defaults.AUDIO_BUFFER_SIZE)
        ));
        
        // Input/Output Levels
        settingsItems.add(createHeaderItem("Audio Levels"));
        settingsItems.add(createSliderItem(
            Keys.MIC_SENSITIVITY,
            "Microphone Sensitivity",
            "Microphone input sensitivity multiplier",
            Defaults.MIC_SENSITIVITY,
            0.1f,
            3.0f,
            0.1f
        ));
        settingsItems.add(createSliderItem(
            Keys.SPEAKER_VOLUME,
            "Speaker Volume",
            "Speaker output volume multiplier",
            Defaults.SPEAKER_VOLUME,
            0.1f,
            2.0f,
            0.1f
        ));
        
        // Audio Testing
        settingsItems.add(createHeaderItem("Audio Testing"));
        settingsItems.add(createButtonItem(
            "test_microphone",
            "Test Microphone",
            "Test microphone input and sensitivity"
        ));
        settingsItems.add(createButtonItem(
            "test_speakers",
            "Test Speakers",
            "Test speaker output and volume"
        ));
        settingsItems.add(createButtonItem(
            "calibrate_audio",
            "Auto Calibrate",
            "Automatically calibrate audio levels"
        ));
        
        // Advanced Audio
        settingsItems.add(createHeaderItem("Advanced"));
        settingsItems.add(createButtonItem(
            "reset_audio",
            "Reset Audio Settings",
            "Reset all audio settings to defaults"
        ));
        
        adapter.notifyDataSetChanged();
    }
    
    @Override
    protected void handleButtonClick(SettingsItem item) {
        switch (item.key) {
            case "test_microphone":
                testMicrophone();
                break;
            case "test_speakers":
                testSpeakers();
                break;
            case "calibrate_audio":
                calibrateAudio();
                break;
            case "reset_audio":
                resetAudioSettings();
                break;
        }
    }
    
    private void testMicrophone() {
        if (audioTestManager != null) {
            AudioTestDialogs.showMicrophoneTestDialog(getContext(), audioTestManager);
        } else {
            SettingsDialogs.showInfoDialog(getContext(), "Error", 
                "Audio test manager not available. Please restart the app and try again.");
        }
    }
    
    private void testSpeakers() {
        if (audioTestManager != null) {
            AudioTestDialogs.showSpeakerTestDialog(getContext(), audioTestManager);
        } else {
            SettingsDialogs.showInfoDialog(getContext(), "Error", 
                "Audio test manager not available. Please restart the app and try again.");
        }
    }
    
    private void calibrateAudio() {
        if (audioTestManager != null) {
            AudioTestDialogs.showAutoCalibrationDialog(getContext(), audioTestManager);
        } else {
            SettingsDialogs.showInfoDialog(getContext(), "Error", 
                "Audio test manager not available. Please restart the app and try again.");
        }
    }
    
    private void resetAudioSettings() {
        SettingsDialogs.showConfirmationDialog(getContext(), 
            "Reset Audio Settings", 
            "Are you sure you want to reset all audio settings to their defaults?",
            () -> {
                settingsManager.resetCategory("audio");
                loadSettings(); // Refresh the UI
                SettingsDialogs.showInfoDialog(getContext(), "Reset Complete", 
                    "All audio settings have been reset to defaults.");
            });
    }
    
    @Override
    protected void handleSliderClick(SettingsItem item) {
        // Custom handling for audio sliders with specific ranges
        float currentValue = settingsManager.getFloat(item.key, item.defaultFloatValue);
        
        // Adjust ranges and step sizes for specific audio settings
        float minValue = item.minValue;
        float maxValue = item.maxValue;
        float stepSize = item.stepSize;
        
        switch (item.key) {
            case Keys.VAD_SILENCE_THRESHOLD:
                // Integer values for threshold
                SettingsDialogs.showSliderDialog(getContext(), item.title, currentValue, 
                    minValue, maxValue, stepSize,
                    (newValue) -> {
                        settingsManager.setInt(item.key, Math.round(newValue));
                        item.value = Math.round(newValue);
                        adapter.notifyDataSetChanged();
                    });
                return;
            case Keys.VAD_MIN_SILENCE_DURATION:
                // Integer values for duration
                SettingsDialogs.showSliderDialog(getContext(), item.title, currentValue, 
                    minValue, maxValue, stepSize,
                    (newValue) -> {
                        settingsManager.setLong(item.key, Math.round(newValue));
                        item.value = Math.round(newValue);
                        adapter.notifyDataSetChanged();
                    });
                return;
        }
        
        // Default slider handling for float values
        super.handleSliderClick(item);
    }
    
    @Override
    protected boolean validateSetting(String key, String value) {
        switch (key) {
            case Keys.AUDIO_SAMPLE_RATE:
                try {
                    int sampleRate = Integer.parseInt(value);
                    return sampleRate >= 8000 && sampleRate <= 48000;
                } catch (NumberFormatException e) {
                    return false;
                }
            case Keys.AUDIO_BUFFER_SIZE:
                try {
                    int bufferSize = Integer.parseInt(value);
                    return bufferSize >= 1024 && bufferSize <= 8192;
                } catch (NumberFormatException e) {
                    return false;
                }
            default:
                return super.validateSetting(key, value);
        }
    }
}

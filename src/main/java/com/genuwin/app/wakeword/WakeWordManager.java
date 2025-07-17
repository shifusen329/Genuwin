package com.genuwin.app.wakeword;

import android.content.Context;
import android.util.Log;

import com.genuwin.app.managers.AudioManager;
import com.genuwin.app.managers.CharacterManager;
import com.genuwin.app.settings.SettingsManager;

public class WakeWordManager {
    private static final String TAG = "WakeWordManager";

    private final Context context;
    private final AudioManager audioManager;
    private final SettingsManager settingsManager;
    private MicroWakeWordDetector detector;
    private final WakeWordListener listener;
    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;
    private Thread detectionThread;
    private final byte[] audioBuffer = new byte[3072];
    private int bufferOffset = 0;
    private final float[] predictionHistory = new float[3];
    private int predictionIndex = 0;
    private String currentModelPath;
    
    // Test mode support
    private volatile boolean testMode = false;
    private TestModeListener testListener;
    private volatile float lastConfidence = 0.0f;

    public interface WakeWordListener {
        void onWakeWordDetected();
    }
    
    public interface TestModeListener {
        void onConfidenceUpdate(float confidence);
        void onWakeWordDetected(float confidence);
        void onTestModeStarted();
        void onTestModeStopped();
    }

    public WakeWordManager(Context context, WakeWordListener listener) {
        this.context = context;
        this.audioManager = new AudioManager(context);
        this.settingsManager = SettingsManager.getInstance(context);
        this.listener = listener;
        
        // Initialize with current character's wake word model
        initializeDetector();
    }
    
    /**
     * Initialize the detector with the current character's wake word model
     */
    private void initializeDetector() {
        try {
            // Get current character's wake word model path
            com.genuwin.app.managers.CharacterManager characterManager = 
                com.genuwin.app.managers.CharacterManager.getInstance(context);
            String modelPath = characterManager.getCurrentCharacterWakeWordModel();
            
            if (!modelPath.equals(currentModelPath)) {
                currentModelPath = modelPath;
                detector = new MicroWakeWordDetector(context, modelPath);
                
                if (com.genuwin.app.live2d.WaifuDefine.DEBUG_LOG_ENABLE) {
                    Log.d(TAG, "Initialized wake word detector with model: " + modelPath);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize wake word detector", e);
            // Fallback to default model
            currentModelPath = "wakeword/hey_haru.tflite";
            detector = new MicroWakeWordDetector(context, currentModelPath);
        }
    }
    
    /**
     * Switch to a different wake word model
     */
    public void switchWakeWordModel(String modelPath) {
        if (modelPath == null || modelPath.equals(currentModelPath)) {
            return;
        }
        
        try {
            boolean wasRunning = isRunning;
            
            // Stop current detection if running
            if (wasRunning) {
                stop();
                // Wait a bit for the thread to stop
                Thread.sleep(100);
            }
            
            // Switch to new model
            currentModelPath = modelPath;
            detector = new MicroWakeWordDetector(context, modelPath);
            
            // Reset prediction history
            for (int i = 0; i < predictionHistory.length; i++) {
                predictionHistory[i] = 0.0f;
            }
            predictionIndex = 0;
            bufferOffset = 0;
            
            if (com.genuwin.app.live2d.WaifuDefine.DEBUG_LOG_ENABLE) {
                Log.d(TAG, "Switched wake word model to: " + modelPath);
            }
            
            // Restart detection if it was running
            if (wasRunning) {
                start();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to switch wake word model to: " + modelPath, e);
        }
    }
    
    /**
     * Update wake word model based on current character
     */
    public void updateForCurrentCharacter() {
        try {
            com.genuwin.app.managers.CharacterManager characterManager = 
                com.genuwin.app.managers.CharacterManager.getInstance(context);
            String modelPath = characterManager.getCurrentCharacterWakeWordModel();
            switchWakeWordModel(modelPath);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update wake word model for current character", e);
        }
    }

    public void start() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        detectionThread = new Thread(this::runDetection);
        detectionThread.start();
    }

    public void stop() {
        isRunning = false;
        if (detectionThread != null) {
            detectionThread.interrupt();
        }
    }
    
    /**
     * Pause wake word detection (for VAD recording)
     * This properly stops the continuous recording to avoid microphone conflicts
     */
    public void pause() {
        Log.d(TAG, "Pausing wake word detection for VAD recording");
        isPaused = true;
        isRunning = false;
        
        // Stop the continuous recording to free up microphone
        audioManager.stopContinuousRecording();
        
        if (detectionThread != null) {
            detectionThread.interrupt();
        }
    }
    
    /**
     * Resume wake word detection (after VAD recording completes)
     */
    public void resume() {
        if (!isPaused) {
            Log.d(TAG, "Wake word detection not paused, ignoring resume");
            return;
        }
        
        Log.d(TAG, "Resuming wake word detection after VAD recording");
        isPaused = false;
        start();
    }

    private void runDetection() {
        audioManager.startContinuousRecording(audioData -> {
            if (!isRunning) {
                return;
            }
            try {
                int remaining = audioBuffer.length - bufferOffset;
                if (audioData.length >= remaining) {
                    // Fill the buffer to complete it
                    System.arraycopy(audioData, 0, audioBuffer, bufferOffset, remaining);
                    
                    // Process the complete buffer
                    float[] floatArray = AudioPreprocessor.preprocess(audioBuffer);
                    float prediction = detector.getPrediction(floatArray);
                    predictionHistory[predictionIndex] = prediction;
                    predictionIndex = (predictionIndex + 1) % predictionHistory.length;

                    float averagePrediction = 0;
                    for (float p : predictionHistory) {
                        averagePrediction += p;
                    }
                    averagePrediction /= predictionHistory.length;
                    
                    // Store confidence for test mode
                    lastConfidence = averagePrediction;
                    
                    // Notify test mode listener of confidence updates
                    if (testMode && testListener != null) {
                        testListener.onConfidenceUpdate(averagePrediction);
                    }

                    // Get threshold from settings
                    float threshold = settingsManager.getWakeWordThreshold();
                    
                    if (averagePrediction > threshold) {
                        Log.d(TAG, "Wake word detected with confidence: " + averagePrediction);
                        
                        // Notify test mode listener
                        if (testMode && testListener != null) {
                            testListener.onWakeWordDetected(averagePrediction);
                        }
                        
                        // Notify main listener
                        if (listener != null) {
                            listener.onWakeWordDetected();
                        }
                    }
                    
                    // Handle leftover data - ensure it doesn't exceed buffer size
                    int leftover = audioData.length - remaining;
                    if (leftover > 0) {
                        // Ensure leftover doesn't exceed buffer capacity
                        int copySize = Math.min(leftover, audioBuffer.length);
                        System.arraycopy(audioData, remaining, audioBuffer, 0, copySize);
                        bufferOffset = copySize;
                    } else {
                        bufferOffset = 0;
                    }
                } else {
                    // Ensure we don't exceed buffer capacity
                    int copySize = Math.min(audioData.length, audioBuffer.length - bufferOffset);
                    System.arraycopy(audioData, 0, audioBuffer, bufferOffset, copySize);
                    bufferOffset += copySize;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during wake word detection", e);
            }
        });
    }
    
    /**
     * Set test mode for wake word detection
     */
    public void setTestMode(boolean enabled, TestModeListener listener) {
        this.testMode = enabled;
        this.testListener = listener;
        
        if (enabled && listener != null) {
            listener.onTestModeStarted();
            Log.d(TAG, "Wake word test mode started");
        } else if (!enabled && listener != null) {
            listener.onTestModeStopped();
            Log.d(TAG, "Wake word test mode stopped");
        }
    }
    
    /**
     * Get the current confidence value (for test mode)
     */
    public float getCurrentConfidence() {
        return lastConfidence;
    }
    
    /**
     * Update sensitivity from settings
     */
    public void updateSensitivityFromSettings() {
        // The threshold is now read directly from settings in runDetection()
        // This method can be called when settings change to trigger any needed updates
        Log.d(TAG, "Wake word sensitivity updated from settings");
    }
    
    /**
     * Validate a wake word model file
     */
    public boolean validateModel(String modelPath) {
        try {
            if (modelPath == null || modelPath.isEmpty()) {
                return false;
            }
            
            // Check if model file exists in assets
            context.getAssets().open(modelPath).close();
            
            // Try to create a detector with the model (basic validation)
            MicroWakeWordDetector testDetector = new MicroWakeWordDetector(context, modelPath);
            return testDetector != null;
            
        } catch (Exception e) {
            Log.e(TAG, "Model validation failed for: " + modelPath, e);
            return false;
        }
    }
    
    /**
     * Set character-specific sensitivity (if different from global)
     */
    public void setCharacterSpecificSensitivity(String characterId, float sensitivity) {
        // This would be used if we want to override the global threshold per character
        // For now, we use the global threshold from settings
        Log.d(TAG, "Character-specific sensitivity set for " + characterId + ": " + sensitivity);
    }
    
    /**
     * Get effective sensitivity for current character
     */
    public float getEffectiveSensitivity() {
        try {
            com.genuwin.app.managers.CharacterManager characterManager = 
                com.genuwin.app.managers.CharacterManager.getInstance(context);
            CharacterManager.CharacterInfo currentCharacter = characterManager.getCurrentCharacter();
            
            if (currentCharacter != null) {
                return settingsManager.getEffectiveWakeWordSensitivity(currentCharacter.modelDirectory);
            } else {
                return settingsManager.getWakeWordSensitivity();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get effective sensitivity", e);
            return settingsManager.getWakeWordSensitivity();
        }
    }
}

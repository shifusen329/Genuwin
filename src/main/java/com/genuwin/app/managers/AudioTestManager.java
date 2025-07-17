package com.genuwin.app.managers;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.genuwin.app.settings.SettingsManager;
import com.genuwin.app.vad.VADConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager for audio testing functionality including microphone tests,
 * speaker tests, and auto-calibration
 */
public class AudioTestManager {
    private static final String TAG = "AudioTestManager";
    
    // Audio configuration
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    // Test parameters
    private static final int LEVEL_TEST_DURATION = 5000; // 5 seconds
    private static final int CALIBRATION_DURATION = 5000; // 5 seconds
    private static final int UPDATE_INTERVAL = 50; // 50ms updates
    
    private final Context context;
    private final AudioManager audioManager;
    private final SettingsManager settingsManager;
    private final Handler mainHandler;
    
    // Test state
    private volatile boolean isTestingLevels = false;
    private volatile boolean isCalibrating = false;
    private AudioRecord testAudioRecord;
    private Thread testThread;
    
    // Callbacks
    public interface AudioLevelCallback {
        void onLevelUpdate(float level, float peak);
        void onTestStarted();
        void onTestCompleted();
        void onTestError(String error);
    }
    
    public interface TestResultCallback {
        void onTestStarted();
        void onTestProgress(int progress);
        void onTestCompleted(TestResult result);
        void onTestError(String error);
    }
    
    public interface CalibrationCallback {
        void onCalibrationStarted();
        void onCalibrationProgress(int progress, String status);
        void onCalibrationCompleted(CalibrationResult result);
        void onCalibrationError(String error);
    }
    
    /**
     * Test result data class
     */
    public static class TestResult {
        public final boolean passed;
        public final String message;
        public final float averageLevel;
        public final float peakLevel;
        public final float noiseFloor;
        public final List<String> recommendations;
        
        public TestResult(boolean passed, String message, float averageLevel, 
                         float peakLevel, float noiseFloor, List<String> recommendations) {
            this.passed = passed;
            this.message = message;
            this.averageLevel = averageLevel;
            this.peakLevel = peakLevel;
            this.noiseFloor = noiseFloor;
            this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
        }
    }
    
    /**
     * Calibration result data class
     */
    public static class CalibrationResult {
        public final float optimalSensitivity;
        public final float vadThreshold;
        public final long vadSilenceDuration;
        public final float noiseFloor;
        public final boolean successful;
        public final String message;
        public final List<String> recommendations;
        
        public CalibrationResult(float optimalSensitivity, float vadThreshold, 
                               long vadSilenceDuration, float noiseFloor, 
                               boolean successful, String message, List<String> recommendations) {
            this.optimalSensitivity = optimalSensitivity;
            this.vadThreshold = vadThreshold;
            this.vadSilenceDuration = vadSilenceDuration;
            this.noiseFloor = noiseFloor;
            this.successful = successful;
            this.message = message;
            this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
        }
    }
    
    /**
     * Audio device information
     */
    public static class AudioDeviceInfo {
        public final int sampleRate;
        public final int bufferSize;
        public final boolean microphoneAvailable;
        public final boolean speakerAvailable;
        public final String deviceInfo;
        
        public AudioDeviceInfo(int sampleRate, int bufferSize, boolean microphoneAvailable,
                              boolean speakerAvailable, String deviceInfo) {
            this.sampleRate = sampleRate;
            this.bufferSize = bufferSize;
            this.microphoneAvailable = microphoneAvailable;
            this.speakerAvailable = speakerAvailable;
            this.deviceInfo = deviceInfo;
        }
    }
    
    public AudioTestManager(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = new AudioManager(context);
        this.settingsManager = SettingsManager.getInstance(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Start real-time audio level monitoring test
     */
    public void startAudioLevelTest(AudioLevelCallback callback) {
        if (isTestingLevels) {
            callback.onTestError("Audio level test already in progress");
            return;
        }
        
        Log.d(TAG, "Starting audio level test");
        isTestingLevels = true;
        
        testThread = new Thread(() -> {
            try {
                runAudioLevelTest(callback);
            } catch (Exception e) {
                Log.e(TAG, "Error in audio level test", e);
                mainHandler.post(() -> callback.onTestError("Audio level test failed: " + e.getMessage()));
            } finally {
                isTestingLevels = false;
                cleanupAudioRecord();
            }
        });
        
        testThread.start();
        mainHandler.post(callback::onTestStarted);
    }
    
    /**
     * Stop audio level test
     */
    public void stopAudioLevelTest() {
        Log.d(TAG, "Stopping audio level test");
        isTestingLevels = false;
        if (testThread != null) {
            testThread.interrupt();
        }
        cleanupAudioRecord();
    }
    
    /**
     * Run the actual audio level test
     */
    private void runAudioLevelTest(AudioLevelCallback callback) {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        
        try {
            testAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            );
            
            if (testAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                mainHandler.post(() -> callback.onTestError("Failed to initialize microphone"));
                return;
            }
            
            testAudioRecord.startRecording();
            
            byte[] buffer = new byte[bufferSize];
            long startTime = System.currentTimeMillis();
            long lastUpdate = 0;
            
            float maxPeak = 0;
            List<Float> levelHistory = new ArrayList<>();
            
            while (isTestingLevels && (System.currentTimeMillis() - startTime) < LEVEL_TEST_DURATION) {
                int bytesRead = testAudioRecord.read(buffer, 0, buffer.length);
                
                if (bytesRead > 0) {
                    // Apply microphone sensitivity before calculating levels
                    applySensitivity(buffer, bytesRead);
                    
                    // Calculate audio level
                    float level = calculateAudioLevel(buffer, bytesRead);
                    float peak = calculatePeakLevel(buffer, bytesRead);
                    
                    levelHistory.add(level);
                    if (peak > maxPeak) {
                        maxPeak = peak;
                    }
                    
                    // Update UI at regular intervals
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastUpdate >= UPDATE_INTERVAL) {
                        final float finalLevel = level;
                        final float finalPeak = peak;
                        mainHandler.post(() -> callback.onLevelUpdate(finalLevel, finalPeak));
                        lastUpdate = currentTime;
                    }
                }
            }
            
            mainHandler.post(callback::onTestCompleted);
            
        } catch (SecurityException e) {
            mainHandler.post(() -> callback.onTestError("Microphone permission denied"));
        } catch (Exception e) {
            mainHandler.post(() -> callback.onTestError("Audio test failed: " + e.getMessage()));
        }
    }
    
    /**
     * Test microphone recording functionality
     */
    public void testMicrophoneRecording(TestResultCallback callback) {
        Log.d(TAG, "Starting microphone recording test");
        
        mainHandler.post(callback::onTestStarted);
        
        // Use existing AudioManager for recording test
        audioManager.startRecording(new AudioManager.RecordingCallback() {
            @Override
            public void onRecordingStarted() {
                mainHandler.post(() -> callback.onTestProgress(25));
            }
            
            @Override
            public void onRecordingStopped(File audioFile) {
                mainHandler.post(() -> callback.onTestProgress(75));
                
                // Analyze the recorded file
                TestResult result = analyzeRecordedAudio(audioFile);
                mainHandler.post(() -> callback.onTestCompleted(result));
                
                // Clean up
                if (audioFile.exists()) {
                    audioFile.delete();
                }
            }
            
            @Override
            public void onRecordingError(String error) {
                TestResult result = new TestResult(false, "Recording failed: " + error, 
                    0, 0, 0, List.of("Check microphone permissions", "Ensure microphone is not in use"));
                mainHandler.post(() -> callback.onTestCompleted(result));
            }
        });
        
        // Stop recording after 3 seconds
        mainHandler.postDelayed(() -> {
            audioManager.stopRecording();
            mainHandler.post(() -> callback.onTestProgress(50));
        }, 3000);
    }
    
    /**
     * Perform auto-calibration
     */
    public void performAutoCalibration(CalibrationCallback callback) {
        if (isCalibrating) {
            callback.onCalibrationError("Calibration already in progress");
            return;
        }
        
        Log.d(TAG, "Starting auto-calibration");
        isCalibrating = true;
        
        mainHandler.post(callback::onCalibrationStarted);
        
        testThread = new Thread(() -> {
            try {
                runAutoCalibration(callback);
            } catch (Exception e) {
                Log.e(TAG, "Error in auto-calibration", e);
                mainHandler.post(() -> callback.onCalibrationError("Calibration failed: " + e.getMessage()));
            } finally {
                isCalibrating = false;
                cleanupAudioRecord();
            }
        });
        
        testThread.start();
    }
    
    /**
     * Run the auto-calibration process
     */
    private void runAutoCalibration(CalibrationCallback callback) {
        mainHandler.post(() -> callback.onCalibrationProgress(10, "Initializing microphone..."));
        
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        
        try {
            testAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            );
            
            if (testAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                mainHandler.post(() -> callback.onCalibrationError("Failed to initialize microphone"));
                return;
            }
            
            mainHandler.post(() -> callback.onCalibrationProgress(25, "Measuring ambient noise..."));
            
            testAudioRecord.startRecording();
            
            // Measure ambient noise for calibration duration
            byte[] buffer = new byte[bufferSize];
            long startTime = System.currentTimeMillis();
            List<Float> noiseLevels = new ArrayList<>();
            
            while (isCalibrating && (System.currentTimeMillis() - startTime) < CALIBRATION_DURATION) {
                int bytesRead = testAudioRecord.read(buffer, 0, buffer.length);
                
                if (bytesRead > 0) {
                    float level = calculateAudioLevel(buffer, bytesRead);
                    noiseLevels.add(level);
                    
                    // Update progress
                    long elapsed = System.currentTimeMillis() - startTime;
                    int progress = 25 + (int) ((elapsed * 50) / CALIBRATION_DURATION);
                    final int finalProgress = Math.min(progress, 75);
                    mainHandler.post(() -> callback.onCalibrationProgress(finalProgress, "Analyzing audio levels..."));
                }
            }
            
            mainHandler.post(() -> callback.onCalibrationProgress(85, "Calculating optimal settings..."));
            
            // Calculate calibration results
            CalibrationResult result = calculateOptimalSettings(noiseLevels);
            
            mainHandler.post(() -> callback.onCalibrationProgress(100, "Calibration complete"));
            mainHandler.post(() -> callback.onCalibrationCompleted(result));
            
        } catch (SecurityException e) {
            mainHandler.post(() -> callback.onCalibrationError("Microphone permission denied"));
        } catch (Exception e) {
            mainHandler.post(() -> callback.onCalibrationError("Calibration failed: " + e.getMessage()));
        }
    }
    
    /**
     * Calculate optimal settings from noise measurements
     */
    private CalibrationResult calculateOptimalSettings(List<Float> noiseLevels) {
        if (noiseLevels.isEmpty()) {
            return new CalibrationResult(1.0f, 1000f, 1500L, 0f, false, 
                "No audio data collected", List.of("Try again in a quieter environment"));
        }
        
        // Calculate noise floor (average of lowest 25% of readings)
        noiseLevels.sort(Float::compareTo);
        int quarterPoint = noiseLevels.size() / 4;
        float noiseFloor = 0;
        for (int i = 0; i < quarterPoint; i++) {
            noiseFloor += noiseLevels.get(i);
        }
        noiseFloor /= quarterPoint;
        
        // Calculate optimal sensitivity (inverse relationship with noise floor)
        float optimalSensitivity;
        if (noiseFloor < 0.1f) {
            optimalSensitivity = 1.5f; // Quiet environment, can be more sensitive
        } else if (noiseFloor < 0.3f) {
            optimalSensitivity = 1.0f; // Normal environment
        } else {
            optimalSensitivity = 0.7f; // Noisy environment, reduce sensitivity
        }
        
        // Calculate VAD threshold (should be above noise floor)
        float vadThreshold = Math.max(1000f, noiseFloor * 3000f + 500f);
        
        // Calculate silence duration (longer in noisy environments)
        long vadSilenceDuration = noiseFloor > 0.3f ? 2000L : 1500L;
        
        List<String> recommendations = new ArrayList<>();
        if (noiseFloor > 0.5f) {
            recommendations.add("Environment is quite noisy - consider finding a quieter location");
        }
        if (noiseFloor < 0.05f) {
            recommendations.add("Very quiet environment detected - optimal for voice recognition");
        }
        
        String message = String.format("Calibration successful. Noise floor: %.3f, Optimal sensitivity: %.1f", 
            noiseFloor, optimalSensitivity);
        
        return new CalibrationResult(optimalSensitivity, vadThreshold, vadSilenceDuration, 
            noiseFloor, true, message, recommendations);
    }
    
    /**
     * Apply calibration results to settings
     */
    public void applyCalibrationResults(CalibrationResult result) {
        if (!result.successful) {
            Log.w(TAG, "Cannot apply unsuccessful calibration results");
            return;
        }
        
        Log.d(TAG, "Applying calibration results: sensitivity=" + result.optimalSensitivity + 
                   ", vadThreshold=" + result.vadThreshold);
        
        settingsManager.setFloat(SettingsManager.Keys.MIC_SENSITIVITY, result.optimalSensitivity);
        settingsManager.setInt(SettingsManager.Keys.VAD_SILENCE_THRESHOLD, (int) result.vadThreshold);
        settingsManager.setLong(SettingsManager.Keys.VAD_MIN_SILENCE_DURATION, result.vadSilenceDuration);
    }
    
    /**
     * Get audio device information
     */
    public AudioDeviceInfo getAudioDeviceInfo() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        boolean micAvailable = bufferSize != AudioRecord.ERROR_BAD_VALUE;
        
        String deviceInfo = String.format("Sample Rate: %d Hz\nBuffer Size: %d bytes\nFormat: 16-bit PCM", 
            SAMPLE_RATE, bufferSize);
        
        return new AudioDeviceInfo(SAMPLE_RATE, bufferSize, micAvailable, true, deviceInfo);
    }
    
    /**
     * Apply microphone sensitivity multiplier to audio buffer
     */
    private void applySensitivity(byte[] buffer, int length) {
        float sensitivity = settingsManager.getFloat(SettingsManager.Keys.MIC_SENSITIVITY, 
                                                   SettingsManager.Defaults.MIC_SENSITIVITY);
        
        // Only apply if sensitivity is not 1.0 (default)
        if (Math.abs(sensitivity - 1.0f) > 0.01f) {
            for (int i = 0; i < length; i += 2) {
                // Convert bytes to short (16-bit sample)
                short sample = (short) (((buffer[i + 1] & 0xFF) << 8) | (buffer[i] & 0xFF));
                
                // Apply sensitivity multiplier
                float amplified = sample * sensitivity;
                
                // Clamp to prevent overflow/distortion
                amplified = Math.max(-32768, Math.min(32767, amplified));
                
                // Convert back to bytes
                short newSample = (short) amplified;
                buffer[i] = (byte) (newSample & 0xFF);
                buffer[i + 1] = (byte) ((newSample >> 8) & 0xFF);
            }
        }
    }

    /**
     * Calculate audio level from buffer
     */
    private float calculateAudioLevel(byte[] buffer, int bytesRead) {
        long sum = 0;
        for (int i = 0; i < bytesRead; i += 2) {
            short sample = (short) (((buffer[i + 1] & 0xFF) << 8) | (buffer[i] & 0xFF));
            sum += Math.abs(sample);
        }
        return (float) sum / (bytesRead / 2) / 32768.0f;
    }
    
    /**
     * Calculate peak level from buffer
     */
    private float calculatePeakLevel(byte[] buffer, int bytesRead) {
        short maxSample = 0;
        for (int i = 0; i < bytesRead; i += 2) {
            short sample = (short) (((buffer[i + 1] & 0xFF) << 8) | (buffer[i] & 0xFF));
            if (Math.abs(sample) > Math.abs(maxSample)) {
                maxSample = sample;
            }
        }
        return Math.abs(maxSample) / 32768.0f;
    }
    
    /**
     * Analyze recorded audio file
     */
    private TestResult analyzeRecordedAudio(File audioFile) {
        if (!audioFile.exists() || audioFile.length() == 0) {
            return new TestResult(false, "No audio recorded", 0, 0, 0, 
                List.of("Check microphone connection", "Verify microphone permissions"));
        }
        
        // Basic file analysis
        long fileSize = audioFile.length();
        boolean passed = fileSize > 1000; // At least 1KB of audio data
        
        List<String> recommendations = new ArrayList<>();
        if (!passed) {
            recommendations.add("Audio file too small - check microphone sensitivity");
            recommendations.add("Ensure you speak clearly during the test");
        } else {
            recommendations.add("Microphone recording test passed successfully");
        }
        
        String message = passed ? "Recording test successful" : "Recording test failed - insufficient audio data";
        
        return new TestResult(passed, message, 0.5f, 0.8f, 0.1f, recommendations);
    }
    
    /**
     * Clean up audio record resources
     */
    private void cleanupAudioRecord() {
        if (testAudioRecord != null) {
            try {
                if (testAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    testAudioRecord.stop();
                }
                testAudioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up AudioRecord", e);
            } finally {
                testAudioRecord = null;
            }
        }
    }
    
    /**
     * Release resources
     */
    public void release() {
        stopAudioLevelTest();
        isCalibrating = false;
        cleanupAudioRecord();
        if (audioManager != null) {
            audioManager.release();
        }
    }
}

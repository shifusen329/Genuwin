package com.genuwin.app.managers;

import com.genuwin.app.live2d.WaifuLive2DManager;
import com.genuwin.app.live2d.WaifuPal;
import com.genuwin.app.live2d.WaifuDefine;

public class LipSyncManager {
    private static final float SMOOTHING_FACTOR = 0.15f; // Slightly faster response
    private static final float VOLUME_MULTIPLIER = 6.75f; // More exaggerated mouth movement (increased by 1.5x)
    private static final float VOLUME_THRESHOLD = 0.005f; // Minimum volume to trigger lip sync
    private static final float MAX_MOUTH_OPEN = 0.95f; // Maximum mouth opening
    private static final float MIN_MOUTH_OPEN = 0.1f; // Minimum mouth opening when speaking
    
    private static volatile float currentVolume = 0.0f;
    private static volatile float smoothedVolume = 0.0f;
    private static volatile boolean isSpeaking = false;

    private static WaifuLive2DManager live2DManager;

    public static void setLive2DManager(WaifuLive2DManager manager) {
        live2DManager = manager;
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("LipSyncManager: Live2DManager set");
        }
    }

    public static void processAudioData(byte[] audioData, int readSize) {
        if (readSize <= 0) {
            currentVolume = 0;
            return;
        }

        double sum = 0;
        for (int i = 0; i < readSize; i += 2) {
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            sum += sample * sample;
        }
        double rms = Math.sqrt(sum / (readSize / 2.0));
        currentVolume = (float) (rms / 32767.0); // Normalize to 0-1
        
        // Disabled: Too verbose during TTS playback
        // if (WaifuDefine.DEBUG_LOG_ENABLE && currentVolume > 0.01f) {
        //     WaifuPal.printLog("LipSyncManager: Audio volume = " + currentVolume);
        // }
    }

    public static void update() {
        if (live2DManager == null) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("LipSyncManager: live2DManager is null");
            }
            return;
        }
        
        // Check if model is available before trying to update
        if (live2DManager.getWaifuModel() == null) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("LipSyncManager: WaifuModel is null, skipping update");
            }
            return;
        }
        
        // Smooth the volume changes with faster response
        smoothedVolume += (currentVolume - smoothedVolume) * SMOOTHING_FACTOR;
        
        // Determine if currently speaking based on volume threshold
        boolean wasSpeaking = isSpeaking;
        isSpeaking = smoothedVolume > VOLUME_THRESHOLD;
        
        float mouthOpen = 0.0f;
        
        if (isSpeaking) {
            // Enhanced mouth movement calculation
            // Apply volume multiplier for more exaggerated movement
            mouthOpen = smoothedVolume * VOLUME_MULTIPLIER;
            
            // Add a base opening when speaking to avoid mumbling look
            mouthOpen = Math.max(mouthOpen, MIN_MOUTH_OPEN);
            
            // Clamp to maximum opening
            mouthOpen = Math.min(mouthOpen, MAX_MOUTH_OPEN);
            
            // Add slight variation to make it look more natural
            float variation = (float) (Math.sin(System.currentTimeMillis() * 0.01) * 0.05f);
            mouthOpen += variation;
            
            // Final clamp after variation
            mouthOpen = Math.max(0.0f, Math.min(mouthOpen, MAX_MOUTH_OPEN));
        } else {
            // Gradually close mouth when not speaking
            mouthOpen = 0.0f;
        }

        // Disabled: Too verbose during TTS playback
        // if (WaifuDefine.DEBUG_LOG_ENABLE && (mouthOpen > 0.01f || wasSpeaking != isSpeaking)) {
        //     WaifuPal.printLog("LipSyncManager: Speaking=" + isSpeaking + ", Volume=" + smoothedVolume + ", MouthOpen=" + mouthOpen);
        // }

        live2DManager.getWaifuModel().setLipSyncValue(mouthOpen);
    }

    public static void reset() {
        currentVolume = 0;
        smoothedVolume = 0;
        isSpeaking = false;
        if (live2DManager != null && live2DManager.getWaifuModel() != null) {
            live2DManager.getWaifuModel().setLipSyncValue(0);
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("LipSyncManager: Reset lip sync");
            }
        }
    }

    public static float getCurrentLipSyncValue() {
        // Smooth the volume changes with faster response
        smoothedVolume += (currentVolume - smoothedVolume) * SMOOTHING_FACTOR;
        
        // Determine if currently speaking based on volume threshold
        boolean currentlySpeaking = smoothedVolume > VOLUME_THRESHOLD;
        
        float mouthOpen = 0.0f;
        
        if (currentlySpeaking) {
            // Enhanced mouth movement calculation
            // Apply volume multiplier for more exaggerated movement
            mouthOpen = smoothedVolume * VOLUME_MULTIPLIER;
            
            // Add a base opening when speaking to avoid mumbling look
            mouthOpen = Math.max(mouthOpen, MIN_MOUTH_OPEN);
            
            // Clamp to maximum opening
            mouthOpen = Math.min(mouthOpen, MAX_MOUTH_OPEN);
            
            // Add slight variation to make it look more natural
            float variation = (float) (Math.sin(System.currentTimeMillis() * 0.01) * 0.05f);
            mouthOpen += variation;
            
            // Final clamp after variation
            mouthOpen = Math.max(0.0f, Math.min(mouthOpen, MAX_MOUTH_OPEN));
        } else {
            // Gradually close mouth when not speaking
            mouthOpen = 0.0f;
        }

        // Disabled: Too verbose during TTS playback
        // if (WaifuDefine.DEBUG_LOG_ENABLE && mouthOpen > 0.01f) {
        //     WaifuPal.printLog("LipSyncManager: getCurrentLipSyncValue = " + mouthOpen);
        // }

        return mouthOpen;
    }
}

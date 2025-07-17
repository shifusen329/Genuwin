package com.genuwin.app.vad;

public class VoiceActivityDetector {
    private VADConfig config;
    private long silenceStartTime = 0;
    private boolean hasDetectedSpeech = false;
    private double totalAudioEnergy = 0.0;
    private int audioFrameCount = 0;

    public VoiceActivityDetector() {
        // Default configuration - increased threshold to reduce false positives
        this.config = new VADConfig(300, 2000);
    }

    public VoiceActivityDetector(VADConfig config) {
        this.config = config;
    }

    public void setConfig(VADConfig config) {
        this.config = config;
    }

    public synchronized boolean isSpeechEnded(short[] audioData) {
        double rms = calculateRMS(audioData);
        
        // Track total audio energy to detect if there's meaningful audio
        totalAudioEnergy += rms;
        audioFrameCount++;

        if (rms < config.getSilenceThreshold()) {
            if (silenceStartTime == 0) {
                silenceStartTime = System.currentTimeMillis();
                android.util.Log.d("VAD", "Silence started, threshold: " + config.getSilenceThreshold() + ", RMS: " + rms);
            } else {
                long silenceDuration = System.currentTimeMillis() - silenceStartTime;
                if (silenceDuration > config.getMinSilenceDuration()) {
                    // Always end recording after silence duration, regardless of speech detection
                    android.util.Log.d("VAD", "Speech ended after " + silenceDuration + "ms silence, hasDetectedSpeech: " + hasDetectedSpeech);
                    return true;
                }
            }
        } else {
            if (silenceStartTime != 0) {
                android.util.Log.d("VAD", "Speech resumed, RMS: " + rms + " > threshold: " + config.getSilenceThreshold());
            }
            silenceStartTime = 0; // Reset on speech
            hasDetectedSpeech = true; // Mark that we've detected actual speech
        }

        return false;
    }

    public synchronized boolean hasDetectedMeaningfulAudio() {
        if (audioFrameCount == 0) return false;
        
        double averageEnergy = totalAudioEnergy / audioFrameCount;
        // Require both: actual speech detection AND sufficient average energy
        // Use a lower threshold (50% of silence threshold) to be less strict
        boolean hasEnoughEnergy = averageEnergy > (config.getSilenceThreshold() * 0.5);
        
        android.util.Log.d("VAD", "Meaningful audio check - hasDetectedSpeech: " + hasDetectedSpeech + 
                           ", averageEnergy: " + averageEnergy + 
                           ", threshold: " + (config.getSilenceThreshold() * 0.5) + 
                           ", hasEnoughEnergy: " + hasEnoughEnergy);
        
        return hasDetectedSpeech && hasEnoughEnergy;
    }

    public synchronized void reset() {
        silenceStartTime = 0;
        hasDetectedSpeech = false;
        totalAudioEnergy = 0.0;
        audioFrameCount = 0;
    }

    private double calculateRMS(short[] audioData) {
        double sum = 0.0;
        for (short data : audioData) {
            sum += data * data;
        }
        double rms = Math.sqrt(sum / audioData.length);
        return rms;
    }
}

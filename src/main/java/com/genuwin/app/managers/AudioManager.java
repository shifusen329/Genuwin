package com.genuwin.app.managers;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.genuwin.app.settings.SettingsManager;
import com.genuwin.app.vad.VADConfig;
import com.genuwin.app.vad.VoiceActivityDetector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Manager class for handling audio recording and playback
 */
public class AudioManager {
    private static final String TAG = "AudioManager";
    
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    private Context context;
    private SettingsManager settingsManager;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private volatile boolean isRecording = false;
    private volatile boolean isPlaying = false;
    private volatile boolean playbackInterrupted = false;
    private File audioFile;
    private RecordingCallback recordingCallback;
    private AudioDataCallback audioDataCallback;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private Thread playbackThread;
    private VoiceActivityDetector vad;
    
    // Timeout management
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable currentTimeoutRunnable = null;

    public interface AudioDataCallback {
        void onAudioData(byte[] audioData);
    }
    
    public interface RecordingCallback {
        void onRecordingStarted();
        void onRecordingStopped(File audioFile);
        void onRecordingError(String error);
    }

    public interface PlaybackCallback {
        void onPlaybackStarted();
        void onPlaybackCompleted();
        void onPlaybackInterrupted();
        void onPlaybackError(String error);
    }
    
    public AudioManager(Context context) {
        this.context = context.getApplicationContext();
        this.settingsManager = SettingsManager.getInstance(context);
    }

    public void startContinuousRecording(AudioDataCallback callback) {
        this.audioDataCallback = callback;
        if (isRecording) {
            return;
        }
        isRecording = true;
        recordingThread = new Thread(this::recordContinuously);
        recordingThread.start();
    }
    
    public void stopContinuousRecording() {
        Log.d(TAG, "Stopping continuous recording to free microphone");
        isRecording = false;
        
        if (recordingThread != null) {
            recordingThread.interrupt();
        }
        
        synchronized (this) {
            if (audioRecord != null) {
                try {
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                    Log.d(TAG, "AudioRecord stopped and released in stopContinuousRecording");
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping continuous recording: " + e.getMessage());
                }
            }
        }
    }

    private void recordContinuously() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        AudioRecord localAudioRecord = null;
        
        try {
            localAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);
            
            synchronized (this) {
                if (!isRecording) {
                    localAudioRecord.release();
                    return;
                }
                audioRecord = localAudioRecord;
            }
            
            audioRecord.startRecording();
            byte[] buffer = new byte[bufferSize];
            
            while (isRecording) {
                AudioRecord currentRecord;
                synchronized (this) {
                    currentRecord = audioRecord;
                    if (currentRecord == null || !isRecording) {
                        break;
                    }
                }
                
                try {
                    int read = currentRecord.read(buffer, 0, buffer.length);
                    if (read > 0 && audioDataCallback != null) {
                        // Apply microphone sensitivity before passing to callback
                        applySensitivity(buffer, read);
                        audioDataCallback.onAudioData(buffer);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error reading audio data: " + e.getMessage());
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in recordContinuously: " + e.getMessage());
        } finally {
            // Clean up in synchronized block
            synchronized (this) {
                if (audioRecord != null) {
                    try {
                        audioRecord.stop();
                        audioRecord.release();
                        audioRecord = null;
                        Log.d(TAG, "AudioRecord cleaned up successfully in recordContinuously");
                    } catch (Exception e) {
                        Log.e(TAG, "Error stopping continuous recording in recordContinuously: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    public void startRecordingWithVAD(VADConfig config, RecordingCallback callback) {
        this.recordingCallback = callback;
        
        // Create fresh VAD instance and ensure it's properly configured
        this.vad = new VoiceActivityDetector(config);
        this.vad.reset(); // Ensure clean state
        Log.d(TAG, "Starting VAD recording with threshold: " + config.getSilenceThreshold() + 
                   ", silence duration: " + config.getMinSilenceDuration() + "ms");
        
        if (isRecording) {
            callback.onRecordingError("Already recording");
            return;
        }
        isRecording = true;
        recordingThread = new Thread(() -> recordWithVAD(config));
        recordingThread.start();
    }

    private void recordWithVAD(VADConfig config) {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);
        audioRecord.startRecording();
        byte[] buffer = new byte[bufferSize];
        File audioFile = new File(context.getCacheDir(), "recorded_audio_" + System.currentTimeMillis() + ".wav");
        
        boolean speechEnded = false;
        long startTime = System.currentTimeMillis();
        final long MAX_RECORDING_TIME = 10000; // 10 seconds maximum recording time
        
        try (FileOutputStream fos = new FileOutputStream(audioFile)) {
            while (isRecording && !speechEnded) {
                // Check for timeout to prevent infinite recording
                if (System.currentTimeMillis() - startTime > MAX_RECORDING_TIME) {
                    Log.d(TAG, "Recording timeout reached after " + MAX_RECORDING_TIME + "ms");
                    break;
                }
                
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0) {
                    // Apply microphone sensitivity before processing
                    applySensitivity(buffer, read);
                    fos.write(buffer, 0, read);
                    short[] shortBuffer = toShortArray(buffer);
                    speechEnded = vad.isSpeechEnded(shortBuffer);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to audio file", e);
            if (recordingCallback != null) {
                recordingCallback.onRecordingError("Failed to write to audio file: " + e.getMessage());
            }
            return;
        }
        
        // Clean stop - no race condition
        isRecording = false;
        audioRecord.stop();
        audioRecord.release();
        
        // Check if we detected meaningful audio before processing
        if (!vad.hasDetectedMeaningfulAudio()) {
            Log.d(TAG, "No meaningful audio detected, skipping STT processing");
            audioFile.delete(); // Clean up the empty audio file
            if (recordingCallback != null) {
                recordingCallback.onRecordingError("No meaningful audio detected");
            }
            return;
        }
        
        try {
            addWavHeader(audioFile);
        } catch (IOException e) {
            Log.e(TAG, "Failed to add WAV header", e);
            if (recordingCallback != null) {
                recordingCallback.onRecordingError("Failed to add WAV header: " + e.getMessage());
            }
            return;
        }
        
        Log.d(TAG, "Meaningful audio detected, processing with STT");
        if (recordingCallback != null) {
            recordingCallback.onRecordingStopped(audioFile);
        }
    }

    private void addWavHeader(File file) throws IOException {
        long fileSize = file.length();
        long totalDataLen = fileSize + 36;
        long longSampleRate = SAMPLE_RATE;
        int channels = 1;
        long byteRate = 16 * SAMPLE_RATE * channels / 8;

        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = 16;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (fileSize & 0xff);
        header[41] = (byte) ((fileSize >> 8) & 0xff);
        header[42] = (byte) ((fileSize >> 16) & 0xff);
        header[43] = (byte) ((fileSize >> 24) & 0xff);

        File tempFile = new File(context.getCacheDir(), "temp.wav");
        try (FileOutputStream fos = new FileOutputStream(tempFile);
             FileInputStream fis = new FileInputStream(file)) {
            fos.write(header);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        }
        file.delete();
        tempFile.renameTo(file);
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

    private short[] toShortArray(byte[] byteArray) {
        short[] shortArray = new short[byteArray.length / 2];
        for (int i = 0; i < shortArray.length; i++) {
            shortArray[i] = (short) (((byteArray[i * 2 + 1] & 0xFF) << 8) | (byteArray[i * 2] & 0xFF));
        }
        return shortArray;
    }

    public void startRecording(RecordingCallback callback) {
        if (isRecording) {
            callback.onRecordingError("Already recording");
            return;
        }
        
        this.recordingCallback = callback;
        
        try {
            audioFile = new File(context.getCacheDir(), "recorded_audio_" + System.currentTimeMillis() + ".wav");
            
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            
            mediaRecorder.setOnErrorListener((mr, what, extra) -> {
                Log.e(TAG, "MediaRecorder error: " + what + ", " + extra);
                if (recordingCallback != null) {
                    recordingCallback.onRecordingError("Recording error: " + what);
                }
                stopRecording();
            });
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            
            if (recordingCallback != null) {
                recordingCallback.onRecordingStarted();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording", e);
            if (recordingCallback != null) {
                recordingCallback.onRecordingError("Failed to start recording: " + e.getMessage());
            }
        }
    }
    
    public void stopRecording() {
        if (isRecording) {
            isRecording = false;
            if (recordingThread != null) {
                recordingThread.interrupt();
            }
        }
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                if (recordingCallback != null) {
                    recordingCallback.onRecordingStopped(audioFile);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to stop recording", e);
                if (recordingCallback != null) {
                    recordingCallback.onRecordingError("Failed to stop recording: " + e.getMessage());
                }
            }
        }
    }
    
    public void playAudio(File audioFile, PlaybackCallback callback) {
        if (isPlaying) {
            callback.onPlaybackError("Already playing audio");
            return;
        }
        isPlaying = true;
        playbackInterrupted = false;
        playbackThread = new Thread(() -> {
            try {
                playAudioWithAudioTrack(audioFile, callback);
            } catch (IOException e) {
                Log.e(TAG, "Failed to play audio", e);
                callback.onPlaybackError("Failed to play audio: " + e.getMessage());
            } finally {
                isPlaying = false;
                playbackThread = null;
            }
        });
        playbackThread.start();
    }

    private void playAudioWithAudioTrack(File audioFile, PlaybackCallback callback) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(audioFile.getAbsolutePath());

        MediaFormat format = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat trackFormat = extractor.getTrackFormat(i);
            String mime = trackFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                format = trackFormat;
                extractor.selectTrack(i);
                break;
            }
        }

        if (format == null) {
            callback.onPlaybackError("No audio track found");
            return;
        }

        String mime = format.getString(MediaFormat.KEY_MIME);
        MediaCodec codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null, null, 0);
        codec.start();

        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        int channelConfig = (channelCount == 1) ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;

        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AUDIO_FORMAT);
        AudioTrack audioTrack = new AudioTrack(
                android.media.AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfig,
                AUDIO_FORMAT,
                bufferSize,
                AudioTrack.MODE_STREAM
        );

        audioTrack.play();
        callback.onPlaybackStarted();

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        byte[] buffer = new byte[bufferSize];
        boolean isEOS = false;

        while (!isEOS && !playbackInterrupted) {
            int inputBufferIndex = codec.dequeueInputBuffer(10000);
            if (inputBufferIndex >= 0) {
                int sampleSize = extractor.readSampleData(codec.getInputBuffer(inputBufferIndex), 0);
                if (sampleSize < 0) {
                    codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    isEOS = true;
                } else {
                    codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                    extractor.advance();
                }
            }

            int outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000);
            if (outputBufferIndex >= 0) {
                byte[] chunk = new byte[bufferInfo.size];
                codec.getOutputBuffer(outputBufferIndex).get(chunk);
                codec.releaseOutputBuffer(outputBufferIndex, false);

                // Check for interruption before writing audio
                if (playbackInterrupted) {
                    break;
                }

                audioTrack.write(chunk, 0, chunk.length);
                LipSyncManager.processAudioData(chunk, chunk.length);
            }
        }

        audioTrack.stop();
        audioTrack.release();
        codec.stop();
        codec.release();
        extractor.release();
        LipSyncManager.reset();
        
        if (playbackInterrupted) {
            Log.d(TAG, "Audio playback was interrupted");
            callback.onPlaybackInterrupted();
        } else {
            callback.onPlaybackCompleted();
        }
    }
    
    /**
     * Interrupt current audio playback
     */
    public void interruptPlayback() {
        if (isPlaying) {
            Log.d(TAG, "Interrupting audio playback");
            playbackInterrupted = true;
            
            // Interrupt the playback thread if it exists
            if (playbackThread != null) {
                playbackThread.interrupt();
            }
        }
    }
    
    public void stopPlayback() {
        if (mediaPlayer != null && isPlaying) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                isPlaying = false;
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to stop playback", e);
            }
        }
    }
    
    public boolean isRecording() {
        return isRecording;
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public void release() {
        stopRecording();
        stopPlayback();
        cancelCurrentTimeout();
    }
    
    // ========== TIMEOUT MANAGEMENT ==========
    
    /**
     * Cancel the current timeout runnable to prevent race conditions
     */
    public void cancelCurrentTimeout() {
        if (currentTimeoutRunnable != null) {
            handler.removeCallbacks(currentTimeoutRunnable);
            currentTimeoutRunnable = null;
        }
    }
    
    /**
     * Start recording with VAD and timeout fallback for initial listening
     */
    public void startInitialListening(VADConfig config, RecordingCallback callback, long timeoutMs) {
        Log.d(TAG, "Starting initial listening with VAD(" + config.getSilenceThreshold() + ", " + config.getMinSilenceDuration() + ") and timeout: " + timeoutMs + "ms");
        
        startRecordingWithVAD(config, new RecordingCallback() {
            @Override
            public void onRecordingStarted() {
                callback.onRecordingStarted();
                
                // Set up fallback timeout
                currentTimeoutRunnable = () -> {
                    if (isRecording) {
                        Log.d(TAG, "Initial listening timeout reached, stopping recording");
                        stopRecording();
                    }
                };
                handler.postDelayed(currentTimeoutRunnable, timeoutMs);
            }

            @Override
            public void onRecordingStopped(File audioFile) {
                cancelCurrentTimeout();
                callback.onRecordingStopped(audioFile);
            }

            @Override
            public void onRecordingError(String error) {
                cancelCurrentTimeout();
                callback.onRecordingError(error);
            }
        });
    }
    
    /**
     * Start follow-up listening with shorter timeout for better responsiveness
     */
    public void startFollowUpListening(VADConfig config, RecordingCallback callback, long timeoutMs) {
        Log.d(TAG, "Starting follow-up listening with VAD(" + config.getSilenceThreshold() + ", " + config.getMinSilenceDuration() + ") and timeout: " + timeoutMs + "ms");
        
        startRecordingWithVAD(config, new RecordingCallback() {
            @Override
            public void onRecordingStarted() {
                callback.onRecordingStarted();
                
                // Set up shorter fallback timeout for follow-up
                currentTimeoutRunnable = () -> {
                    if (isRecording) {
                        Log.d(TAG, "Follow-up listening timeout reached, stopping recording");
                        stopRecording();
                    }
                };
                handler.postDelayed(currentTimeoutRunnable, timeoutMs);
            }

            @Override
            public void onRecordingStopped(File audioFile) {
                cancelCurrentTimeout();
                callback.onRecordingStopped(audioFile);
            }

            @Override
            public void onRecordingError(String error) {
                cancelCurrentTimeout();
                callback.onRecordingError(error);
            }
        });
    }
}

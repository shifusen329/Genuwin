package com.genuwin.app.wakeword;

import org.tensorflow.lite.support.audio.TensorAudio;
import org.tensorflow.lite.support.audio.TensorAudio.TensorAudioFormat;

public class AudioPreprocessor {
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = android.media.AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = 1536 * 2; // 3072 bytes

    public static float[] preprocess(byte[] audioData) {
        short[] shortArray = toShortArray(audioData);
        float[] floatArray = new float[1536];
        for (int i = 0; i < 1536; i++) {
            if (i < shortArray.length) {
                floatArray[i] = shortArray[i] / 32768.0f;
            } else {
                floatArray[i] = 0.0f;
            }
        }
        return floatArray;
    }

    private static short[] toShortArray(byte[] byteArray) {
        short[] shortArray = new short[byteArray.length / 2];
        for (int i = 0; i < shortArray.length; i++) {
            shortArray[i] = (short) (((byteArray[i * 2 + 1] & 0xFF) << 8) | (byteArray[i * 2] & 0xFF));
        }
        return shortArray;
    }
}

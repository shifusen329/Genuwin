package com.genuwin.app.wakeword;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MicroWakeWordDetector {
    private static final String TAG = "MicroWakeWordDetector";
    private Interpreter interpreter;

    public MicroWakeWordDetector(Context context, String modelPath) {
        try {
            interpreter = new Interpreter(loadModelFile(context, modelPath));
            int[] inputShape = interpreter.getInputTensor(0).shape();
            Log.d(TAG, "Model input shape: " + java.util.Arrays.toString(inputShape));
        } catch (IOException e) {
            Log.e(TAG, "Error loading model", e);
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public float getPrediction(float[] audioData) {
        float[][] output = new float[1][1];
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(audioData.length * 4);
        inputBuffer.order(ByteOrder.nativeOrder());
        for (float val : audioData) {
            inputBuffer.putFloat(val);
        }
        inputBuffer.rewind();
        interpreter.run(inputBuffer, output);
        return output[0][0];
    }
}

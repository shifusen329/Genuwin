package com.genuwin.app.live2d;

import android.content.res.AssetManager;
import android.util.Log;

import com.genuwin.app.live2d.WaifuDefine;

import java.io.IOException;
import java.io.InputStream;

public class WaifuPal {
    private static final String TAG = "WaifuPal";
    private static long s_currentFrame;
    private static long s_lastFrame;
    private static float s_deltaTime;

    public static byte[] loadFileAsBytes(String path) {
        InputStream inputStream = null;
        try {
            AssetManager assets = WaifuAppDelegate.getInstance().getActivity().getAssets();
            inputStream = assets.open(path);

            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);

            return buffer;
        } catch (IOException e) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                Log.e(TAG, "File open error", e);
            }
            return null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                // Do nothing
            }
        }
    }

    public static void updateTime() {
        s_currentFrame = System.nanoTime();
        s_deltaTime = (s_currentFrame - s_lastFrame) / 1000000000.0f;
        s_lastFrame = s_currentFrame;
    }

    public static float getDeltaTime() {
        return s_deltaTime;
    }

    public static void printLog(String message) {
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            Log.d(TAG, message);
        }
    }

    public static class PrintLogFunction implements com.live2d.sdk.cubism.core.ICubismLogger {
        @Override
        public void print(String message) {
            WaifuPal.printLog(message);
        }
    }
}

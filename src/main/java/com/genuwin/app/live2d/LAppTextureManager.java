package com.genuwin.app.live2d;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class LAppTextureManager {
    private static final String TAG = "LAppTextureManager";
    private final List<TextureInfo> textures = new ArrayList<>();

    public static class TextureInfo {
        public int id;
        public int width;
        public int height;
        public String filePath;
    }

    public TextureInfo createTextureFromPngFile(String filePath) {
        for (TextureInfo textureInfo : textures) {
            if (textureInfo.filePath.equals(filePath)) {
                if (WaifuDefine.DEBUG_LOG_ENABLE) {
                    WaifuPal.printLog("LAppTextureManager: Returning cached texture for: " + filePath + " (ID: " + textureInfo.id + ")");
                }
                return textureInfo;
            }
        }

        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("LAppTextureManager: Creating new texture for: " + filePath);
        }

        TextureInfo newTextureInfo = new TextureInfo();
        newTextureInfo.filePath = filePath;

        Context context = WaifuAppDelegate.getInstance().getActivity();
        try (InputStream inputStream = context.getAssets().open(filePath)) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from " + filePath);
                return null;
            }

            int[] textureId = new int[1];
            GLES20.glGenTextures(1, textureId, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

            newTextureInfo.id = textureId[0];
            newTextureInfo.width = bitmap.getWidth();
            newTextureInfo.height = bitmap.getHeight();

            bitmap.recycle();
            textures.add(newTextureInfo);

            return newTextureInfo;
        } catch (IOException e) {
            Log.e(TAG, "Failed to load texture: " + filePath, e);
            return null;
        }
    }

    public void releaseTextures() {
        // CRITICAL FIX: Remove threading violation
        // When EGL context is lost, GPU resources are automatically invalidated
        // We only need to clear the Java-side cache, and this must be done on GL thread
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("LAppTextureManager: Clearing texture cache (count: " + textures.size() + ")");
        }
        textures.clear();
    }
}

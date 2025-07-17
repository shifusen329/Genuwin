package com.genuwin.app.live2d;

import android.app.Activity;
import android.opengl.GLES20;
import android.util.Log;
import android.view.MotionEvent;

import com.genuwin.app.managers.LipSyncManager;
import com.live2d.sdk.cubism.framework.CubismFramework;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glClearDepthf;
import java.util.concurrent.LinkedBlockingQueue;

public class WaifuAppDelegate {
    private static WaifuAppDelegate s_instance;
    private Activity activity;
    private WaifuView view;
    private LAppTextureManager textureManager;
    private int windowWidth;
    private int windowHeight;
    private boolean isActive = true;
    private int currentModel = 0;
    private boolean isCaptured;
    private float mouseX;
    private float mouseY;
    
    // Context loss state management
    private boolean contextWasLost = false;
    
    // Pending operations flags
    private volatile boolean pendingViewSettingsUpdate = false;
    private final LinkedBlockingQueue<Runnable> glTaskQueue = new LinkedBlockingQueue<>();

    private final CubismFramework.Option cubismOption = new CubismFramework.Option();

    public static WaifuAppDelegate getInstance() {
        if (s_instance == null) {
            s_instance = new WaifuAppDelegate();
        }
        return s_instance;
    }

    public static void releaseInstance() {
        if (s_instance != null) {
            s_instance = null;
        }
    }

    public void onStart(Activity activity) {
        this.activity = activity;
        view = new WaifuView();
        textureManager = new LAppTextureManager();
        WaifuPal.updateTime();
    }

    public void onPause() {
        currentModel = WaifuLive2DManager.getInstance().getCurrentModel();
    }

    public void onStop() {
        if (view != null) {
            view.close();
        }
        textureManager = null;

        WaifuLive2DManager.releaseInstance();
        CubismFramework.dispose();
    }

    public void onDestroy() {
        releaseInstance();
    }
    
    public void onSurfaceCreated() {
        // テクスチャサンプリング設定
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        // 透過設定
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        // Initialize Cubism SDK framework
        CubismFramework.initialize();
    }

    public void onSurfaceChanged(int width, int height) {
        // 描画範囲指定
        GLES20.glViewport(0, 0, width, height);
        windowWidth = width;
        windowHeight = height;

        // AppViewの初期化
        view.initialize();
        view.initializeSprite();

        // load models
        if (WaifuLive2DManager.getInstance().getCurrentModel() != currentModel) {
            WaifuLive2DManager.getInstance().changeScene(currentModel);
        }

        isActive = true;
    }

    private volatile boolean isRendering = false;

    public void run() {
        if (!isActive) {
            return;
        }
        isRendering = true;

        // Process queued GL tasks first
        Runnable task;
        while ((task = glTaskQueue.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                Log.e("WaifuAppDelegate", "Error executing GL task: " + e.getMessage());
            }
        }

        // Process pending operations on GL thread
        if (pendingViewSettingsUpdate) {
            processPendingViewSettingsUpdate();
            pendingViewSettingsUpdate = false;
        }

        WaifuPal.updateTime();
        glClearDepthf(1.0f);
        if (view != null) {
            view.render();
        }
        LipSyncManager.update();

        isRendering = false;

        // CRITICAL FIX: Removed activity.finishAndRemoveTask() call
        // This was incorrectly terminating the app during normal context loss events.
        // Context loss is temporary and should be recovered from, not cause app termination.
        // Let Android's lifecycle management handle app termination properly.
    }

    public void onTouchBegan(float x, float y) {
        mouseX = x;
        mouseY = y;
        if (view != null) {
            isCaptured = true;
            view.onTouchesBegan(mouseX, mouseY);
        }
    }

    public void onTouchEnd(float x, float y) {
        mouseX = x;
        mouseY = y;
        if (view != null) {
            isCaptured = false;
            view.onTouchesEnded(mouseX, mouseY);
        }
    }

    public void onTouchMoved(float x, float y) {
        mouseX = x;
        mouseY = y;
        if (isCaptured && view != null) {
            view.onTouchesMoved(mouseX, mouseY);
        }
    }

    public void onTouch(MotionEvent event) {
        final float pointX = event.getX();
        final float pointY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onTouchBegan(pointX, pointY);
                break;
            case MotionEvent.ACTION_UP:
                onTouchEnd(pointX, pointY);
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMoved(pointX, pointY);
                break;
        }
    }

    public WaifuLive2DManager getLive2DManager() {
        return WaifuLive2DManager.getInstance();
    }

    public WaifuView getView() {
        return view;
    }

    public Activity getActivity() {
        return activity;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public LAppTextureManager getTextureManager() {
        if (textureManager == null) {
            textureManager = new LAppTextureManager();
        }
        return textureManager;
    }
    
    /**
     * Update view settings when user changes preferences
     * Call this method when view settings are changed in the settings UI
     * This method is thread-safe and can be called from any thread
     */
    public void updateViewSettings() {
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("WaifuAppDelegate: Queuing view settings update for GL thread");
        }
        pendingViewSettingsUpdate = true;
    }
    
    /**
     * Process pending view settings update on GL thread
     * This method is called from the render loop and executes GL operations safely
     */
    private void processPendingViewSettingsUpdate() {
        if (view != null && activity != null) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("WaifuAppDelegate: Processing view update on GL thread");
            }
            view.updateViewSettings(activity);
            // Force reload current model after view update to ensure proper rendering
            WaifuLive2DManager.getInstance().changeScene(currentModel, true);
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("WaifuAppDelegate: View settings updated and model reloaded on GL thread");
            }
        } else {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("WaifuAppDelegate: Cannot update view settings - view or activity is null");
            }
        }
    }
    
    /**
     * Update performance mode settings
     * Call this method when performance mode is changed in the settings UI
     * Queues the GL state changes for execution on GL thread
     */
    public void updatePerformanceMode(String performanceMode) {
        // Queue GL state changes
        glTaskQueue.offer(() -> {
            switch (performanceMode) {
                case "high_quality":
                    // Enable all quality features
                    GLES20.glEnable(GLES20.GL_BLEND);
                    GLES20.glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                    break;
                case "balanced":
                    // Standard quality (current default)
                    GLES20.glEnable(GLES20.GL_BLEND);
                    GLES20.glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                    break;
                case "performance":
                    // Reduced quality for better performance
                    GLES20.glDisable(GLES20.GL_BLEND);
                    break;
            }
            
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("Performance mode updated on GL thread: " + performanceMode);
            }
        });
        
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            WaifuPal.printLog("Performance mode update queued: " + performanceMode);
        }
    }

    private WaifuAppDelegate() {
        currentModel = 0;

        // Set up Cubism SDK framework.
        cubismOption.logFunction = new WaifuPal.PrintLogFunction();
        cubismOption.loggingLevel = WaifuDefine.cubismLoggingLevel;

        CubismFramework.cleanUp();
        CubismFramework.startUp(cubismOption);
    }
}

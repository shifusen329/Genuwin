package com.genuwin.app.live2d;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import java.util.concurrent.LinkedBlockingQueue;

public class GLRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "GLRenderer";
    private boolean isARMode = false;
    private boolean contextLost = false;
    private boolean needsReinitialization = false;
    private final LinkedBlockingQueue<Runnable> glTaskQueue = new LinkedBlockingQueue<>();

    public interface SurfaceReadyListener {
        void onSurfaceReady();
    }

    private SurfaceReadyListener surfaceReadyListener;
    
    public void setSurfaceReadyListener(SurfaceReadyListener listener) {
        this.surfaceReadyListener = listener;
    }
    
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Check if this is a context restoration after being lost
        if (contextLost) {
            needsReinitialization = true;
            contextLost = false;
        }
        
        // Basic GL setup only - onDrawFrame() handles mode-specific state
        WaifuAppDelegate.getInstance().onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        WaifuAppDelegate.getInstance().onSurfaceChanged(width, height);
        
        // Notify listener that surface is ready
        if (surfaceReadyListener != null) {
            surfaceReadyListener.onSurfaceReady();
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Process queued GL tasks
        Runnable task;
        while ((task = glTaskQueue.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                Log.e(TAG, "Error executing GL task: " + e.getMessage());
            }
        }


        // Enable depth testing for proper 3D rendering
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        
        // Enable blending for transparency
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        
        // FRAMEBUFFER FIX: Ensure proper framebuffer state for Live2D rendering
        // Check and fix framebuffer state before clearing
        int framebufferStatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (framebufferStatus != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "Framebuffer not complete before clear (status: " + framebufferStatus + ")");
            // Try to recover by binding default framebuffer
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
        
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        // FRAMEBUFFER FIX: Verify framebuffer state after clear
        int postClearError = GLES20.glGetError();
        if (postClearError != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "GL error after clear: " + postClearError);
            // Clear error state to prevent propagation
            GLES20.glGetError();
        }
        
        int setupError = GLES20.glGetError();
        if (setupError != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "GL ERROR after setup: " + setupError);
        }
        
        // Render the Live2D model
        try {
            WaifuAppDelegate.getInstance().run();
            
            int renderError = GLES20.glGetError();
            if (renderError != GLES20.GL_NO_ERROR) {
                Log.e(TAG, "GL ERROR after Live2D render: " + renderError);
            }
        } catch (Exception e) {
            Log.e(TAG, "ERROR: Exception during model rendering: " + e.getMessage(), e);
            
            int exceptionError = GLES20.glGetError();
            if (exceptionError != GLES20.GL_NO_ERROR) {
                Log.e(TAG, "GL ERROR after exception: " + exceptionError);
            }
        }
        
    }
    
    /**
     * Set AR mode for transparent rendering
     * Note: This method only sets the flag. All OpenGL state changes are handled
     * in onDrawFrame() to ensure thread safety.
     */
    public void setARMode(boolean arMode) {
        this.isARMode = arMode;
        // All GL calls have been removed. onDrawFrame() will handle the state.
    }
    
    /**
     * Get current AR mode state
     */
    public boolean isARMode() {
        return isARMode;
    }
    
    /**
     * Notify renderer that OpenGL context will be lost
     * Called when app is minimized or context is about to be destroyed
     */
    public void onContextLost() {
        contextLost = true;
        // AR mode state is preserved automatically since it's just a boolean flag
    }
    
    /**
     * Check if context was lost and needs reinitialization
     */
    public boolean needsReinitialization() {
        return needsReinitialization;
    }
    
    /**
     * Mark reinitialization as complete
     */
    public void reinitializationComplete() {
        needsReinitialization = false;
    }
    
    /**
     * Queue a task to be executed on the GL thread
     * Used for thread-safe OpenGL operations from UI thread
     */
    public void queueGLTask(Runnable task) {
        glTaskQueue.offer(task);
    }
    
    /**
     * CRITICAL FIX: Handle surface destruction on GL thread
     * 
     * This method should NOT dispose of application-level singletons
     * (CubismFramework, WaifuLive2DManager) as they need to persist across
     * Activity transitions. These are now managed by WaifuAppDelegate.
     * 
     * This method only handles surface-specific cleanup.
     */
    public void onSurfaceDestroyed() {
        try {
            // Mark as inactive to stop rendering
            // Note: We do NOT dispose of singletons here as they need to persist
            // across Activity transitions (e.g., when navigating to settings)
        } catch (Exception e) {
            Log.e(TAG, "Error during surface cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

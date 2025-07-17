package com.genuwin.app.live2d;

import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.math.CubismViewMatrix;
import com.genuwin.app.managers.TouchManager;
import android.content.Context;
import android.view.MotionEvent;

public class WaifuView {
    private final CubismMatrix44 deviceToScreen;
    private final CubismViewMatrix viewMatrix;
    private final TouchManager touchManager;
    
    // View update management
    private volatile boolean pendingViewUpdate = false;
    private volatile Context pendingUpdateContext = null;

    public WaifuView() {
        deviceToScreen = CubismMatrix44.create();
        viewMatrix = new CubismViewMatrix();
        touchManager = new TouchManager();
    }

    public void initialize() {
        // Use default values when no context is provided
        int width = WaifuAppDelegate.getInstance().getWindowWidth();
        int height = WaifuAppDelegate.getInstance().getWindowHeight();
        float ratio = (float) height / (float) width;
        float left = WaifuDefine.LogicalView.LEFT.getValue();
        float right = WaifuDefine.LogicalView.RIGHT.getValue();
        // CRITICAL FIX: Maintain square viewport to prevent stretching
        // Use fixed coordinates instead of aspect-ratio dependent ones
        float bottom = WaifuDefine.LogicalView.BOTTOM.getValue(); // -1.0
        float top = WaifuDefine.LogicalView.TOP.getValue(); // 1.0

        viewMatrix.setScreenRect(left, right, bottom, top);
        
        // Use dynamic view settings based on user preferences
        float viewScale = WaifuDefine.getViewScale(WaifuAppDelegate.getInstance().getActivity());
        float viewYOffset = WaifuDefine.getViewYOffset(WaifuAppDelegate.getInstance().getActivity());
        
        // Apply uniform scaling to maintain aspect ratio
        viewMatrix.scale(viewScale, viewScale);
        viewMatrix.translateY(viewYOffset);
        
        float screenW = Math.abs(left - right);
        deviceToScreen.loadIdentity();
        deviceToScreen.scale(2.0f / screenW, -2.0f / screenW);
        deviceToScreen.translate(-screenW / 2.0f, -screenW / 2.0f);
    }

    public void initializeSprite() {
        // Not implemented in this version
    }

    public void render() {
        // Process pending view updates on GL thread
        if (pendingViewUpdate && pendingUpdateContext != null) {
            processViewUpdate();
        }
        
        WaifuLive2DManager.getInstance().onUpdate();
    }
    
    /**
     * Process pending view update on GL thread
     */
    private void processViewUpdate() {
        try {
            Context context = pendingUpdateContext;
            if (context != null) {
                float viewScale = WaifuDefine.getViewScale(context);
                float viewYOffset = WaifuDefine.getViewYOffset(context);
                
                if (WaifuDefine.DEBUG_LOG_ENABLE) {
                    WaifuPal.printLog("Processing view update on GL thread - Scale: " + viewScale + ", Offset: " + viewYOffset);
                }
                
                // Reset and reapply view matrix with new settings
                float left = WaifuDefine.LogicalView.LEFT.getValue();
                float right = WaifuDefine.LogicalView.RIGHT.getValue();
                float bottom = WaifuDefine.LogicalView.BOTTOM.getValue();
                float top = WaifuDefine.LogicalView.TOP.getValue();

                viewMatrix.setScreenRect(left, right, bottom, top);
                viewMatrix.scale(viewScale, viewScale);
                viewMatrix.translateY(viewYOffset);
                
                if (WaifuDefine.DEBUG_LOG_ENABLE) {
                    WaifuPal.printLog("View matrix updated successfully on GL thread");
                    WaifuPal.printLog("DEBUG: Final view matrix state - Screen rect: (" + left + "," + right + "," + bottom + "," + top + ")");
                    WaifuPal.printLog("DEBUG: Final view matrix state - Scale: " + viewScale + ", Y Offset: " + viewYOffset);
                }
            }
        } catch (Exception e) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("Error processing view update: " + e.getMessage());
            }
        } finally {
            // Clear the pending update
            pendingViewUpdate = false;
            pendingUpdateContext = null;
        }
    }

    public CubismViewMatrix getViewMatrix() {
        return viewMatrix;
    }

    public void onTouchesBegan(float x, float y) {
        touchManager.touchesBegan(x, y);
    }

    public void onTouchesMoved(float x, float y) {
        float viewX = transformViewX(touchManager.getLastX());
        float viewY = transformViewY(touchManager.getLastY());

        touchManager.touchesMoved(x, y);

        WaifuLive2DManager.getInstance().onDrag(viewX, viewY);
    }

    public void onTouchesEnded(float x, float y) {
        WaifuLive2DManager live2DManager = WaifuLive2DManager.getInstance();
        live2DManager.onDrag(0.0f, 0.0f);

        float viewX = deviceToScreen.transformX(touchManager.getLastX());
        float viewY = deviceToScreen.transformY(touchManager.getLastY());

        if (touchManager.isDoubleTap()) {
            live2DManager.onDoubleTap(viewX, viewY);
        } else if (touchManager.isTap()) {
            live2DManager.onTap(viewX, viewY);
            touchManager.updateTapTime();
        } else if (touchManager.isFlick()) {
            live2DManager.onSwipe(touchManager.getStartX(), touchManager.getStartY(), touchManager.getLastX(), touchManager.getLastY());
        }
    }

    public void onTouch(MotionEvent event) {
        touchManager.touchesMoved(event.getX(), event.getY());
    }

    public float transformViewX(float deviceX) {
        float screenX = deviceToScreen.transformX(deviceX);
        return viewMatrix.invertTransformX(screenX);
    }

    public float transformViewY(float deviceY) {
        float screenY = deviceToScreen.transformY(deviceY);
        return viewMatrix.invertTransformY(screenY);
    }

    /**
     * Update view settings based on current preferences
     * Call this when settings change to apply new view configuration
     * This method queues the update for GL thread processing
     */
    public void updateViewSettings(Context context) {
        if (context != null) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("Queuing view settings update for GL thread");
            }
            
            // Queue the update for GL thread processing
            pendingUpdateContext = context;
            pendingViewUpdate = true;
        }
    }

    public void close() {
        // Not implemented in this version
    }

    public void preModelDraw(WaifuModel model) {
        // Not implemented in this version
    }

    public void postModelDraw(WaifuModel model) {
        // Not implemented in this version
    }
}

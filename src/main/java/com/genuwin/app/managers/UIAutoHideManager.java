package com.genuwin.app.managers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

/**
 * Manages auto-hide functionality for UI elements in AR mode
 * Provides smooth fade in/out animations and timer-based hiding
 */
public class UIAutoHideManager {
    private static final String TAG = "UIAutoHideManager";
    
    // Animation constants
    private static final int AUTO_HIDE_DELAY = 3000; // 3 seconds
    private static final int FADE_IN_DURATION = 300;
    private static final int FADE_OUT_DURATION = 500;
    
    // UI state enum
    public enum UIState {
        HIDDEN,
        SHOWING,
        VISIBLE,
        HIDING
    }
    
    // UI elements to manage
    private final View topUIContainer;
    private final View bottomUIContainer;
    private final View statusText;
    
    // State management
    private UIState currentState = UIState.VISIBLE;
    private boolean isAutoHideEnabled = true;
    private boolean isPaused = false;
    
    // Timer management
    private final Handler autoHideHandler;
    private final Runnable autoHideRunnable;
    
    // Animation management
    private ValueAnimator currentAnimation;
    
    /**
     * Constructor
     * @param topUIContainer Top UI container (settings, AR button, character button)
     * @param bottomUIContainer Bottom UI container (voice and text buttons)
     * @param statusText Status text banner
     */
    public UIAutoHideManager(View topUIContainer, View bottomUIContainer, View statusText) {
        this.topUIContainer = topUIContainer;
        this.bottomUIContainer = bottomUIContainer;
        this.statusText = statusText;
        
        this.autoHideHandler = new Handler(Looper.getMainLooper());
        this.autoHideRunnable = this::hideUI;
        
        Log.d(TAG, "UIAutoHideManager initialized");
    }
    
    /**
     * Show UI elements with fade-in animation
     */
    public void showUI() {
        if (currentState == UIState.VISIBLE || currentState == UIState.SHOWING) {
            // Already visible or showing, just reset timer
            resetAutoHideTimer();
            return;
        }
        
        Log.d(TAG, "Showing UI elements");
        currentState = UIState.SHOWING;
        
        // Cancel any existing animation
        if (currentAnimation != null) {
            currentAnimation.cancel();
        }
        
        // Cancel auto-hide timer
        autoHideHandler.removeCallbacks(autoHideRunnable);
        
        // Create fade-in animation
        currentAnimation = ValueAnimator.ofFloat(getCurrentAlpha(), 1.0f);
        currentAnimation.setDuration(FADE_IN_DURATION);
        currentAnimation.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            setUIAlpha(alpha);
        });
        
        currentAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentState = UIState.VISIBLE;
                resetAutoHideTimer();
                Log.d(TAG, "UI fade-in complete");
            }
        });
        
        currentAnimation.start();
    }
    
    /**
     * Hide UI elements with fade-out animation
     */
    public void hideUI() {
        if (currentState == UIState.HIDDEN || currentState == UIState.HIDING || isPaused) {
            return;
        }
        
        Log.d(TAG, "Hiding UI elements");
        currentState = UIState.HIDING;
        
        // Cancel any existing animation
        if (currentAnimation != null) {
            currentAnimation.cancel();
        }
        
        // Cancel auto-hide timer
        autoHideHandler.removeCallbacks(autoHideRunnable);
        
        // Create fade-out animation
        currentAnimation = ValueAnimator.ofFloat(getCurrentAlpha(), 0.0f);
        currentAnimation.setDuration(FADE_OUT_DURATION);
        currentAnimation.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            setUIAlpha(alpha);
        });
        
        currentAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentState = UIState.HIDDEN;
                Log.d(TAG, "UI fade-out complete");
            }
        });
        
        currentAnimation.start();
    }
    
    /**
     * Reset the auto-hide timer (restart 3-second countdown)
     */
    public void resetAutoHideTimer() {
        if (!isAutoHideEnabled || isPaused) {
            return;
        }
        
        // Cancel existing timer
        autoHideHandler.removeCallbacks(autoHideRunnable);
        
        // Start new timer
        autoHideHandler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY);
        // Disabled: Too verbose during normal operation
        // Log.d(TAG, "Auto-hide timer reset (3 seconds)");
    }
    
    /**
     * Enable or disable auto-hide functionality
     * @param enabled True to enable auto-hide, false to disable
     */
    public void setAutoHideEnabled(boolean enabled) {
        this.isAutoHideEnabled = enabled;
        
        if (!enabled) {
            // Cancel timer and show UI
            autoHideHandler.removeCallbacks(autoHideRunnable);
            showUI();
        } else {
            // Re-enable and reset timer
            resetAutoHideTimer();
        }
        
        Log.d(TAG, "Auto-hide " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Pause auto-hide functionality (e.g., during voice recording)
     * @param paused True to pause, false to resume
     */
    public void setPaused(boolean paused) {
        this.isPaused = paused;
        
        if (paused) {
            // Cancel timer and show UI
            autoHideHandler.removeCallbacks(autoHideRunnable);
            showUI();
        } else {
            // Resume and reset timer
            resetAutoHideTimer();
        }
        
        Log.d(TAG, "Auto-hide " + (paused ? "paused" : "resumed"));
    }
    
    /**
     * Get current UI visibility state
     * @return Current UIState
     */
    public UIState getCurrentState() {
        return currentState;
    }
    
    /**
     * Check if UI is currently visible (fully or partially)
     * @return True if UI is visible or showing
     */
    public boolean isUIVisible() {
        return currentState == UIState.VISIBLE || currentState == UIState.SHOWING;
    }
    
    /**
     * Force UI to be immediately visible without animation
     */
    public void forceShowUI() {
        if (currentAnimation != null) {
            currentAnimation.cancel();
        }
        autoHideHandler.removeCallbacks(autoHideRunnable);
        
        setUIAlpha(1.0f);
        currentState = UIState.VISIBLE;
        resetAutoHideTimer();
        
        Log.d(TAG, "UI forced to visible state");
    }
    
    /**
     * Force UI to be immediately hidden without animation
     */
    public void forceHideUI() {
        if (currentAnimation != null) {
            currentAnimation.cancel();
        }
        autoHideHandler.removeCallbacks(autoHideRunnable);
        
        setUIAlpha(0.0f);
        currentState = UIState.HIDDEN;
        
        Log.d(TAG, "UI forced to hidden state");
    }
    
    /**
     * Clean up resources
     */
    public void destroy() {
        if (currentAnimation != null) {
            currentAnimation.cancel();
        }
        autoHideHandler.removeCallbacks(autoHideRunnable);
        Log.d(TAG, "UIAutoHideManager destroyed");
    }
    
    /**
     * Set alpha for all UI elements
     * @param alpha Alpha value (0.0 to 1.0)
     */
    private void setUIAlpha(float alpha) {
        if (topUIContainer != null) {
            topUIContainer.setAlpha(alpha);
        }
        if (bottomUIContainer != null) {
            bottomUIContainer.setAlpha(alpha);
        }
        if (statusText != null) {
            statusText.setAlpha(alpha);
        }
    }
    
    /**
     * Get current alpha value from UI elements
     * @return Current alpha value
     */
    private float getCurrentAlpha() {
        if (topUIContainer != null) {
            return topUIContainer.getAlpha();
        }
        return 1.0f;
    }
}

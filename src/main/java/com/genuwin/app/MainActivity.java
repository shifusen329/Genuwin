package com.genuwin.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.content.Intent;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.util.Consumer;

import com.google.common.util.concurrent.ListenableFuture;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.genuwin.app.api.models.ChatMessage;
import com.genuwin.app.live2d.GLRenderer;
import com.genuwin.app.live2d.WaifuAppDelegate;
import com.genuwin.app.live2d.WaifuDefine;
import com.genuwin.app.managers.ApiManager;
import com.genuwin.app.managers.AudioManager;
import com.genuwin.app.managers.TouchManager;
import com.genuwin.app.managers.EmotionManager;
import com.genuwin.app.managers.CharacterManager;
import com.genuwin.app.managers.ConversationManager;
import com.genuwin.app.managers.UIAutoHideManager;
import com.genuwin.app.live2d.WaifuLive2DManager;
import com.genuwin.app.settings.SettingsActivity;
import com.genuwin.app.settings.SettingsManager;
import com.genuwin.app.vad.VADConfig;
import com.genuwin.app.wakeword.WakeWordManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Main activity for the Genuwin app
 */
public class MainActivity extends AppCompatActivity implements ConversationManager.ConversationCallback {
    private static final String TAG = "MainActivity";
    
    private ConversationManager conversationManager;
    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;
    private TextView statusText;
    private TextView arStateIndicator;
    private ImageButton settingsButton;
    private ImageButton reloadButton;
    private Button voiceButton;
    private Button textButton;
    private Button characterButton;
    private Button arButton;
    
    private TouchManager touchManager;
    
    // UI Auto-hide functionality
    private UIAutoHideManager uiAutoHideManager;
    private View topUIContainer;
    private View bottomUIContainer;
    
    // AR functionality
    private PreviewView cameraPreview;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private boolean isARMode = false;
    
    // Wake lock to prevent screen from sleeping
    private PowerManager.WakeLock wakeLock;
    
    // Permission launcher
    private ActivityResultLauncher<String[]> permissionLauncher;

    // Activity result launcher for settings
    private ActivityResultLauncher<Intent> settingsLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        initializeManagers();
        setupPermissions();
        setupUI();
        setupFullscreen();
        
        // Initialize memory system asynchronously
        initializeMemorySystem();
        
        conversationManager = new ConversationManager(this, this);
        conversationManager.start();
    }
    
    private void initializeViews() {
        glSurfaceView = findViewById(R.id.gl_surface_view);
        statusText = findViewById(R.id.status_text);
        settingsButton = findViewById(R.id.settings_button);
        reloadButton = findViewById(R.id.reload_button);
        voiceButton = findViewById(R.id.voice_button);
        textButton = findViewById(R.id.text_button);
        characterButton = findViewById(R.id.character_button);
        arButton = findViewById(R.id.ar_button);
        cameraPreview = findViewById(R.id.camera_preview);
        arStateIndicator = findViewById(R.id.ar_state_indicator);
        
        // Initialize UI containers for auto-hide functionality
        topUIContainer = findViewById(R.id.top_ui_container);
        bottomUIContainer = findViewById(R.id.bottom_ui_container);
        
        // Initialize UIAutoHideManager
        uiAutoHideManager = new UIAutoHideManager(topUIContainer, bottomUIContainer, statusText);
        
        // Setup OpenGL surface view
        glSurfaceView.setEGLContextClientVersion(2);
        
        // Configure GLSurfaceView for transparency capability (fixes surface recreation issue)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // RGBA8888 with alpha channel
        glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT); // Enable transparency
        // Note: setZOrderOnTop() is now managed conditionally in AR mode methods
        
        glRenderer = new GLRenderer();
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        
        // Initialize camera provider future
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        WaifuAppDelegate.getInstance().onStart(this);
    }
    
    private void initializeManagers() {
        touchManager = new TouchManager();
        
        // Initialize ToolManager early to ensure tools are ready
        com.genuwin.app.tools.ToolManager.getInstance(this);
        Log.d(TAG, "ToolManager initialized at app startup");
    }
    
    private void setupPermissions() {
        permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                
                if (allGranted) {
                    updateStatus("Ready to chat with " + WaifuDefine.DEFAULT_WAIFU_NAME + "!");
                } else {
                    updateStatus("Permissions required for voice features");
                    Toast.makeText(this, "Audio permissions are required for voice chat", Toast.LENGTH_LONG).show();
                }
            }
        );
        
        // Request permissions
        requestPermissions();
    }
    
    private void requestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }
        
        if (!permissionsToRequest.isEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        } else {
            updateStatus("Ready to chat with " + WaifuDefine.DEFAULT_WAIFU_NAME + "!");
        }
    }
    
    private void setupUI() {
        settingsButton.setOnClickListener(v -> handleSettingsButtonClick());
        reloadButton.setOnClickListener(v -> reloadModel());
        voiceButton.setOnClickListener(v -> handleVoiceButtonClick());
        textButton.setOnClickListener(v -> handleTextButtonClick());
        characterButton.setOnClickListener(v -> handleCharacterButtonClick());
        arButton.setOnClickListener(v -> handleARButtonClick());
        
        // Setup touch handling for Live2D model
        glSurfaceView.setOnTouchListener(this::handleModelTouch);
        
        // Update character button text with current character
        updateCharacterButton();
        
        // Update AR button text
        updateARButton();
    }
    
    private void setupFullscreen() {
        // Apply fullscreen setting from preferences
        SettingsManager settings = SettingsManager.getInstance(this);
        boolean isFullscreen = settings.isFullscreenMode();
        
        if (isFullscreen) {
            enableFullscreen();
        } else {
            disableFullscreen();
        }
        
        // Apply status text visibility setting
        updateStatusTextVisibility();
    }
    
    private void initializeWaifuPersonality() {
        // This is now handled by the ConversationManager
    }
    
    /**
     * Initialize the memory system asynchronously
     * This ensures the EmbeddingService is properly initialized before any memory operations
     */
    private void initializeMemorySystem() {
        com.genuwin.app.managers.MemoryManager.getInstance(this).initialize()
            .thenRun(() -> {
                Log.d(TAG, "Memory system initialized successfully");
            })
            .exceptionally(throwable -> {
                Log.e(TAG, "Failed to initialize memory system: " + throwable.getMessage(), throwable);
                // Don't crash the app if memory initialization fails
                return null;
            });
    }
    
    private void handleVoiceButtonClick() {
        // Reset auto-hide timer on UI interaction
        if (uiAutoHideManager != null) {
            uiAutoHideManager.showUI();
        }
        conversationManager.startListening();
    }
    
    private void handleSettingsButtonClick() {
        // Reset auto-hide timer on UI interaction
        if (uiAutoHideManager != null) {
            uiAutoHideManager.showUI();
        }
        
        // Log current model state before going to settings
        if (WaifuAppDelegate.getInstance().getLive2DManager() != null) {
            int currentModel = WaifuAppDelegate.getInstance().getLive2DManager().getCurrentModel();
        }
        
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    
    private void handleTextButtonClick() {
        // Reset auto-hide timer on UI interaction
        if (uiAutoHideManager != null) {
            uiAutoHideManager.showUI();
        }
        // TODO: Implement text input dialog
        Toast.makeText(this, "Text input coming soon!", Toast.LENGTH_SHORT).show();
    }
    
    private void handleCharacterButtonClick() {
        // Reset auto-hide timer on UI interaction
        if (uiAutoHideManager != null) {
            uiAutoHideManager.showUI();
        }
        
        // Notify emotion manager of user interaction
        EmotionManager.getInstance().onUserInteraction();
        
        // Switch to next character
        CharacterManager.getInstance(this).switchToNextCharacter(new CharacterManager.CharacterSwitchCallback() {
            @Override
            public void onCharacterSwitched(CharacterManager.CharacterInfo newCharacter, CharacterManager.CharacterInfo previousCharacter) {
                runOnUiThread(() -> {
                    updateCharacterButton();
                    updateStatus("Switched to " + newCharacter.displayName + "!");
                    
                    // Show character greeting
                    Toast.makeText(MainActivity.this, newCharacter.greeting, Toast.LENGTH_LONG).show();
                    
                    Log.d(TAG, "Character switched from " + 
                        (previousCharacter != null ? previousCharacter.displayName : "none") + 
                        " to " + newCharacter.displayName);
                });
            }
            
            @Override
            public void onCharacterSwitchError(String error) {
                runOnUiThread(() -> {
                    updateStatus("Character switch failed: " + error);
                    Toast.makeText(MainActivity.this, "Failed to switch character: " + error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Character switch error: " + error);
                });
            }
        });
    }
    
    /**
     * Update the character button text to show current character
     */
    private void updateCharacterButton() {
        CharacterManager.CharacterInfo currentCharacter = CharacterManager.getInstance(this).getCurrentCharacter();
        if (currentCharacter != null) {
            characterButton.setText(currentCharacter.displayName);
        } else {
            characterButton.setText("Switch Character");
        }
    }
    
    private boolean handleModelTouch(View v, MotionEvent event) {
        // Check if we're in SPEAKING state and handle TTS interruption FIRST
        // This must happen before UI logic to ensure tap-to-interrupt works in AR mode
        if (conversationManager.getCurrentState() == ConversationManager.State.SPEAKING) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Start touch tracking
                touchManager.touchesBegan(event.getX(), event.getY());
                Log.d(TAG, "Touch down during speaking - tracking for interruption");
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                // Check for tap vs double tap
                if (touchManager.isTap()) {
                    // Check for double tap BEFORE updating tap time
                    boolean isDoubleTap = touchManager.isDoubleTap();
                    // Now update the tap time for future double tap detection
                    touchManager.updateTapTime();
                    
                    if (isDoubleTap) {
                        // Double tap - interrupt to idle mode
                        Log.d(TAG, "Double tap detected during speaking - interrupting to idle");
                        conversationManager.interruptToIdle();
                    } else {
                        // Single tap - wait a bit to make sure it's not a double tap
                        new android.os.Handler().postDelayed(() -> {
                            // Check again if this became part of a double tap sequence
                            if (!touchManager.isDoubleTap()) {
                                Log.d(TAG, "Single tap confirmed during speaking - interrupting to listening");
                                conversationManager.interruptToListening();
                            } else {
                                Log.d(TAG, "Single tap cancelled - was part of double tap sequence");
                            }
                        }, 300); // 300ms delay to detect double tap
                    }
                }
            }
            
            // Handle UI showing AFTER speech interruption logic
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Show UI on touch, but only in non-AR mode or if AR mode allows it
                if (uiAutoHideManager != null && !isARMode) {
                    uiAutoHideManager.showUI();
                } else if (uiAutoHideManager != null && isARMode) {
                    // In AR mode, still show UI briefly on touch for user feedback
                    uiAutoHideManager.showUI();
                }
            }
            
            return true; // Consume the touch event during speaking
        } else {
            // Not speaking - handle UI interactions only (tap motions disabled)
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Show UI on any screen touch (except in AR mode where it auto-hides)
                if (uiAutoHideManager != null) {
                    uiAutoHideManager.showUI();
                }
                
                // Notify emotion manager of user interaction for idle animation reset
                EmotionManager.getInstance().onUserInteraction();
            }
            
            // TAP MOTION FEATURE DISABLED: No longer forwarding touches to Live2D
            // This prevents race condition between tap motions and speech interruption
            // WaifuAppDelegate.getInstance().onTouch(event); // DISABLED
            
            return true; // Consume the touch event
        }
    }
    
    @Override
    public void onStateChanged(ConversationManager.State state) {
        runOnUiThread(() -> {
            // Manage auto-hide based on conversation state
            if (uiAutoHideManager != null) {
                if (isARMode) {
                    // In AR mode: ALWAYS keep UI hidden, regardless of speaking state
                    // UI will only show on touch via handleModelTouch()
                    uiAutoHideManager.forceHideUI();
                } else {
                    // Normal mode: Use existing auto-hide logic
                    switch (state) {
                        case LISTENING:
                            // Pause auto-hide during voice recording
                            uiAutoHideManager.setPaused(true);
                            break;
                        case SPEAKING:
                            // Pause auto-hide during TTS playback
                            uiAutoHideManager.setPaused(true);
                            break;
                        case IDLE:
                        case PROCESSING:
                            // Resume auto-hide during idle and processing
                            uiAutoHideManager.setPaused(false);
                            break;
                    }
                }
            }
            
            // Update AR state indicator if in AR mode
            updateARStateIndicator(state);
            
            switch (state) {
                case IDLE:
                    updateStatus("Ready to chat!");
                    voiceButton.setText("Voice Chat");
                    break;
                case LISTENING:
                    updateStatus("Listening...");
                    voiceButton.setText("Stop Listening");
                    break;
                case PROCESSING:
                    updateStatus("Processing...");
                    break;
                case SPEAKING:
                    updateStatus("Speaking...");
                    break;
            }
        });
    }

    @Override
    public void onTranscription(String text) {
        runOnUiThread(() -> updateStatus("You: " + text));
    }

    @Override
    public void onResponse(String text) {
        runOnUiThread(() -> updateStatus(WaifuDefine.DEFAULT_WAIFU_NAME + ": " + text));
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            updateStatus("Error: " + error);
            Log.e(TAG, "Conversation error: " + error);
        });
    }
    
    private void updateStatus(String status) {
        statusText.setText(status);
        Log.d(TAG, "Status: " + status);
    }
    
    /**
     * Update AR state indicator based on conversation state
     * Shows visual feedback when UI is hidden in AR mode
     */
    private void updateARStateIndicator(ConversationManager.State state) {
        if (arStateIndicator == null) return;
        
        if (isARMode) {
            // Show indicator in AR mode
            arStateIndicator.setVisibility(View.VISIBLE);
            
            // Update indicator based on state
            switch (state) {
                case IDLE:
                    arStateIndicator.setText("●");
                    arStateIndicator.setTextColor(0xFF00FF00); // Green - ready
                    break;
                case LISTENING:
                    arStateIndicator.setText("●");
                    arStateIndicator.setTextColor(0xFFFF0000); // Red - listening
                    // Add pulsing animation for listening
                    arStateIndicator.animate()
                        .alpha(0.3f)
                        .setDuration(500)
                        .withEndAction(() -> {
                            if (conversationManager.getCurrentState() == ConversationManager.State.LISTENING) {
                                arStateIndicator.animate()
                                    .alpha(1.0f)
                                    .setDuration(500)
                                    .withEndAction(() -> updateARStateIndicator(ConversationManager.State.LISTENING));
                            }
                        });
                    break;
                case PROCESSING:
                    arStateIndicator.setText("●");
                    arStateIndicator.setTextColor(0xFFFFFF00); // Yellow - processing
                    // Add rotation animation for processing
                    arStateIndicator.animate()
                        .rotation(arStateIndicator.getRotation() + 360)
                        .setDuration(1000)
                        .withEndAction(() -> {
                            if (conversationManager.getCurrentState() == ConversationManager.State.PROCESSING) {
                                updateARStateIndicator(ConversationManager.State.PROCESSING);
                            }
                        });
                    break;
                case SPEAKING:
                    arStateIndicator.setText("●");
                    arStateIndicator.setTextColor(0xFF0080FF); // Blue - speaking
                    // Add scale animation for speaking
                    arStateIndicator.animate()
                        .scaleX(1.3f)
                        .scaleY(1.3f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            arStateIndicator.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(300)
                                .withEndAction(() -> {
                                    if (conversationManager.getCurrentState() == ConversationManager.State.SPEAKING) {
                                        updateARStateIndicator(ConversationManager.State.SPEAKING);
                                    }
                                });
                        });
                    break;
            }
        } else {
            // Hide indicator in normal mode
            arStateIndicator.setVisibility(View.GONE);
            // Clear any ongoing animations
            arStateIndicator.clearAnimation();
            arStateIndicator.animate().cancel();
            // Reset transformations
            arStateIndicator.setAlpha(0.8f);
            arStateIndicator.setRotation(0);
            arStateIndicator.setScaleX(1.0f);
            arStateIndicator.setScaleY(1.0f);
        }
    }
    
    /**
     * Toggle fullscreen mode based on settings
     * Called from VisualSettingsFragment when fullscreen setting changes
     */
    public void updateFullscreenMode() {
        SettingsManager settings = SettingsManager.getInstance(this);
        boolean isFullscreen = settings.isFullscreenMode();
        
        runOnUiThread(() -> {
            if (isFullscreen) {
                enableFullscreen();
            } else {
                disableFullscreen();
            }
            Log.d(TAG, "Fullscreen mode updated: " + isFullscreen);
        });
    }
    
    /**
     * Enable fullscreen mode
     */
    private void enableFullscreen() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            getWindow().getInsetsController().hide(WindowInsets.Type.navigationBars() | WindowInsets.Type.statusBars());
            getWindow().getInsetsController().setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }
    
    /**
     * Disable fullscreen mode
     */
    private void disableFullscreen() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        } else {
            getWindow().getInsetsController().show(WindowInsets.Type.navigationBars() | WindowInsets.Type.statusBars());
        }
    }
    
    /**
     * Update status text visibility based on settings
     * Called from VisualSettingsFragment when show status text setting changes
     */
    public void updateStatusTextVisibility() {
        SettingsManager settings = SettingsManager.getInstance(this);
        boolean showStatusText = settings.getBoolean(SettingsManager.Keys.SHOW_STATUS_TEXT, SettingsManager.Defaults.SHOW_STATUS_TEXT);
        
        runOnUiThread(() -> {
            statusText.setVisibility(showStatusText ? View.VISIBLE : View.GONE);
            Log.d(TAG, "Status text visibility updated: " + (showStatusText ? "visible" : "hidden"));
        });
    }
    
    /**
     * Update performance mode settings
     * Called from VisualSettingsFragment when performance mode setting changes
     */
    public void updatePerformanceMode() {
        SettingsManager settings = SettingsManager.getInstance(this);
        String performanceMode = settings.getString(SettingsManager.Keys.PERFORMANCE_MODE, SettingsManager.Defaults.PERFORMANCE_MODE);
        
        // Apply performance mode to Live2D rendering
        WaifuAppDelegate.getInstance().updatePerformanceMode(performanceMode);
        
        Log.d(TAG, "Performance mode updated: " + performanceMode);
    }
    
    /**
     * Apply all visual settings changes
     * Called when returning from settings if visual settings changed
     */
    private void applyVisualSettings() {
        // Log current model state before applying settings
        if (WaifuAppDelegate.getInstance().getLive2DManager() != null) {
            int currentModel = WaifuAppDelegate.getInstance().getLive2DManager().getCurrentModel();
        }
        
        // Update fullscreen mode
        updateFullscreenMode();
        
        // Update status text visibility
        updateStatusTextVisibility();
        
        // Update performance mode
        updatePerformanceMode();
        
        // Update Live2D view settings (most important for the view switching issue)
        WaifuAppDelegate.getInstance().updateViewSettings();
        
        if (WaifuAppDelegate.getInstance().getLive2DManager() != null) {
            int currentModel = WaifuAppDelegate.getInstance().getLive2DManager().getCurrentModel();
        }
    }
    
    /**
     * Acquire wake lock to prevent screen from sleeping
     */
    private void acquireWakeLock() {
        try {
            if (wakeLock == null) {
                PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (powerManager != null) {
                    wakeLock = powerManager.newWakeLock(
                        PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "Genuwin:ScreenWakeLock"
                    );
                    wakeLock.setReferenceCounted(false);
                }
            }
            
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire();
                Log.d(TAG, "Wake lock acquired - screen will stay on");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to acquire wake lock: " + e.getMessage());
        }
    }
    
    /**
     * Release wake lock to allow screen to sleep normally
     */
    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "Wake lock released - screen can sleep normally");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to release wake lock: " + e.getMessage());
        }
    }
    
    // ========== AR FUNCTIONALITY ==========
    
    /**
     * Handle AR button click to toggle AR mode
     */
    private void handleARButtonClick() {
        // Reset auto-hide timer on UI interaction
        if (uiAutoHideManager != null) {
            uiAutoHideManager.showUI();
        }
        
        // Check camera permission first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Camera permission required for AR mode", Toast.LENGTH_SHORT).show();
            requestPermissions();
            return;
        }
        
        // Toggle AR mode
        toggleARMode();
    }
    
    /**
     * Toggle between normal and AR mode
     */
    private void toggleARMode() {
        isARMode = !isARMode;
        
        if (isARMode) {
            enableARMode();
        } else {
            disableARMode();
        }
        
        updateARButton();
        Log.d(TAG, "AR mode toggled: " + (isARMode ? "enabled" : "disabled"));
    }
    
    /**
     * Enable AR mode - show camera and make GL transparent
     */
    private void enableARMode() {
        try {
            Log.d(TAG, "Attempting to enable AR mode. Setting glRenderer.setARMode(true)");
            
            // Show camera preview
            cameraPreview.setVisibility(View.VISIBLE);
            
            // Start camera
            startCamera();
            
            // Set GL surface to render on top for AR mode (but UI elements will still be visible)
            glSurfaceView.setZOrderOnTop(true);
            
            // Set renderer to AR mode (surface already configured for transparency)
            glRenderer.setARMode(true);
            
            // Force hide UI in AR mode - UI will only show on touch
            if (uiAutoHideManager != null) {
                uiAutoHideManager.forceHideUI();
            }
            
            updateStatus("AR Mode: Live2D overlay on camera");
            
        } catch (Exception e) {
            Log.e(TAG, "Error enabling AR mode: " + e.getMessage());
            Toast.makeText(this, "Failed to enable AR mode: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            
            // Revert to normal mode on error
            isARMode = false;
            disableARMode();
            updateARButton();
        }
    }
    
    /**
     * Disable AR mode - hide camera and restore normal GL
     */
    private void disableARMode() {
        try {
            Log.d(TAG, "Attempting to disable AR mode. Setting glRenderer.setARMode(false)");
            
            // Stop camera
            stopCamera();
            
            // Hide camera preview
            cameraPreview.setVisibility(View.GONE);
            
            // Restore normal Z-order so UI elements are visible
            glSurfaceView.setZOrderOnTop(false);
            
            // Set renderer to normal mode (surface remains configured for transparency)
            glRenderer.setARMode(false);
            
            // Restore normal UI auto-hide behavior when exiting AR mode
            if (uiAutoHideManager != null) {
                uiAutoHideManager.setAutoHideEnabled(true);
                uiAutoHideManager.showUI(); // Show UI when exiting AR mode
            }
            
            updateStatus("Normal Mode: Live2D character");
            
        } catch (Exception e) {
            Log.e(TAG, "Error disabling AR mode: " + e.getMessage());
        }
    }
    
    /**
     * Start camera for AR mode
     */
    private void startCamera() {
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                
                // Create preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());
                
                // Select back camera
                CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();
                
                // Unbind any existing use cases before rebinding
                cameraProvider.unbindAll();
                
                // Bind preview to lifecycle
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);
                
                Log.d(TAG, "Camera started successfully for AR mode");
                
            } catch (Exception e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
                Toast.makeText(this, "Failed to start camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                
                // Revert to normal mode on camera error
                runOnUiThread(() -> {
                    isARMode = false;
                    disableARMode();
                    updateARButton();
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    /**
     * Stop camera
     */
    private void stopCamera() {
        try {
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
                Log.d(TAG, "Camera stopped");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping camera: " + e.getMessage());
        }
    }
    
    /**
     * Update AR button text based on current mode
     */
    private void updateARButton() {
        if (isARMode) {
            arButton.setText("Exit AR");
            arButton.setBackgroundColor(0xFF4CAF50); // Green when active
        } else {
            arButton.setText("AR");
            arButton.setBackgroundColor(0x80333333); // Default gray
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        glSurfaceView.onResume();

        View decor = this.getWindow().getDecorView();
        decor.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }
    
    @Override
    protected void onPause() {
        super.onPause();

        glSurfaceView.onPause();
        WaifuAppDelegate.getInstance().onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        WaifuAppDelegate.getInstance().onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        WaifuAppDelegate.getInstance().onDestroy();

    }

    private void reloadModel() {
        WaifuAppDelegate.getInstance().getLive2DManager().changeScene(
            WaifuAppDelegate.getInstance().getLive2DManager().getCurrentModel(), true
        );
    }
    
    /**
     * Debug method to print model coordinates and view settings
     * This helps identify if the model is being rendered off-screen
     */
    private void debugModelCoordinates() {
        try {
            // Get current model state
            if (WaifuAppDelegate.getInstance().getLive2DManager() != null) {
                int currentModel = WaifuAppDelegate.getInstance().getLive2DManager().getCurrentModel();
            }
            
            // Get window dimensions
            int windowWidth = WaifuAppDelegate.getInstance().getWindowWidth();
            int windowHeight = WaifuAppDelegate.getInstance().getWindowHeight();
            
            // Get view settings from WaifuDefine
            float viewScale = WaifuDefine.getViewScale(this);
            float viewYOffset = WaifuDefine.getViewYOffset(this);
            
            // Get logical view coordinates
            float left = WaifuDefine.LogicalView.LEFT.getValue();
            float right = WaifuDefine.LogicalView.RIGHT.getValue();
            float bottom = WaifuDefine.LogicalView.BOTTOM.getValue();
            float top = WaifuDefine.LogicalView.TOP.getValue();
            
            // Calculate effective model position
            float effectiveLeft = left * viewScale;
            float effectiveRight = right * viewScale;
            float effectiveBottom = (bottom * viewScale) + viewYOffset;
            float effectiveTop = (top * viewScale) + viewYOffset;
            
            // Check if model is within viewport
            boolean isVisible = (effectiveLeft < right && effectiveRight > left && effectiveBottom < top && effectiveTop > bottom);
            
            // Check if model is off-screen
            if (!isVisible) {
                if (effectiveTop < bottom) {
                }
                if (effectiveBottom > top) {
                }
                if (effectiveLeft > right) {
                }
                if (effectiveRight < left) {
                }
            }
            
            // Get view matrix info if available
            if (WaifuAppDelegate.getInstance().getView() != null) {
            } else {
            }
            
        } catch (Exception e) {
        }
    }
}

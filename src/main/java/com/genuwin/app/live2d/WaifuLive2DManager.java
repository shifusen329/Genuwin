package com.genuwin.app.live2d;

import android.content.res.AssetManager;

import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.motion.ACubismMotion;
import com.live2d.sdk.cubism.framework.motion.IFinishedMotionCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.genuwin.app.live2d.WaifuDefine.DEBUG_LOG_ENABLE;
import static com.genuwin.app.live2d.WaifuDefine.MotionGroup;
import static com.genuwin.app.live2d.WaifuDefine.Priority;
import static com.genuwin.app.live2d.WaifuDefine.HitAreaName;

import com.genuwin.app.managers.LipSyncManager;
import com.genuwin.app.managers.EmotionManager;
import com.genuwin.app.managers.CharacterManager;
import com.genuwin.app.settings.SettingsManager;

public class WaifuLive2DManager {
    private static WaifuLive2DManager s_instance;
    private final List<WaifuModel> models = new ArrayList<>();
    private int currentModel;
    private final List<String> modelDir = new ArrayList<>();
    private final CubismMatrix44 viewMatrix = CubismMatrix44.create();
    private final CubismMatrix44 projection = CubismMatrix44.create();
    
    // Model loading queue for GL thread
    private volatile ModelLoadRequest pendingLoadRequest = null;
    private volatile boolean isLoading = false;
    private final List<WaifuModel> oldModels = new ArrayList<>(); // Keep old models during transition
    
    private static class ModelLoadRequest {
        final String modelPath;
        final String modelJsonName;
        final String modelDirName;
        final int targetIndex;
        
        ModelLoadRequest(String modelPath, String modelJsonName, String modelDirName, int targetIndex) {
            this.modelPath = modelPath;
            this.modelJsonName = modelJsonName;
            this.modelDirName = modelDirName;
            this.targetIndex = targetIndex;
        }
    }

    public static WaifuLive2DManager getInstance() {
        if (s_instance == null) {
            s_instance = new WaifuLive2DManager();
        }
        return s_instance;
    }

    public static void releaseInstance() {
        s_instance = null;
    }

    public void releaseAllModels() {
        for (WaifuModel model : models) {
            // CRITICAL: Properly cleanup model resources
            try {
                model.deleteModel();
            } catch (Exception e) {
                // Log error if needed, but debug log is disabled
            }
        }
        models.clear();
    }

    public void setUpModels() {
        modelDir.clear();
        final AssetManager assets = WaifuAppDelegate.getInstance().getActivity().getResources().getAssets();
        try {
            String[] root = assets.list("");
            for (String subdir : root) {
                try {
                    String[] subdirFiles = assets.list(subdir);
                    for (String file : subdirFiles) {
                        if (file.endsWith(".model3.json")) {
                            modelDir.add(subdir);
                            break;
                        }
                    }
                } catch (IOException e) {
                    // Not a directory, skip
                }
            }
            Collections.sort(modelDir);

            // Prioritize becca_vts model
            if (modelDir.contains("becca_vts")) {
                modelDir.remove("becca_vts");
                modelDir.add(0, "becca_vts");
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void onUpdate() {
        // CRITICAL FIX: Process pending model load requests on GL thread
        if (pendingLoadRequest != null) {
            processModelLoadRequest();
        }
        
        int width = WaifuAppDelegate.getInstance().getWindowWidth();
        int height = WaifuAppDelegate.getInstance().getWindowHeight();


        for (int i = 0; i < models.size(); i++) {
            WaifuModel model = models.get(i);
            if (model.getModel() == null) {
                if (DEBUG_LOG_ENABLE) {
                    WaifuPal.printLog("WaifuLive2DManager: Model " + i + " has null getModel() - skipping");
                }
                continue;
            }


            projection.loadIdentity();
            if (model.getModel().getCanvasWidth() > 1.0f && width < height) {
                model.getModelMatrix().setWidth(2.0f);
                projection.scale(1.0f, (float) width / (float) height);
            } else {
                projection.scale((float) height / (float) width, 1.0f);
            }

            CubismMatrix44 projectionMatrix = WaifuAppDelegate.getInstance().getView().getViewMatrix();
            if (projectionMatrix != null) {
                projection.multiplyByMatrix(projectionMatrix);
            } else {
                if (DEBUG_LOG_ENABLE) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime % 5000 < 16) { // Log every ~5 seconds
                        WaifuPal.printLog("WaifuLive2DManager: WARNING - View matrix is null");
                    }
                }
            }


            try {
                WaifuAppDelegate.getInstance().getView().preModelDraw(model);
                model.update();
                model.draw(projection);
                WaifuAppDelegate.getInstance().getView().postModelDraw(model);
                
            } catch (Exception e) {
                if (DEBUG_LOG_ENABLE) {
                    WaifuPal.printLog("WaifuLive2DManager: Error rendering model " + i + ": " + e.getMessage());
                }
                e.printStackTrace();
            }
        }
        
    }
    
    private void processModelLoadRequest() {
        ModelLoadRequest request = pendingLoadRequest;
        if (request == null) return;
        
        try {
            // Clear current models first, but keep old models for rendering
            models.clear();
            
            // Create and load new model on GL thread with proper context
            WaifuModel newModel = new WaifuModel();
            newModel.loadAssets(request.modelPath, request.modelJsonName);
            
            // Only add the new model if it loaded successfully
            if (newModel.getModel() != null) {
                models.add(newModel);
                
                // Now that new model is ready, safely dispose of old models
                for (WaifuModel oldModel : oldModels) {
                    try {
                        oldModel.deleteModel();
                    } catch (Exception e) {
                        // Log error if needed
                    }
                }
                oldModels.clear();
            } else {
                // Model failed to load, restore old models
                models.addAll(oldModels);
                oldModels.clear();
                throw new RuntimeException("Model failed to load - getModel() returned null");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            
            // Restore old models if new model failed
            if (models.isEmpty() && !oldModels.isEmpty()) {
                models.addAll(oldModels);
                oldModels.clear();
            }
            
            // Try to recover by loading the first model
            if (request.targetIndex != 0) {
                currentModel = 0;
                pendingLoadRequest = null;
                isLoading = false;
                changeScene(0);
                return;
            }
        } finally {
            // Clear the request and loading flag
            pendingLoadRequest = null;
            isLoading = false;
        }
    }

    public void onDrag(float x, float y) {
        for (int i = 0; i < models.size(); i++) {
            WaifuModel model = getModel(i);
            model.setDragging(x, y);
        }
    }

    public void onTap(float x, float y) {
        // Notify emotion manager of user interaction
        EmotionManager.getInstance().onUserInteraction();

        for (int i = 0; i < models.size(); i++) {
            WaifuModel model = models.get(i);
            if (model.hitTest(HitAreaName.HEAD.getId(), x, y)) {
                // Try TapHead first, fallback to TapBody if not available
                int result = model.startRandomMotion(MotionGroup.TAP_HEAD.getId(), Priority.NORMAL.getPriority(), null);
                if (result == -1) {
                    model.startRandomMotion(MotionGroup.TAP_BODY.getId(), Priority.NORMAL.getPriority(), null);
                }
            } else if (model.hitTest(HitAreaName.BODY.getId(), x, y)) {
                model.startRandomMotion(MotionGroup.TAP_BODY.getId(), Priority.NORMAL.getPriority(), null);
            }
        }
    }

    public void onDoubleTap(float x, float y) {
        // Notify emotion manager of user interaction
        EmotionManager.getInstance().onUserInteraction();

        for (int i = 0; i < models.size(); i++) {
            WaifuModel model = models.get(i);
            if (model.hitTest(HitAreaName.HEAD.getId(), x, y)) {
                model.setRandomExpression();
            }
        }
    }

    public void onSwipe(float startX, float startY, float endX, float endY) {
        // Notify emotion manager of user interaction
        EmotionManager.getInstance().onUserInteraction();

        for (int i = 0; i < models.size(); i++) {
            WaifuModel model = models.get(i);
            if (model.hitTest(HitAreaName.HEAD.getId(), startX, startY)) {
                // Try SwipeHead first, fallback to TapHead, then TapBody if not available
                int result = model.startRandomMotion(MotionGroup.SWIPE_HEAD.getId(), Priority.NORMAL.getPriority(), null);
                if (result == -1) {
                    result = model.startRandomMotion(MotionGroup.TAP_HEAD.getId(), Priority.NORMAL.getPriority(), null);
                    if (result == -1) {
                        model.startRandomMotion(MotionGroup.TAP_BODY.getId(), Priority.NORMAL.getPriority(), null);
                    }
                }
            }
        }
    }

    public void nextScene() {
        final int number = (currentModel + 1) % modelDir.size();
        changeScene(number);
    }

    public void changeScene(int index) {
        changeScene(index, false); // Overload for backward compatibility
    }

    public void changeScene(int index, boolean forceReload) {
        if (modelDir.isEmpty()) {
            return;
        }
        
        if (index < 0 || index >= modelDir.size()) {
            return;
        }
        
        // Don't switch if already on the same model and not loading, unless forcing a reload
        if (!forceReload && index == currentModel && !models.isEmpty() && !isLoading) {
            return;
        }
        
        // Don't start new loading if already loading
        if (isLoading) {
            return;
        }
        
        currentModel = index;

        String modelDirName = modelDir.get(index);
        String modelPath = modelDirName + "/";
        String modelJsonName = "";

        try {
            String[] files = WaifuAppDelegate.getInstance().getActivity().getAssets().list(modelDirName);
            for (String file : files) {
                if (file.endsWith(".model3.json")) {
                    modelJsonName = file;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (modelJsonName.isEmpty()) {
            return;
        }

        // CRITICAL FIX: Keep old models visible during transition to prevent flickering
        oldModels.clear();
        oldModels.addAll(models); // Keep old models for rendering during transition
        
        // Queue model loading for GL thread instead of loading immediately
        pendingLoadRequest = new ModelLoadRequest(modelPath, modelJsonName, modelDirName, index);
        isLoading = true;
        
        // Don't clear models immediately - let them render until new model is ready
    }

    public WaifuModel getModel(int number) {
        if (number < models.size()) {
            return models.get(number);
        }
        return null;
    }

    public WaifuModel getWaifuModel() {
        // During loading, return the first available model (old or new)
        if (models.isEmpty()) {
            return null;
        }
        return models.get(0);
    }

    public int getCurrentModel() {
        return currentModel;
    }

    public int getModelNum() {
        if (models == null) {
            return 0;
        }
        return models.size();
    }

    /**
     * Get the model index for a given character directory name
     */
    public int getModelIndexForCharacter(String characterDirectory) {
        if (characterDirectory == null || modelDir.isEmpty()) {
            return -1;
        }
        
        for (int i = 0; i < modelDir.size(); i++) {
            if (characterDirectory.equals(modelDir.get(i))) {
                return i;
            }
        }
        
        return -1;
    }

    /**
     * Get the character directory name for the current model
     */
    public String getCurrentCharacterDirectory() {
        if (currentModel >= 0 && currentModel < modelDir.size()) {
            return modelDir.get(currentModel);
        }
        return null;
    }
    
    /**
     * Change to a specific model by index
     * This is an alias for changeScene() for better API clarity
     */
    public void changeModel(int index) {
        changeScene(index);
    }

    private static class FinishedMotion implements IFinishedMotionCallback {
        @Override
        public void execute(ACubismMotion motion) {
            WaifuPal.printLog("Motion Finished: " + motion);
        }
    }

    private WaifuLive2DManager() {
        setUpModels();
        if (!modelDir.isEmpty()) {
            // Load the default character from settings instead of always loading index 0
            loadDefaultCharacterFromSettings();
        }
        
        // Initialize LipSyncManager
        LipSyncManager.setLive2DManager(this);
    }

    /**
     * Load the default character from settings, with fallback to first available model
     */
    private void loadDefaultCharacterFromSettings() {
        try {
            // Get the settings manager instance
            SettingsManager settingsManager = SettingsManager.getInstance(
                WaifuAppDelegate.getInstance().getActivity().getApplicationContext()
            );
            
            // Get the user's preferred default character
            String defaultCharacter = settingsManager.getDefaultCharacter();
            
            // Find the model index for the configured character
            int modelIndex = getModelIndexForCharacter(defaultCharacter);
            
            if (modelIndex != -1) {
                // Load the configured character
                changeScene(modelIndex);
                
                // Update CharacterManager to match the loaded model
                updateCharacterManagerIndex(modelIndex);
            } else {
                // Fallback to first available model if configured character not found
                changeScene(0);
                updateCharacterManagerIndex(0);
            }
            
        } catch (Exception e) {
            // Fallback to first model if any error occurs
            changeScene(0);
            updateCharacterManagerIndex(0);
        }
    }

    /**
     * Update CharacterManager to match the currently loaded Live2D model
     */
    private void updateCharacterManagerIndex(int modelIndex) {
        try {
            if (modelIndex >= 0 && modelIndex < modelDir.size()) {
                String characterDirectory = modelDir.get(modelIndex);
                CharacterManager characterManager = CharacterManager.getInstance(WaifuAppDelegate.getInstance().getActivity());
                
                // Find the character index in CharacterManager that matches this directory
                List<CharacterManager.CharacterInfo> characters = characterManager.getAllCharacters();
                for (int i = 0; i < characters.size(); i++) {
                    if (characterDirectory.equals(characters.get(i).modelDirectory)) {
                        // Update CharacterManager to match the loaded model
                        characterManager.switchToCharacter(i, new CharacterManager.CharacterSwitchCallback() {
                            @Override
                            public void onCharacterSwitched(CharacterManager.CharacterInfo newCharacter, 
                                                           CharacterManager.CharacterInfo previousCharacter) {
                                // Log if needed
                            }
                            
                            @Override
                            public void onCharacterSwitchError(String error) {
                                // Log if needed
                            }
                        });
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // Log if needed
        }
    }
}

package com.genuwin.app.live2d;

import com.live2d.sdk.cubism.framework.CubismDefaultParameterId.ParameterId;
import com.live2d.sdk.cubism.framework.CubismFramework;
import com.live2d.sdk.cubism.framework.CubismModelSettingJson;
import com.live2d.sdk.cubism.framework.ICubismModelSetting;
import com.live2d.sdk.cubism.framework.effect.CubismBreath;
import com.live2d.sdk.cubism.framework.effect.CubismEyeBlink;
import com.live2d.sdk.cubism.framework.id.CubismId;
import com.live2d.sdk.cubism.framework.id.CubismIdManager;
import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.model.CubismMoc;
import com.live2d.sdk.cubism.framework.model.CubismUserModel;
import com.live2d.sdk.cubism.framework.motion.ACubismMotion;
import com.live2d.sdk.cubism.framework.motion.CubismExpressionMotion;
import com.live2d.sdk.cubism.framework.motion.CubismMotion;
import com.live2d.sdk.cubism.framework.motion.IFinishedMotionCallback;
import com.live2d.sdk.cubism.framework.rendering.CubismRenderer;
import com.live2d.sdk.cubism.framework.rendering.android.CubismRendererAndroid;
import com.live2d.sdk.cubism.framework.utils.CubismDebug;
import com.genuwin.app.managers.EmotionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WaifuModel extends CubismUserModel {
    private ICubismModelSetting modelSetting;
    private String modelHomeDirectory;
    private float userTimeSeconds;
    private final List<CubismId> eyeBlinkIds = new ArrayList<>();
    private final List<CubismId> lipSyncIds = new ArrayList<>();
    private final Map<String, ACubismMotion> motions = new HashMap<>();
    private final Map<String, ACubismMotion> expressions = new HashMap<>();
    private final CubismId idParamAngleX;
    private final CubismId idParamAngleY;
    private final CubismId idParamAngleZ;
    private final CubismId idParamBodyAngleX;
    private final CubismId idParamEyeBallX;
    private final CubismId idParamEyeBallY;

    public WaifuModel() {
        if (WaifuDefine.MOC_CONSISTENCY_VALIDATION_ENABLE) {
            mocConsistency = true;
        }
        if (WaifuDefine.DEBUG_LOG_ENABLE) {
            debugMode = true;
        }

        CubismIdManager idManager = CubismFramework.getIdManager();
        idParamAngleX = idManager.getId(ParameterId.ANGLE_X.getId());
        idParamAngleY = idManager.getId(ParameterId.ANGLE_Y.getId());
        idParamAngleZ = idManager.getId(ParameterId.ANGLE_Z.getId());
        idParamBodyAngleX = idManager.getId(ParameterId.BODY_ANGLE_X.getId());
        idParamEyeBallX = idManager.getId(ParameterId.EYE_BALL_X.getId());
        idParamEyeBallY = idManager.getId(ParameterId.EYE_BALL_Y.getId());
    }

    public void loadAssets(final String dir, final String fileName) {
        modelHomeDirectory = dir;
        String filePath = modelHomeDirectory + fileName;
        byte[] buffer = createBuffer(filePath);
        ICubismModelSetting setting = new CubismModelSettingJson(buffer);
        setupModel(setting);
        if (model == null) {
            WaifuPal.printLog("Failed to loadAssets().");
            return;
        }
        CubismRenderer renderer = CubismRendererAndroid.create();
        setupRenderer(renderer);
        setupTextures();
    }

    public void deleteModel() {
        delete();
    }

    public void update() {
        final float deltaTimeSeconds = WaifuPal.getDeltaTime();
        userTimeSeconds += deltaTimeSeconds;
        
        // Update the emotion manager (now handles idle expression cycling)
        EmotionManager.getInstance().update(deltaTimeSeconds);
        
        dragManager.update(deltaTimeSeconds);
        dragX = dragManager.getX();
        dragY = dragManager.getY();
        boolean isMotionUpdated = false;
        model.loadParameters();
        
        // EmotionManager now handles idle expression cycling
        // Only update motion if there's an active motion
        if (!motionManager.isFinished()) {
            isMotionUpdated = motionManager.updateMotion(model, deltaTimeSeconds);
        }
        
        model.saveParameters();
        if (!isMotionUpdated) {
            if (eyeBlink != null) {
                eyeBlink.updateParameters(model, deltaTimeSeconds);
            }
        }
        if (expressionManager != null) {
            expressionManager.updateMotion(model, deltaTimeSeconds);
        }
        model.addParameterValue(idParamAngleX, dragX * 30);
        model.addParameterValue(idParamAngleY, dragY * 30);
        model.addParameterValue(idParamAngleZ, dragX * dragY * (-30));
        model.addParameterValue(idParamBodyAngleX, dragX * 10);
        model.addParameterValue(idParamEyeBallX, dragX);
        model.addParameterValue(idParamEyeBallY, dragY);
        if (breath != null) {
            breath.updateParameters(model, deltaTimeSeconds);
        }
        if (physics != null) {
            physics.evaluate(model, deltaTimeSeconds);
        }
        // Lip Sync Setting - similar to sample LAppModel
        if (!lipSyncIds.isEmpty()) {
            // Get lip sync value from LipSyncManager
            float lipSyncValue = getLipSyncValue();
            for (CubismId lipSyncId : lipSyncIds) {
                model.addParameterValue(lipSyncId, lipSyncValue, 0.8f);
            }
        }

        if (pose != null) {
            pose.updateParameters(model, deltaTimeSeconds);
        }
        
        // Apply voice parameter values during each update cycle
        try {
            com.genuwin.app.managers.VoiceParameterManager voiceParamManager = 
                com.genuwin.app.managers.VoiceParameterManager.getInstance();
            
            // Apply current voice parameter states
            float param4Value = voiceParamManager.getParameterState("Param4");
            float param5Value = voiceParamManager.getParameterState("Param5");
            float paramCheekValue = voiceParamManager.getParameterState("ParamCheek");
            
            CubismIdManager idManager = CubismFramework.getIdManager();
            model.setParameterValue(idManager.getId("Param4"), param4Value);
            model.setParameterValue(idManager.getId("Param5"), param5Value);
            model.setParameterValue(idManager.getId("ParamCheek"), paramCheekValue);
            
        } catch (Exception e) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("WaifuModel: Error applying voice parameters: " + e.getMessage());
            }
        }
        
        model.update();
    }

    public int startMotion(final String group, int number, int priority, IFinishedMotionCallback onFinishedMotionHandler) {
        if (priority == WaifuDefine.Priority.FORCE.getPriority()) {
            motionManager.setReservationPriority(priority);
        } else if (!motionManager.reserveMotion(priority)) {
            if (debugMode) {
                WaifuPal.printLog("Cannot start motion.");
            }
            return -1;
        }
        String name = group + "_" + number;
        CubismMotion motion = (CubismMotion) motions.get(name);
        if (motion == null) {
            String fileName = modelSetting.getMotionFileName(group, number);
            if (!fileName.equals("")) {
                String path = modelHomeDirectory + fileName;
                byte[] buffer = createBuffer(path);
                motion = (CubismMotion) loadMotion(buffer, onFinishedMotionHandler, null, false);
                if (motion != null) {
                    final float fadeInTime = modelSetting.getMotionFadeInTimeValue(group, number);
                    if (fadeInTime != -1.0f) {
                        motion.setFadeInTime(fadeInTime);
                    }
                    final float fadeOutTime = modelSetting.getMotionFadeOutTimeValue(group, number);
                    if (fadeOutTime != -1.0f) {
                        motion.setFadeOutTime(fadeOutTime);
                    }
                    motion.setEffectIds(eyeBlinkIds, lipSyncIds);
                }
            }
        } else {
            motion.setFinishedMotionHandler(onFinishedMotionHandler);
        }
        return motionManager.startMotionPriority(motion, priority);
    }

    public int startRandomMotion(final String group, int priority, IFinishedMotionCallback onFinishedMotionHandler) {
        if (modelSetting.getMotionCount(group) == 0) {
            return -1;
        }
        Random random = new Random();
        int number = random.nextInt(Integer.MAX_VALUE) % modelSetting.getMotionCount(group);
        return startMotion(group, number, priority, onFinishedMotionHandler);
    }

    public void draw(CubismMatrix44 matrix) {
        if (model == null) {
            return;
        }
        
        // Check if renderer is available before drawing
        CubismRendererAndroid renderer = this.<CubismRendererAndroid>getRenderer();
        if (renderer == null) {
            return;
        }
        
        // CRITICAL FIX: Properly combine view matrix with model matrix instead of overwriting
        try {
            // Create a new matrix to hold the final transformation
            CubismMatrix44 finalMatrix = CubismMatrix44.create();
            
            // Properly multiply: finalMatrix = modelMatrix * viewMatrix
            // This preserves the user's view settings (scale/offset) from the view matrix
            CubismMatrix44.multiply(modelMatrix.getArray(), matrix.getArray(), finalMatrix.getArray());
            
            // Use the combined matrix for rendering
            renderer.setMvpMatrix(finalMatrix);
            
            // FRAMEBUFFER VALIDATION: Check GL state before drawing
            int preDrawError = android.opengl.GLES20.glGetError();
            if (preDrawError != android.opengl.GLES20.GL_NO_ERROR) {
                // Clear the error state before proceeding
                android.opengl.GLES20.glGetError();
            }
            
            // FRAMEBUFFER VALIDATION: Check current framebuffer status
            int framebufferStatus = android.opengl.GLES20.glCheckFramebufferStatus(android.opengl.GLES20.GL_FRAMEBUFFER);
            if (framebufferStatus != android.opengl.GLES20.GL_FRAMEBUFFER_COMPLETE) {
                // Try to recover by ensuring default framebuffer is bound
                android.opengl.GLES20.glBindFramebuffer(android.opengl.GLES20.GL_FRAMEBUFFER, 0);
                
                // Check again after recovery attempt
                framebufferStatus = android.opengl.GLES20.glCheckFramebufferStatus(android.opengl.GLES20.GL_FRAMEBUFFER);
                if (framebufferStatus != android.opengl.GLES20.GL_FRAMEBUFFER_COMPLETE) {
                    return; // Skip this frame to prevent GL_INVALID_FRAMEBUFFER_OPERATION
                }
            }
            
            // CRITICAL FIX: Validate OpenGL context before Live2D draw call
            try {
                // Ensure basic OpenGL state is correct for Live2D rendering
                android.opengl.GLES20.glEnable(android.opengl.GLES20.GL_BLEND);
                android.opengl.GLES20.glBlendFunc(android.opengl.GLES20.GL_ONE, android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA);
                
                // The actual draw call - wrapped in error handling
                renderer.drawModel();
                
                // IMMEDIATE ERROR CHECK: Catch framebuffer errors right after draw
                int postDrawError = android.opengl.GLES20.glGetError();
                if (postDrawError == 1286) { // GL_INVALID_FRAMEBUFFER_OPERATION
                    // RECOVERY: Try to reset framebuffer state for next frame
                    android.opengl.GLES20.glBindFramebuffer(android.opengl.GLES20.GL_FRAMEBUFFER, 0);
                    android.opengl.GLES20.glGetError(); // Clear error state
                    
                } else if (postDrawError != android.opengl.GLES20.GL_NO_ERROR) {
                }
                
            } catch (Exception e) {
                // Clear any GL errors that might have been generated
                android.opengl.GLES20.glGetError();
            }
            
        } catch (IndexOutOfBoundsException e) {
            // Skip this frame if clipping manager isn't ready
        } catch (Exception e) {
            // Handle any other rendering exceptions during model switching
        }
    }

    public boolean hitTest(final String hitAreaName, float x, float y) {
        if (opacity < 1) {
            return false;
        }
        final int count = modelSetting.getHitAreasCount();
        for (int i = 0; i < count; i++) {
            if (modelSetting.getHitAreaName(i).equals(hitAreaName)) {
                final CubismId drawID = modelSetting.getHitAreaId(i);
                return isHit(drawID, x, y);
            }
        }
        return false;
    }

    public void setExpression(final String expressionID) {
        ACubismMotion motion = expressions.get(expressionID);
        if (motion != null) {
            expressionManager.startMotionPriority(motion, WaifuDefine.Priority.FORCE.getPriority());
        }
    }

    public void setRandomExpression() {
        if (expressions.size() == 0) {
            return;
        }
        Random random = new Random();
        int number = random.nextInt(Integer.MAX_VALUE) % expressions.size();
        int i = 0;
        for (String key : expressions.keySet()) {
            if (i == number) {
                setExpression(key);
                return;
            }
            i++;
        }
    }

    public void setLipSyncValue(float value) {
        if (lipSyncIds.isEmpty()) {
            return;
        }
        
        for (CubismId lipSyncId : lipSyncIds) {
            model.setParameterValue(lipSyncId, value);
        }
    }

    /**
     * Get the motion manager to check motion status
     */
    public com.live2d.sdk.cubism.framework.motion.CubismMotionManager getMotionManager() {
        return motionManager;
    }

    private float getLipSyncValue() {
        // Get the current lip sync value from the LipSyncManager
        // This mimics the sample's approach where it gets the value from the system
        return com.genuwin.app.managers.LipSyncManager.getCurrentLipSyncValue();
    }

    private static byte[] createBuffer(final String path) {
        return WaifuPal.loadFileAsBytes(path);
    }

    private void setupModel(ICubismModelSetting setting) {
        modelSetting = setting;
        isUpdated = true;
        isInitialized = false;
        motions.clear();
        expressions.clear();
        String fileName = modelSetting.getModelFileName();
        if (!fileName.equals("")) {
            String path = modelHomeDirectory + fileName;
            byte[] buffer = createBuffer(path);
            loadModel(buffer, mocConsistency);
        }
        if (modelSetting.getExpressionCount() > 0) {
            final int count = modelSetting.getExpressionCount();
            for (int i = 0; i < count; i++) {
                String name = modelSetting.getExpressionName(i);
                String path = modelSetting.getExpressionFileName(i);
                path = modelHomeDirectory + path;
                byte[] buffer = createBuffer(path);
                CubismExpressionMotion motion = loadExpression(buffer);
                if (motion != null) {
                    expressions.put(name, motion);
                }
            }
        }
        String physicsPath = modelSetting.getPhysicsFileName();
        if (!physicsPath.equals("")) {
            String modelPath = modelHomeDirectory + physicsPath;
            byte[] buffer = createBuffer(modelPath);
            loadPhysics(buffer);
        }
        String posePath = modelSetting.getPoseFileName();
        if (!posePath.equals("")) {
            String modelPath = modelHomeDirectory + posePath;
            byte[] buffer = createBuffer(modelPath);
            loadPose(buffer);
        }
        if (modelSetting.getEyeBlinkParameterCount() > 0) {
            eyeBlink = CubismEyeBlink.create(modelSetting);
        }
        breath = CubismBreath.create();
        List<CubismBreath.BreathParameterData> breathParameters = new ArrayList<>();
        breathParameters.add(new CubismBreath.BreathParameterData(idParamAngleX, 0.0f, 15.0f, 6.5345f, 0.5f));
        breathParameters.add(new CubismBreath.BreathParameterData(idParamAngleY, 0.0f, 8.0f, 3.5345f, 0.5f));
        breathParameters.add(new CubismBreath.BreathParameterData(idParamAngleZ, 0.0f, 10.0f, 5.5345f, 0.5f));
        breathParameters.add(new CubismBreath.BreathParameterData(idParamBodyAngleX, 0.0f, 4.0f, 15.5345f, 0.5f));
        breathParameters.add(new CubismBreath.BreathParameterData(CubismFramework.getIdManager().getId(ParameterId.BREATH.getId()), 0.5f, 0.5f, 3.2345f, 0.5f));
        breath.setParameters(breathParameters);
        String userDataPath = modelSetting.getUserDataFile();
        if (!userDataPath.equals("")) {
            String modelPath = modelHomeDirectory + userDataPath;
            byte[] buffer = createBuffer(modelPath);
            loadUserData(buffer);
        }
        int eyeBlinkIdCount = modelSetting.getEyeBlinkParameterCount();
        for (int i = 0; i < eyeBlinkIdCount; i++) {
            eyeBlinkIds.add(modelSetting.getEyeBlinkParameterId(i));
        }
        int lipSyncIdCount = modelSetting.getLipSyncParameterCount();
        for (int i = 0; i < lipSyncIdCount; i++) {
            lipSyncIds.add(modelSetting.getLipSyncParameterId(i));
        }
        if (modelSetting == null || modelMatrix == null) {
            WaifuPal.printLog("Failed to setupModel().");
            return;
        }
        Map<String, Float> layout = new HashMap<>();
        if (modelSetting.getLayoutMap(layout)) {
            modelMatrix.setupFromLayout(layout);
        }
        model.saveParameters();
        for (int i = 0; i < modelSetting.getMotionGroupCount(); i++) {
            String group = modelSetting.getMotionGroupName(i);
            preLoadMotionGroup(group);
        }
        motionManager.stopAllMotions();
        
        // Apply default voice parameter values after model is fully initialized
        try {
            com.genuwin.app.managers.VoiceParameterManager.getInstance().applyDefaultParameters();
        } catch (Exception e) {
            if (WaifuDefine.DEBUG_LOG_ENABLE) {
                WaifuPal.printLog("WaifuModel: Error applying default voice parameters: " + e.getMessage());
            }
        }
        
        isUpdated = false;
        isInitialized = true;
    }

    private void preLoadMotionGroup(final String group) {
        final int count = modelSetting.getMotionCount(group);
        for (int i = 0; i < count; i++) {
            String name = group + "_" + i;
            String path = modelSetting.getMotionFileName(group, i);
            if (!path.equals("")) {
                String modelPath = modelHomeDirectory + path;
                byte[] buffer = createBuffer(modelPath);
                CubismMotion tmp = (CubismMotion) loadMotion(buffer);
                if (tmp == null) {
                    continue;
                }
                final float fadeInTime = modelSetting.getMotionFadeInTimeValue(group, i);
                if (fadeInTime != -1.0f) {
                    tmp.setFadeInTime(fadeInTime);
                }
                final float fadeOutTime = modelSetting.getMotionFadeOutTimeValue(group, i);
                if (fadeOutTime != -1.0f) {
                    tmp.setFadeOutTime(fadeOutTime);
                }
                tmp.setEffectIds(eyeBlinkIds, lipSyncIds);
                motions.put(name, tmp);
            }
        }
    }

    private void setupTextures() {
        // Check if renderer is available before setting up textures
        CubismRendererAndroid renderer = this.<CubismRendererAndroid>getRenderer();
        if (renderer == null) {
            return;
        }
        
        // CRITICAL: Verify GL context is available before texture operations
        try {
            for (int modelTextureNumber = 0; modelTextureNumber < modelSetting.getTextureCount(); modelTextureNumber++) {
                if (modelSetting.getTextureFileName(modelTextureNumber).equals("")) {
                    continue;
                }
                String texturePath = modelSetting.getTextureFileName(modelTextureNumber);
                texturePath = modelHomeDirectory + texturePath;
                
                LAppTextureManager.TextureInfo texture = WaifuAppDelegate.getInstance().getTextureManager().createTextureFromPngFile(texturePath);
                final int glTextureNumber = texture.id;
                renderer.bindTexture(modelTextureNumber, glTextureNumber);
                
                if (WaifuDefine.PREMULTIPLIED_ALPHA_ENABLE) {
                    renderer.isPremultipliedAlpha(true);
                } else {
                    renderer.isPremultipliedAlpha(false);
                }
            }
        } catch (Exception e) {
            // Don't throw - let the model load without textures and retry later
        }
    }
}

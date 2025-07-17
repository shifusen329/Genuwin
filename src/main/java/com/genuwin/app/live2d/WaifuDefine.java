package com.genuwin.app.live2d;

import android.content.Context;
import com.live2d.sdk.cubism.framework.CubismFrameworkConfig.LogLevel;
import com.genuwin.app.settings.SettingsManager;

/**
 * Constants used in the Genuwin app
 */
public class WaifuDefine {
    /**
     * Scaling rate.
     */
    public enum Scale {
        DEFAULT(1.0f),
        MAX(2.0f),
        MIN(0.8f);

        private final float value;

        Scale(float value) {
            this.value = value;
        }

        public float getValue() {
            return value;
        }
    }

    /**
     * Logical view coordinate system.
     */
    public enum LogicalView {
        LEFT(-1.0f),
        RIGHT(1.0f),
        BOTTOM(-1.0f),
        TOP(1.0f);

        private final float value;

        LogicalView(float value) {
            this.value = value;
        }

        public float getValue() {
            return value;
        }
    }

    /**
     * Motion group
     */
    public enum MotionGroup {
        IDLE("Idle"),
        TAP_BODY("TapBody"),
        TAP_HEAD("TapHead"),
        SWIPE_HEAD("SwipeHead"),
        TALKING("Talking"),
        LISTENING("Listening");

        private final String id;

        MotionGroup(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    /**
     * Hit area names for interaction
     */
    public enum HitAreaName {
        HEAD("Head"),
        BODY("Body");

        private final String id;

        HitAreaName(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    /**
     * Motion priority
     */
    public enum Priority {
        NONE(0),
        IDLE(1),
        NORMAL(2),
        FORCE(3);

        private final int priority;

        Priority(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    // Debug and validation settings
    public static final boolean MOC_CONSISTENCY_VALIDATION_ENABLE = true;
    public static final boolean MOTION_CONSISTENCY_VALIDATION_ENABLE = true;
    public static final boolean DEBUG_LOG_ENABLE = false;
    public static final boolean DEBUG_TOUCH_LOG_ENABLE = false;
    public static final LogLevel cubismLoggingLevel = LogLevel.VERBOSE;
    public static final boolean PREMULTIPLIED_ALPHA_ENABLE = true;

    // Rendering settings
    public static final boolean USE_RENDER_TARGET = false;
    public static final boolean USE_MODEL_RENDER_TARGET = false;
    
    /**
     * Get the current view scale based on settings
     * @param context Application context
     * @return View scale (1.0f for full body, 2.0f for waist up)
     */
    public static float getViewScale(Context context) {
        SettingsManager settings = SettingsManager.getInstance(context);
        return settings.isFullBodyView() ? 1.0f : 2.0f;
    }
    
    /**
     * Get the current view Y offset based on settings
     * @param context Application context
     * @return View Y offset (0.0f for full body, -0.3f for waist up)
     */
    public static float getViewYOffset(Context context) {
        SettingsManager settings = SettingsManager.getInstance(context);
        // CRITICAL FIX: Adjusted offset for proper waist-up view positioning
        // -0.3f was too high (torso focus), -0.6f provides proper waist-up framing
        return settings.isFullBodyView() ? 0.0f : -1.0f;
    }
    
    // Waifu personality settings
    public static final String DEFAULT_WAIFU_NAME = "Genuwin";
    public static final String WAIFU_PERSONALITY = "You are a cheerful AI assistant. Keep your responses concise and to the point.";
}

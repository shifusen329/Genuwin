package com.genuwin.app.settings;

import com.genuwin.app.settings.SettingsManager.Keys;
import com.genuwin.app.settings.SettingsManager.Defaults;
import com.genuwin.app.live2d.WaifuAppDelegate;

/**
 * Settings fragment for visual configuration
 */
public class VisualSettingsFragment extends BaseSettingsFragment {
    
    @Override
    protected void loadSettings() {
        settingsItems.clear();
        
        // Display Settings
        settingsItems.add(createHeaderItem("Display"));
        settingsItems.add(createSwitchItem(
            Keys.FULLSCREEN_MODE,
            "Fullscreen Mode",
            "Hide system bars for immersive experience",
            Defaults.FULLSCREEN_MODE
        ));
        settingsItems.add(createSwitchItem(
            Keys.SHOW_STATUS_TEXT,
            "Show Status Text",
            "Display status messages on screen",
            Defaults.SHOW_STATUS_TEXT
        ));
        settingsItems.add(createSpinnerItem(
            Keys.UI_THEME,
            "UI Theme",
            "Color theme for the user interface",
            new String[]{"dark", "light", "auto"},
            Defaults.UI_THEME
        ));
        
        // Character View Settings
        settingsItems.add(createHeaderItem("Character View"));
        settingsItems.add(createSwitchItem(
            Keys.FULL_BODY_VIEW,
            "Full Body View",
            "Show full body character instead of waist-up view (requires app restart)",
            Defaults.FULL_BODY_VIEW
        ));
        
        // Animation Settings
        settingsItems.add(createHeaderItem("Animations"));
        settingsItems.add(createSliderItem(
            Keys.ANIMATION_SPEED,
            "Animation Speed",
            "Speed multiplier for Live2D animations",
            Defaults.ANIMATION_SPEED,
            0.1f,
            3.0f,
            0.1f
        ));
        settingsItems.add(createSliderItem(
            Keys.TOUCH_SENSITIVITY,
            "Touch Sensitivity",
            "Sensitivity for touch interactions",
            Defaults.TOUCH_SENSITIVITY,
            0.1f,
            3.0f,
            0.1f
        ));
        
        // Performance Settings
        settingsItems.add(createHeaderItem("Performance"));
        settingsItems.add(createSpinnerItem(
            Keys.PERFORMANCE_MODE,
            "Performance Mode",
            "Balance between quality and performance",
            new String[]{"high_quality", "balanced", "performance"},
            Defaults.PERFORMANCE_MODE
        ));
        
        // Visual Testing
        settingsItems.add(createHeaderItem("Testing"));
        settingsItems.add(createButtonItem(
            "test_animations",
            "Test Animations",
            "Preview animation speed and touch sensitivity"
        ));
        settingsItems.add(createButtonItem(
            "reset_visual",
            "Reset Visual Settings",
            "Reset all visual settings to defaults"
        ));
        
        adapter.notifyDataSetChanged();
    }
    
    @Override
    protected void handleButtonClick(SettingsItem item) {
        switch (item.key) {
            case "test_animations":
                testAnimations();
                break;
            case "reset_visual":
                resetVisualSettings();
                break;
        }
    }
    
    private void testAnimations() {
        SettingsDialogs.showInfoDialog(getContext(), "Animation Test", 
            "Animation testing will be implemented in a future update. " +
            "This will allow you to preview how animation speed and touch sensitivity " +
            "settings affect the Live2D character interactions.");
    }
    
    private void resetVisualSettings() {
        SettingsDialogs.showConfirmationDialog(getContext(), 
            "Reset Visual Settings", 
            "Are you sure you want to reset all visual settings to their defaults?",
            () -> {
                settingsManager.resetCategory("visual");
                loadSettings(); // Refresh the UI
                SettingsDialogs.showInfoDialog(getContext(), "Reset Complete", 
                    "All visual settings have been reset to defaults.");
            });
    }
    
    @Override
    protected boolean validateSetting(String key, String value) {
        switch (key) {
            case Keys.UI_THEME:
                return "dark".equals(value) || "light".equals(value) || "auto".equals(value);
            case Keys.PERFORMANCE_MODE:
                return "high_quality".equals(value) || "balanced".equals(value) || "performance".equals(value);
            default:
                return super.validateSetting(key, value);
        }
    }
    
    @Override
    protected void onSettingChangedCustom(String key, Object value) {
        android.util.Log.d("VisualSettingsFragment", "onSettingChangedCustom called - Key: " + key + ", Value: " + value);
        
        // Mark visual settings as changed so MainActivity can apply them on resume
        switch (key) {
            case Keys.FULL_BODY_VIEW:
                android.util.Log.d("VisualSettingsFragment", "Marking visual settings as changed for key: " + key);
                settingsManager.markVisualSettingsChanged();
                break;
                
            case Keys.FULLSCREEN_MODE:
            case Keys.SHOW_STATUS_TEXT:
            case Keys.PERFORMANCE_MODE:
            case Keys.ANIMATION_SPEED:
            case Keys.TOUCH_SENSITIVITY:
            case Keys.UI_THEME:
                android.util.Log.d("VisualSettingsFragment", "Marking visual settings as changed for key: " + key);
                settingsManager.markVisualSettingsChanged();
                break;
                
            default:
                android.util.Log.d("VisualSettingsFragment", "Non-visual setting change: " + key);
                break;
        }
    }
}

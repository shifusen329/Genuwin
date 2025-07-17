package com.genuwin.app.settings;

/**
 * Data class representing a settings item
 */
public class SettingsItem {
    
    public enum Type {
        HEADER,
        SWITCH,
        EDIT_TEXT,
        MULTILINE_TEXT,
        SPINNER,
        SLIDER,
        BUTTON
    }
    
    public Type type;
    public String key;
    public String title;
    public String description;
    public Object value;
    
    // For spinner items
    public String[] options;
    
    // For slider items
    public float defaultFloatValue;
    public float minValue;
    public float maxValue;
    public float stepSize;
    
    // For multi-line text items
    public int maxCharacters;
    public String[] templates;
    
    public SettingsItem(Type type, String key, String title, String description, Object value) {
        this.type = type;
        this.key = key;
        this.title = title;
        this.description = description;
        this.value = value;
    }
    
    public boolean getBooleanValue() {
        return value instanceof Boolean ? (Boolean) value : false;
    }
    
    public String getStringValue() {
        return value != null ? value.toString() : "";
    }
    
    public float getFloatValue() {
        if (value instanceof Float) {
            return (Float) value;
        } else if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return 0.0f;
    }
    
    public int getIntValue() {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
}

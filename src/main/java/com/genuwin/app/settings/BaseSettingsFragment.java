package com.genuwin.app.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.genuwin.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for settings fragments
 */
public abstract class BaseSettingsFragment extends Fragment implements SettingsManager.SettingsChangeListener {
    
    protected SettingsManager settingsManager;
    protected RecyclerView recyclerView;
    protected SettingsItemAdapter adapter;
    protected List<SettingsItem> settingsItems;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsManager = SettingsManager.getInstance(requireContext());
        settingsManager.addSettingsChangeListener(this);
        settingsItems = new ArrayList<>();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        recyclerView = view.findViewById(R.id.settings_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new SettingsItemAdapter(settingsItems, this::onSettingItemClick);
        recyclerView.setAdapter(adapter);
        
        loadSettings();
        
        return view;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (settingsManager != null) {
            settingsManager.removeSettingsChangeListener(this);
        }
    }
    
    /**
     * Load settings items for this fragment
     */
    protected abstract void loadSettings();
    
    /**
     * Handle setting item clicks
     */
    protected void onSettingItemClick(SettingsItem item) {
        switch (item.type) {
            case SWITCH:
                handleSwitchClick(item);
                break;
            case EDIT_TEXT:
                handleEditTextClick(item);
                break;
            case MULTILINE_TEXT:
                handleMultiLineTextClick(item);
                break;
            case SPINNER:
                handleSpinnerClick(item);
                break;
            case SLIDER:
                handleSliderClick(item);
                break;
            case BUTTON:
                handleButtonClick(item);
                break;
        }
    }
    
    protected void handleSwitchClick(SettingsItem item) {
        boolean currentValue = settingsManager.getBoolean(item.key, false);
        settingsManager.setBoolean(item.key, !currentValue);
        item.value = !currentValue;
        adapter.notifyDataSetChanged();
    }
    
    protected void handleEditTextClick(SettingsItem item) {
        // Show edit text dialog
        SettingsDialogs.showEditTextDialog(getContext(), item.title, 
            settingsManager.getString(item.key, ""), 
            (newValue) -> {
                if (validateSetting(item.key, newValue)) {
                    settingsManager.setString(item.key, newValue);
                    item.value = newValue;
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "Invalid value", Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    protected void handleMultiLineTextClick(SettingsItem item) {
        // Show multi-line text dialog with templates and character count
        SettingsDialogs.showMultiLineTextDialog(getContext(), item.title, 
            settingsManager.getString(item.key, ""), 
            item.maxCharacters, item.templates,
            (newValue) -> {
                if (validateSetting(item.key, newValue)) {
                    settingsManager.setString(item.key, newValue);
                    item.value = newValue;
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "Invalid value", Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    protected void handleSpinnerClick(SettingsItem item) {
        if (item.options != null && item.options.length > 0) {
            String currentValue = settingsManager.getString(item.key, item.options[0]);
            SettingsDialogs.showSpinnerDialog(getContext(), item.title, item.options, currentValue,
                (selectedValue) -> {
                    settingsManager.setString(item.key, selectedValue);
                    item.value = selectedValue;
                    adapter.notifyDataSetChanged();
                });
        }
    }
    
    protected void handleSliderClick(SettingsItem item) {
        float currentValue = settingsManager.getFloat(item.key, item.defaultFloatValue);
        SettingsDialogs.showSliderDialog(getContext(), item.title, currentValue, 
            item.minValue, item.maxValue, item.stepSize,
            (newValue) -> {
                settingsManager.setFloat(item.key, newValue);
                item.value = newValue;
                adapter.notifyDataSetChanged();
            });
    }
    
    protected void handleButtonClick(SettingsItem item) {
        // Override in subclasses for custom button actions
    }
    
    /**
     * Validate setting values
     */
    protected boolean validateSetting(String key, String value) {
        switch (key) {
            case SettingsManager.Keys.OLLAMA_BASE_URL:
            case SettingsManager.Keys.TTS_BASE_URL:
            case SettingsManager.Keys.STT_BASE_URL:
            case SettingsManager.Keys.HOME_ASSISTANT_URL:
                return settingsManager.isValidUrl(value);
            default:
                return true;
        }
    }
    
    /**
     * Create a settings item
     */
    protected SettingsItem createSwitchItem(String key, String title, String description, boolean defaultValue) {
        boolean currentValue = settingsManager.getBoolean(key, defaultValue);
        return new SettingsItem(SettingsItem.Type.SWITCH, key, title, description, currentValue);
    }
    
    protected SettingsItem createEditTextItem(String key, String title, String description, String defaultValue) {
        String currentValue = settingsManager.getString(key, defaultValue);
        return new SettingsItem(SettingsItem.Type.EDIT_TEXT, key, title, description, currentValue);
    }
    
    protected SettingsItem createMultiLineTextItem(String key, String title, String description, 
                                                 String defaultValue, int maxCharacters, String[] templates) {
        String currentValue = settingsManager.getString(key, defaultValue);
        SettingsItem item = new SettingsItem(SettingsItem.Type.MULTILINE_TEXT, key, title, description, currentValue);
        item.maxCharacters = maxCharacters;
        item.templates = templates;
        return item;
    }
    
    protected SettingsItem createSpinnerItem(String key, String title, String description, 
                                           String[] options, String defaultValue) {
        String currentValue = settingsManager.getString(key, defaultValue);
        SettingsItem item = new SettingsItem(SettingsItem.Type.SPINNER, key, title, description, currentValue);
        item.options = options;
        return item;
    }
    
    protected SettingsItem createSliderItem(String key, String title, String description, 
                                          float defaultValue, float minValue, float maxValue, float stepSize) {
        float currentValue = settingsManager.getFloat(key, defaultValue);
        SettingsItem item = new SettingsItem(SettingsItem.Type.SLIDER, key, title, description, currentValue);
        item.defaultFloatValue = defaultValue;
        item.minValue = minValue;
        item.maxValue = maxValue;
        item.stepSize = stepSize;
        return item;
    }
    
    protected SettingsItem createButtonItem(String key, String title, String description) {
        return new SettingsItem(SettingsItem.Type.BUTTON, key, title, description, "");
    }
    
    protected SettingsItem createTextItem(String key, String title, String value, String description) {
        return new SettingsItem(SettingsItem.Type.EDIT_TEXT, key, title, description, value);
    }
    
    protected SettingsItem createHeaderItem(String title) {
        return new SettingsItem(SettingsItem.Type.HEADER, "", title, "", null);
    }
    
    @Override
    public void onSettingChanged(String key, Object value) {
        // Update UI when settings change
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Find and update the corresponding item
                for (SettingsItem item : settingsItems) {
                    if (item.key.equals(key)) {
                        item.value = value;
                        adapter.notifyDataSetChanged();
                        break;
                    }
                }
                
                // Call subclass implementation for custom handling
                onSettingChangedCustom(key, value);
            });
        }
    }
    
    /**
     * Override this method in subclasses for custom setting change handling
     */
    protected void onSettingChangedCustom(String key, Object value) {
        // Default implementation does nothing
        // Subclasses can override this for custom behavior
    }
}

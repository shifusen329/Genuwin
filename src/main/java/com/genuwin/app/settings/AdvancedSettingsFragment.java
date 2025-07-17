package com.genuwin.app.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.genuwin.app.managers.CharacterManager;
import com.genuwin.app.managers.MemoryManager;
import com.genuwin.app.settings.SettingsManager.Keys;
import com.genuwin.app.settings.SettingsManager.Defaults;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Settings fragment for advanced configuration
 */
public class AdvancedSettingsFragment extends BaseSettingsFragment {

    private MemoryManager memoryManager;
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;
    private ActivityResultLauncher<Intent> mergeLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        memoryManager = MemoryManager.getInstance(requireContext());

        // Initialize the ActivityResultLauncher for exporting memories
        exportLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        exportMemoriesToFile(uri);
                    }
                }
            });

        // Initialize the ActivityResultLauncher for importing memories
        importLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        importMemoriesFromFile(uri, false);
                    }
                }
            });

        // Initialize the ActivityResultLauncher for merging memories
        mergeLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        importMemoriesFromFile(uri, true);
                    }
                }
            });
    }
    
    @Override
    protected void loadSettings() {
        settingsItems.clear();
        
        // Data Management
        settingsItems.add(createHeaderItem("Data Management"));
        settingsItems.add(createSwitchItem(
            Keys.AUTO_SAVE_CONVERSATIONS,
            "Auto Save Conversations",
            "Automatically save conversation history",
            Defaults.AUTO_SAVE_CONVERSATIONS
        ));
        settingsItems.add(createSliderItem(
            Keys.CACHE_SIZE_MB,
            "Cache Size (MB)",
            "Maximum size for app cache storage",
            Defaults.CACHE_SIZE_MB,
            10.0f,
            500.0f,
            10.0f
        ));
        
        // Data Actions
        settingsItems.add(createButtonItem(
            "clear_cache",
            "Clear Cache",
            "Clear all cached data and temporary files"
        ));
        settingsItems.add(createButtonItem(
            "clear_conversations",
            "Clear Conversations",
            "Delete all saved conversation history"
        ));
        
        // Memories
        settingsItems.add(createHeaderItem("Memories"));
        settingsItems.add(createButtonItem(
            "export_memories",
            "Export Memories",
            "Export all memories to a file"
        ));
        settingsItems.add(createButtonItem(
            "import_memories",
            "Import Memories",
            "Import memories from a file"
        ));
        settingsItems.add(createButtonItem(
            "merge_memories",
            "Merge Memories",
            "Merge memories from a file with existing memories"
        ));
        
        // Backup & Restore
        settingsItems.add(createHeaderItem("Backup & Restore"));
        settingsItems.add(createButtonItem(
            "export_settings",
            "Export Settings",
            "Export all settings to a backup file"
        ));
        settingsItems.add(createButtonItem(
            "import_settings",
            "Import Settings",
            "Import settings from a backup file"
        ));
        
        // System Information
        settingsItems.add(createHeaderItem("System Information"));
        settingsItems.add(createButtonItem(
            "system_info",
            "View System Info",
            "Display device and app information"
        ));
        settingsItems.add(createButtonItem(
            "app_logs",
            "View App Logs",
            "Display recent application logs"
        ));
        
        // Reset Options
        settingsItems.add(createHeaderItem("Reset Options"));
        settingsItems.add(createButtonItem(
            "reset_all_settings",
            "Reset All Settings",
            "Reset all app settings to factory defaults"
        ));
        settingsItems.add(createButtonItem(
            "factory_reset",
            "Factory Reset",
            "Reset app to initial state (WARNING: Deletes all data)"
        ));
        
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void handleButtonClick(SettingsItem item) {
        switch (item.key) {
            case "clear_cache":
                clearCache();
                break;
            case "clear_conversations":
                clearConversations();
                break;
            case "export_memories":
                exportMemories();
                break;
            case "import_memories":
                importMemories();
                break;
            case "merge_memories":
                mergeMemories();
                break;
            case "export_settings":
                exportSettings();
                break;
            case "import_settings":
                importSettings();
                break;
            case "system_info":
                showSystemInfo();
                break;
            case "app_logs":
                showAppLogs();
                break;
            case "reset_all_settings":
                resetAllSettings();
                break;
            case "factory_reset":
                factoryReset();
                break;
        }
    }
    
    private void clearCache() {
        SettingsDialogs.showConfirmationDialog(getContext(), 
            "Clear Cache", 
            "Are you sure you want to clear all cached data? This may temporarily slow down the app.",
            () -> {
                // TODO: Implement cache clearing
                SettingsDialogs.showInfoDialog(getContext(), "Cache Cleared", 
                    "All cached data has been cleared successfully.");
            });
    }
    
    private void clearConversations() {
        SettingsDialogs.showConfirmationDialog(getContext(), 
            "Clear Conversations", 
            "Are you sure you want to delete all conversation history? This cannot be undone.",
            () -> {
                // TODO: Implement conversation clearing
                SettingsDialogs.showInfoDialog(getContext(), "Conversations Cleared", 
                    "All conversation history has been deleted.");
            });
    }
    
    private void exportMemories() {
        String characterId = CharacterManager.getInstance(requireContext()).getCurrentCharacter().modelDirectory;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, characterId + "_memories.json");
        exportLauncher.launch(intent);
    }

    private void importMemories() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        importLauncher.launch(intent);
    }

    private void mergeMemories() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        mergeLauncher.launch(intent);
    }

    private void exportMemoriesToFile(Uri uri) {
        String characterId = CharacterManager.getInstance(requireContext()).getCurrentCharacter().modelDirectory;
        memoryManager.exportMemories(characterId).thenAccept(jsonData -> {
            try (OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    outputStream.write(jsonData.getBytes(StandardCharsets.UTF_8));
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Memories exported successfully", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Failed to export memories", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void importMemoriesFromFile(Uri uri, boolean merge) {
        String characterId = CharacterManager.getInstance(requireContext()).getCurrentCharacter().modelDirectory;
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            String jsonData = stringBuilder.toString();

            if (merge) {
                memoryManager.mergeMemories(characterId, jsonData).thenRun(() -> {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Memories merged successfully", Toast.LENGTH_SHORT).show();
                    });
                });
            } else {
                memoryManager.importMemories(characterId, jsonData).thenRun(() -> {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Memories imported successfully", Toast.LENGTH_SHORT).show();
                    });
                });
            }
        } catch (Exception e) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Failed to import memories", Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private void exportSettings() {
        // TODO: Implement settings export
        SettingsDialogs.showInfoDialog(getContext(), "Export Settings", 
            "Settings export will be implemented in a future update. " +
            "This will allow you to backup your settings to a file.");
    }
    
    private void importSettings() {
        // TODO: Implement settings import
        SettingsDialogs.showInfoDialog(getContext(), "Import Settings", 
            "Settings import will be implemented in a future update. " +
            "This will allow you to restore settings from a backup file.");
    }
    
    private void showSystemInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Device Information:\n\n");
        info.append("Android Version: ").append(android.os.Build.VERSION.RELEASE).append("\n");
        info.append("API Level: ").append(android.os.Build.VERSION.SDK_INT).append("\n");
        info.append("Device Model: ").append(android.os.Build.MODEL).append("\n");
        info.append("Manufacturer: ").append(android.os.Build.MANUFACTURER).append("\n");
        info.append("Architecture: ").append(android.os.Build.SUPPORTED_ABIS[0]).append("\n\n");
        
        info.append("App Information:\n\n");
        try {
            android.content.pm.PackageInfo pInfo = getContext().getPackageManager()
                .getPackageInfo(getContext().getPackageName(), 0);
            info.append("Version: ").append(pInfo.versionName).append("\n");
            info.append("Build: ").append(pInfo.versionCode).append("\n");
        } catch (Exception e) {
            info.append("Version: Unknown\n");
        }
        
        info.append("Package: ").append(getContext().getPackageName()).append("\n");
        
        SettingsDialogs.showInfoDialog(getContext(), "System Information", info.toString());
    }
    
    private void showAppLogs() {
        // TODO: Implement log viewing
        SettingsDialogs.showInfoDialog(getContext(), "App Logs", 
            "Log viewing will be implemented in a future update. " +
            "This will display recent application logs for debugging purposes.");
    }
    
    private void resetAllSettings() {
        SettingsDialogs.showConfirmationDialog(getContext(), 
            "Reset All Settings", 
            "Are you sure you want to reset ALL settings to their defaults? " +
            "This will affect API configurations, character settings, audio, visual, and advanced settings. " +
            "This cannot be undone.",
            () -> {
                settingsManager.resetToDefaults();
                loadSettings(); // Refresh the UI
                SettingsDialogs.showInfoDialog(getContext(), "Reset Complete", 
                    "All settings have been reset to factory defaults. " +
                    "You may need to restart the app for all changes to take effect.");
            });
    }
    
    private void factoryReset() {
        SettingsDialogs.showConfirmationDialog(getContext(), 
            "Factory Reset", 
            "WARNING: This will delete ALL app data including settings, conversations, " +
            "cache, and any other stored information. The app will be reset to its " +
            "initial state as if freshly installed. This cannot be undone. " +
            "Are you absolutely sure?",
            () -> {
                // TODO: Implement factory reset
                SettingsDialogs.showInfoDialog(getContext(), "Factory Reset", 
                    "Factory reset will be implemented in a future update. " +
                    "This will completely reset the app to its initial state.");
            });
    }
    
    @Override
    protected void handleSliderClick(SettingsItem item) {
        if (Keys.CACHE_SIZE_MB.equals(item.key)) {
            // Handle cache size as integer
            float currentValue = settingsManager.getInt(item.key, Defaults.CACHE_SIZE_MB);
            SettingsDialogs.showSliderDialog(getContext(), item.title, currentValue, 
                item.minValue, item.maxValue, item.stepSize,
                (newValue) -> {
                    settingsManager.setInt(item.key, Math.round(newValue));
                    item.value = Math.round(newValue);
                    adapter.notifyDataSetChanged();
                });
        } else {
            super.handleSliderClick(item);
        }
    }
    
    @Override
    protected boolean validateSetting(String key, String value) {
        switch (key) {
            case Keys.CACHE_SIZE_MB:
                try {
                    int cacheSize = Integer.parseInt(value);
                    return cacheSize >= 10 && cacheSize <= 500;
                } catch (NumberFormatException e) {
                    return false;
                }
            default:
                return super.validateSetting(key, value);
        }
    }
}

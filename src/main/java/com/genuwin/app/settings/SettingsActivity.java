package com.genuwin.app.settings;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.genuwin.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Main settings activity with categorized settings options
 */
public class SettingsActivity extends AppCompatActivity implements SettingsManager.SettingsChangeListener {
    private static final String TAG = "SettingsActivity";
    
    private SettingsManager settingsManager;
    private RecyclerView recyclerView;
    private SettingsCategoryAdapter adapter;
    private List<SettingsCategory> categories;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        settingsManager = SettingsManager.getInstance(this);
        settingsManager.addSettingsChangeListener(this);
        
        setupToolbar();
        setupRecyclerView();
        loadCategories();

        // Show help dialog on first launch
        if (isFirstLaunch()) {
            SettingsDialogs.showHelpDialog(this);
            markFirstLaunchDone();
        }
    }

    private boolean isFirstLaunch() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        return prefs.getBoolean("is_first_launch", true);
    }

    private void markFirstLaunchDone() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("is_first_launch", false).apply();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }
    }
    
    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.settings_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        categories = new ArrayList<>();
        adapter = new SettingsCategoryAdapter(categories, this::onCategoryClick);
        recyclerView.setAdapter(adapter);
    }
    
    private void loadCategories() {
        categories.clear();
        
        // API Configuration
        categories.add(new SettingsCategory(
            "api",
            "API Configuration",
            "Configure AI models and external services",
            R.drawable.ic_api,
            ApiSettingsFragment.class
        ));
        
        // Character Settings
        categories.add(new SettingsCategory(
            "character",
            "Character Settings",
            "Manage characters and personalities",
            R.drawable.ic_character,
            CharacterSettingsFragment.class
        ));
        
        // Audio Settings
        categories.add(new SettingsCategory(
            "audio",
            "Audio Settings",
            "Voice recognition and audio preferences",
            R.drawable.ic_audio,
            AudioSettingsFragment.class
        ));
        
        // Visual Settings
        categories.add(new SettingsCategory(
            "visual",
            "Visual Settings",
            "Display and animation preferences",
            R.drawable.ic_visual,
            VisualSettingsFragment.class
        ));
        
        // Advanced Settings
        categories.add(new SettingsCategory(
            "advanced",
            "Advanced Settings",
            "Debug options and performance settings",
            R.drawable.ic_advanced,
            AdvancedSettingsFragment.class
        ));
        
        adapter.notifyDataSetChanged();
    }
    
    private void onCategoryClick(SettingsCategory category) {
        try {
            // Create and show the settings fragment
            BaseSettingsFragment fragment = (BaseSettingsFragment) category.fragmentClass.newInstance();
            
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, fragment)
                .addToBackStack(category.id)
                .commit();
            
            // Hide the category list and show the fragment container
            recyclerView.setVisibility(View.GONE);
            findViewById(R.id.settings_container).setVisibility(View.VISIBLE);
            
            // Update toolbar title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(category.title);
            }
            
        } catch (Exception e) {
            Toast.makeText(this, "Error opening settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            // Pop the fragment and show category list
            getSupportFragmentManager().popBackStack();
            recyclerView.setVisibility(View.VISIBLE);
            findViewById(R.id.settings_container).setVisibility(View.GONE);
            
            // Reset toolbar title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Settings");
            }
        } else {
            // Set a result to indicate that the main activity should be recreated
            setResult(RESULT_OK);
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (settingsManager != null) {
            settingsManager.removeSettingsChangeListener(this);
        }
    }
    
    @Override
    public void onSettingChanged(String key, Object value) {
        // Handle global settings changes if needed
        runOnUiThread(() -> {
            // Update UI based on settings changes
            if (key.equals("reset")) {
                Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    // Settings category data class
    public static class SettingsCategory {
        public final String id;
        public final String title;
        public final String description;
        public final int iconResId;
        public final Class<? extends BaseSettingsFragment> fragmentClass;
        
        public SettingsCategory(String id, String title, String description, int iconResId, 
                              Class<? extends BaseSettingsFragment> fragmentClass) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.iconResId = iconResId;
            this.fragmentClass = fragmentClass;
        }
    }
}

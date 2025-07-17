package com.genuwin.app.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.genuwin.app.R;

import java.util.List;

/**
 * RecyclerView adapter for settings categories
 */
public class SettingsCategoryAdapter extends RecyclerView.Adapter<SettingsCategoryAdapter.CategoryViewHolder> {
    
    private List<SettingsActivity.SettingsCategory> categories;
    private OnCategoryClickListener clickListener;
    
    public interface OnCategoryClickListener {
        void onCategoryClick(SettingsActivity.SettingsCategory category);
    }
    
    public SettingsCategoryAdapter(List<SettingsActivity.SettingsCategory> categories, 
                                 OnCategoryClickListener clickListener) {
        this.categories = categories;
        this.clickListener = clickListener;
    }
    
    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_settings_category, parent, false);
        return new CategoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        SettingsActivity.SettingsCategory category = categories.get(position);
        holder.bind(category, clickListener);
    }
    
    @Override
    public int getItemCount() {
        return categories.size();
    }
    
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconView;
        private TextView titleView;
        private TextView descriptionView;
        
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.category_icon);
            titleView = itemView.findViewById(R.id.category_title);
            descriptionView = itemView.findViewById(R.id.category_description);
        }
        
        public void bind(SettingsActivity.SettingsCategory category, OnCategoryClickListener clickListener) {
            iconView.setImageResource(category.iconResId);
            titleView.setText(category.title);
            descriptionView.setText(category.description);
            
            // Set different colors for different categories
            int iconColor = getIconColor(category.id);
            iconView.setColorFilter(iconColor);
            
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onCategoryClick(category);
                }
            });
        }
        
        private int getIconColor(String categoryId) {
            switch (categoryId) {
                case "api":
                    return 0xFF4CAF50; // Green
                case "character":
                    return 0xFF2196F3; // Blue
                case "audio":
                    return 0xFFFF9800; // Orange
                case "visual":
                    return 0xFF9C27B0; // Purple
                case "advanced":
                    return 0xFFF44336; // Red
                default:
                    return 0xFF757575; // Gray
            }
        }
    }
}

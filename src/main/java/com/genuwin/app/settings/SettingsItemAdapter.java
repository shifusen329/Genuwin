package com.genuwin.app.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.genuwin.app.R;

import java.util.List;

/**
 * RecyclerView adapter for settings items
 */
public class SettingsItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SWITCH = 1;
    private static final int TYPE_EDIT_TEXT = 2;
    private static final int TYPE_MULTILINE_TEXT = 3;
    private static final int TYPE_SPINNER = 4;
    private static final int TYPE_SLIDER = 5;
    private static final int TYPE_BUTTON = 6;
    
    private List<SettingsItem> items;
    private OnSettingItemClickListener clickListener;
    
    public interface OnSettingItemClickListener {
        void onSettingItemClick(SettingsItem item);
    }
    
    public SettingsItemAdapter(List<SettingsItem> items, OnSettingItemClickListener clickListener) {
        this.items = items;
        this.clickListener = clickListener;
    }
    
    @Override
    public int getItemViewType(int position) {
        switch (items.get(position).type) {
            case HEADER:
                return TYPE_HEADER;
            case SWITCH:
                return TYPE_SWITCH;
            case EDIT_TEXT:
                return TYPE_EDIT_TEXT;
            case MULTILINE_TEXT:
                return TYPE_MULTILINE_TEXT;
            case SPINNER:
                return TYPE_SPINNER;
            case SLIDER:
                return TYPE_SLIDER;
            case BUTTON:
                return TYPE_BUTTON;
            default:
                return TYPE_EDIT_TEXT;
        }
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        switch (viewType) {
            case TYPE_HEADER:
                return new HeaderViewHolder(inflater.inflate(R.layout.item_settings_header, parent, false));
            case TYPE_SWITCH:
                return new SwitchViewHolder(inflater.inflate(R.layout.item_settings_switch, parent, false));
            case TYPE_EDIT_TEXT:
            case TYPE_MULTILINE_TEXT:
            case TYPE_SPINNER:
            case TYPE_SLIDER:
                return new TextViewHolder(inflater.inflate(R.layout.item_settings_text, parent, false));
            case TYPE_BUTTON:
                return new ButtonViewHolder(inflater.inflate(R.layout.item_settings_button, parent, false));
            default:
                return new TextViewHolder(inflater.inflate(R.layout.item_settings_text, parent, false));
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SettingsItem item = items.get(position);
        
        switch (holder.getItemViewType()) {
            case TYPE_HEADER:
                ((HeaderViewHolder) holder).bind(item);
                break;
            case TYPE_SWITCH:
                ((SwitchViewHolder) holder).bind(item, clickListener);
                break;
            case TYPE_EDIT_TEXT:
            case TYPE_MULTILINE_TEXT:
            case TYPE_SPINNER:
            case TYPE_SLIDER:
                ((TextViewHolder) holder).bind(item, clickListener);
                break;
            case TYPE_BUTTON:
                ((ButtonViewHolder) holder).bind(item, clickListener);
                break;
        }
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    // Header ViewHolder
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView titleView;
        
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.header_title);
        }
        
        public void bind(SettingsItem item) {
            titleView.setText(item.title);
        }
    }
    
    // Switch ViewHolder
    static class SwitchViewHolder extends RecyclerView.ViewHolder {
        private TextView titleView;
        private TextView descriptionView;
        private Switch switchView;
        
        public SwitchViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.setting_title);
            descriptionView = itemView.findViewById(R.id.setting_description);
            switchView = itemView.findViewById(R.id.setting_switch);
        }
        
        public void bind(SettingsItem item, OnSettingItemClickListener clickListener) {
            titleView.setText(item.title);
            descriptionView.setText(item.description);
            
            // Set switch state without triggering listener
            switchView.setOnCheckedChangeListener(null);
            switchView.setChecked(item.getBooleanValue());
            
            // Set click listeners
            switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (clickListener != null) {
                    clickListener.onSettingItemClick(item);
                }
            });
            
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onSettingItemClick(item);
                }
            });
        }
    }
    
    // Text ViewHolder (for edit text, spinner, slider)
    static class TextViewHolder extends RecyclerView.ViewHolder {
        private TextView titleView;
        private TextView descriptionView;
        private TextView valueView;
        
        public TextViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.setting_title);
            descriptionView = itemView.findViewById(R.id.setting_description);
            valueView = itemView.findViewById(R.id.setting_value);
        }
        
        public void bind(SettingsItem item, OnSettingItemClickListener clickListener) {
            titleView.setText(item.title);
            descriptionView.setText(item.description);
            
            // Format value based on type
            String valueText = "";
            switch (item.type) {
                case EDIT_TEXT:
                case SPINNER:
                    valueText = item.getStringValue();
                    break;
                case MULTILINE_TEXT:
                    // Show preview of multi-line text (first 100 characters)
                    String fullText = item.getStringValue();
                    if (fullText.length() > 100) {
                        valueText = fullText.substring(0, 100) + "...";
                    } else {
                        valueText = fullText;
                    }
                    // Replace newlines with spaces for preview
                    valueText = valueText.replace("\n", " ").replace("\r", " ");
                    break;
                case SLIDER:
                    valueText = String.format("%.2f", item.getFloatValue());
                    break;
            }
            valueView.setText(valueText);
            
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onSettingItemClick(item);
                }
            });
        }
    }
    
    // Button ViewHolder
    static class ButtonViewHolder extends RecyclerView.ViewHolder {
        private TextView titleView;
        private TextView descriptionView;
        
        public ButtonViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.button_title);
            descriptionView = itemView.findViewById(R.id.button_description);
        }
        
        public void bind(SettingsItem item, OnSettingItemClickListener clickListener) {
            titleView.setText(item.title);
            descriptionView.setText(item.description);
            
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onSettingItemClick(item);
                }
            });
        }
    }
}

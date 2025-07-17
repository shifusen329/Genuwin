package com.genuwin.app.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Utility class for showing settings dialogs
 */
public class SettingsDialogs {
    
    public interface OnValueSelectedListener<T> {
        void onValueSelected(T value);
    }
    
    /**
     * Show edit text dialog
     */
    public static void showEditTextDialog(Context context, String title, String currentValue, 
                                        OnValueSelectedListener<String> listener) {
        EditText editText = new EditText(context);
        editText.setText(currentValue);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setTextColor(0xFFFFFFFF);
        editText.setHintTextColor(0xFFCCCCCC);
        
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(editText)
                .setPositiveButton("OK", (dialog, which) -> {
                    String newValue = editText.getText().toString().trim();
                    if (listener != null) {
                        listener.onValueSelected(newValue);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Show multi-line text editor dialog with character count and validation
     */
    public static void showMultiLineTextDialog(Context context, String title, String currentValue, 
                                             int maxCharacters, String[] templates,
                                             OnValueSelectedListener<String> listener) {
        // Create custom layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);
        
        // Add template selection if templates are provided
        if (templates != null && templates.length > 0) {
            TextView templateLabel = new TextView(context);
            templateLabel.setText("Templates:");
            templateLabel.setTextColor(0xFFFFFFFF);
            templateLabel.setTextSize(14);
            layout.addView(templateLabel);
            
            android.widget.Spinner templateSpinner = new android.widget.Spinner(context);
            String[] spinnerOptions = new String[templates.length + 1];
            spinnerOptions[0] = "Select a template...";
            System.arraycopy(templates, 0, spinnerOptions, 1, templates.length);
            
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                context, android.R.layout.simple_spinner_item, spinnerOptions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            templateSpinner.setAdapter(adapter);
            layout.addView(templateSpinner);
            
            // Add spacing
            android.view.View spacer1 = new android.view.View(context);
            spacer1.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 20));
            layout.addView(spacer1);
        }
        
        // Multi-line EditText
        EditText editText = new EditText(context);
        editText.setText(currentValue);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setLines(6);
        editText.setMaxLines(10);
        editText.setVerticalScrollBarEnabled(true);
        editText.setHorizontalScrollBarEnabled(false);
        editText.setTextColor(0xFFFFFFFF);
        editText.setHintTextColor(0xFFCCCCCC);
        editText.setHint("Enter custom personality prompt...");
        
        // Set max characters if specified
        if (maxCharacters > 0) {
            editText.setFilters(new android.text.InputFilter[]{
                new android.text.InputFilter.LengthFilter(maxCharacters)
            });
        }
        
        layout.addView(editText);
        
        // Character count display
        TextView charCountText = new TextView(context);
        charCountText.setTextColor(0xFFCCCCCC);
        charCountText.setTextSize(12);
        updateCharacterCount(charCountText, editText.getText().toString(), maxCharacters);
        layout.addView(charCountText);
        
        // Update character count as user types
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCharacterCount(charCountText, s.toString(), maxCharacters);
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        // Template selection handler
        if (templates != null && templates.length > 0) {
            android.widget.Spinner templateSpinner = (android.widget.Spinner) layout.getChildAt(1);
            templateSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                    if (position > 0) { // Skip "Select a template..." option
                        editText.setText(templates[position - 1]);
                        editText.setSelection(editText.getText().length()); // Move cursor to end
                    }
                }
                
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }
        
        // Add spacing
        android.view.View spacer2 = new android.view.View(context);
        spacer2.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 20));
        layout.addView(spacer2);
        
        // Preview button
        android.widget.Button previewButton = new android.widget.Button(context);
        previewButton.setText("Preview");
        previewButton.setOnClickListener(v -> {
            String previewText = editText.getText().toString().trim();
            if (!previewText.isEmpty()) {
                showInfoDialog(context, "Personality Preview", 
                    "Preview of personality:\n\n" + previewText);
            } else {
                showInfoDialog(context, "Preview", "No personality text to preview.");
            }
        });
        layout.addView(previewButton);
        
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("Save", (d, which) -> {
                    String newValue = editText.getText().toString().trim();
                    if (listener != null) {
                        listener.onValueSelected(newValue);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        
        dialog.show();
        
        // Make the dialog larger
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
    
    /**
     * Update character count display
     */
    private static void updateCharacterCount(TextView charCountText, String text, int maxCharacters) {
        int currentLength = text.length();
        if (maxCharacters > 0) {
            charCountText.setText(currentLength + " / " + maxCharacters + " characters");
            if (currentLength > maxCharacters * 0.9) {
                charCountText.setTextColor(0xFFFF6B6B); // Red when approaching limit
            } else if (currentLength > maxCharacters * 0.7) {
                charCountText.setTextColor(0xFFFFD93D); // Yellow when getting close
            } else {
                charCountText.setTextColor(0xFFCCCCCC); // Normal gray
            }
        } else {
            charCountText.setText(currentLength + " characters");
            charCountText.setTextColor(0xFFCCCCCC);
        }
    }
    
    /**
     * Show spinner dialog (single choice)
     */
    public static void showSpinnerDialog(Context context, String title, String[] options, 
                                       String currentValue, OnValueSelectedListener<String> listener) {
        int selectedIndex = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(currentValue)) {
                selectedIndex = i;
                break;
            }
        }
        
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setSingleChoiceItems(options, selectedIndex, null)
                .setPositiveButton("OK", (dialog, which) -> {
                    int selected = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (selected >= 0 && selected < options.length && listener != null) {
                        listener.onValueSelected(options[selected]);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Show slider dialog
     */
    public static void showSliderDialog(Context context, String title, float currentValue, 
                                      float minValue, float maxValue, float stepSize,
                                      OnValueSelectedListener<Float> listener) {
        // Create custom layout with SeekBar and TextView
        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);
        
        TextView valueText = new TextView(context);
        valueText.setText(String.format("%.2f", currentValue));
        valueText.setTextColor(0xFFFFFFFF);
        valueText.setTextSize(18);
        valueText.setGravity(android.view.Gravity.CENTER);
        layout.addView(valueText);
        
        SeekBar seekBar = new SeekBar(context);
        int steps = (int) ((maxValue - minValue) / stepSize);
        seekBar.setMax(steps);
        seekBar.setProgress((int) ((currentValue - minValue) / stepSize));
        
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = minValue + (progress * stepSize);
                valueText.setText(String.format("%.2f", value));
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        layout.addView(seekBar);
        
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("OK", (dialog, which) -> {
                    float value = minValue + (seekBar.getProgress() * stepSize);
                    if (listener != null) {
                        listener.onValueSelected(value);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Show number input dialog
     */
    public static void showNumberDialog(Context context, String title, int currentValue, 
                                      OnValueSelectedListener<Integer> listener) {
        EditText editText = new EditText(context);
        editText.setText(String.valueOf(currentValue));
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setTextColor(0xFFFFFFFF);
        editText.setHintTextColor(0xFFCCCCCC);
        
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(editText)
                .setPositiveButton("OK", (dialog, which) -> {
                    try {
                        int newValue = Integer.parseInt(editText.getText().toString().trim());
                        if (listener != null) {
                            listener.onValueSelected(newValue);
                        }
                    } catch (NumberFormatException e) {
                        // Invalid number, ignore
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Show confirmation dialog
     */
    public static void showConfirmationDialog(Context context, String title, String message, 
                                            Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (onConfirm != null) {
                        onConfirm.run();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
    
    /**
     * Show info dialog
     */
    public static void showInfoDialog(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Show first time setup help dialog
     */
    public static void showHelpDialog(Context context) {
        String helpMessage = "Welcome to Genuwin AI Waifu!\n\n" +
                "To get started, you'll need to configure a few settings:\n\n" +
                "1. API Provider: Choose between OpenAI and Ollama for the AI chat service.\n\n" +
                "2. API Keys: Enter your API keys for the selected services. You can get these from the respective service providers.\n\n" +
                "3. Server URLs: If you're running your own servers for Ollama, TTS, or STT, enter their URLs here.\n\n" +
                "4. Character: Choose your favorite character and customize their personality and greeting.\n\n" +
                "5. Audio: Configure your microphone and speaker settings for the best experience.\n\n" +
                "You can find all these settings in the app's settings menu. Enjoy your new AI companion!";

        showInfoDialog(context, "First Time Setup", helpMessage);
    }
}

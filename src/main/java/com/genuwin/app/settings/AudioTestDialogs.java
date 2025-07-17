package com.genuwin.app.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.genuwin.app.managers.AudioTestManager;

import java.util.Locale;

/**
 * Custom dialogs for audio testing functionality
 */
public class AudioTestDialogs {
    
    /**
     * Show microphone test dialog with real-time audio level visualization
     */
    public static void showMicrophoneTestDialog(Context context, AudioTestManager testManager) {
        // Create custom layout
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);
        
        // Title
        TextView titleText = new TextView(context);
        titleText.setText("Microphone Test");
        titleText.setTextColor(0xFFFFFFFF);
        titleText.setTextSize(18);
        titleText.setPadding(0, 0, 0, 20);
        layout.addView(titleText);
        
        // Instructions
        TextView instructionText = new TextView(context);
        instructionText.setText("Speak normally to test your microphone. The level meter will show your voice input.");
        instructionText.setTextColor(0xFFCCCCCC);
        instructionText.setTextSize(14);
        instructionText.setPadding(0, 0, 0, 20);
        layout.addView(instructionText);
        
        // Audio level meter
        AudioLevelMeter levelMeter = new AudioLevelMeter(context);
        levelMeter.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 100));
        layout.addView(levelMeter);
        
        // Level text display
        TextView levelText = new TextView(context);
        levelText.setText("Level: 0.0% | Peak: 0.0%");
        levelText.setTextColor(0xFFFFFFFF);
        levelText.setTextSize(14);
        levelText.setPadding(0, 10, 0, 20);
        layout.addView(levelText);
        
        // Status text
        TextView statusText = new TextView(context);
        statusText.setText("Ready to test");
        statusText.setTextColor(0xFF4CAF50);
        statusText.setTextSize(14);
        statusText.setPadding(0, 0, 0, 20);
        layout.addView(statusText);
        
        // Control buttons layout
        LinearLayout buttonLayout = new LinearLayout(context);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setPadding(0, 10, 0, 0);
        
        Button startTestButton = new Button(context);
        startTestButton.setText("Start Level Test");
        startTestButton.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        
        Button recordTestButton = new Button(context);
        recordTestButton.setText("Recording Test");
        recordTestButton.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        recordTestButton.setEnabled(false);
        
        buttonLayout.addView(startTestButton);
        buttonLayout.addView(recordTestButton);
        layout.addView(buttonLayout);
        
        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(layout)
                .setNegativeButton("Close", null)
                .create();
        
        // Test state tracking
        final boolean[] isLevelTestRunning = {false};
        final boolean[] isRecordTestRunning = {false};
        
        // Start level test button handler
        startTestButton.setOnClickListener(v -> {
            if (!isLevelTestRunning[0]) {
                // Start level test
                isLevelTestRunning[0] = true;
                startTestButton.setText("Stop Level Test");
                recordTestButton.setEnabled(false);
                statusText.setText("Testing microphone levels...");
                statusText.setTextColor(0xFFFF9800);
                
                testManager.startAudioLevelTest(new AudioTestManager.AudioLevelCallback() {
                    @Override
                    public void onLevelUpdate(float level, float peak) {
                        levelMeter.updateLevels(level, peak);
                        levelText.setText(String.format(Locale.US, "Level: %.1f%% | Peak: %.1f%%", 
                            level * 100, peak * 100));
                    }
                    
                    @Override
                    public void onTestStarted() {
                        statusText.setText("Speak into the microphone...");
                        statusText.setTextColor(0xFF4CAF50);
                    }
                    
                    @Override
                    public void onTestCompleted() {
                        isLevelTestRunning[0] = false;
                        startTestButton.setText("Start Level Test");
                        recordTestButton.setEnabled(true);
                        statusText.setText("Level test completed");
                        statusText.setTextColor(0xFF4CAF50);
                        levelMeter.updateLevels(0, 0);
                    }
                    
                    @Override
                    public void onTestError(String error) {
                        isLevelTestRunning[0] = false;
                        startTestButton.setText("Start Level Test");
                        recordTestButton.setEnabled(true);
                        statusText.setText("Error: " + error);
                        statusText.setTextColor(0xFFFF5722);
                        levelMeter.updateLevels(0, 0);
                    }
                });
            } else {
                // Stop level test
                testManager.stopAudioLevelTest();
                isLevelTestRunning[0] = false;
                startTestButton.setText("Start Level Test");
                recordTestButton.setEnabled(true);
                statusText.setText("Level test stopped");
                statusText.setTextColor(0xFFCCCCCC);
                levelMeter.updateLevels(0, 0);
            }
        });
        
        // Recording test button handler
        recordTestButton.setOnClickListener(v -> {
            if (!isRecordTestRunning[0]) {
                isRecordTestRunning[0] = true;
                recordTestButton.setText("Testing...");
                recordTestButton.setEnabled(false);
                startTestButton.setEnabled(false);
                statusText.setText("Recording test in progress...");
                statusText.setTextColor(0xFFFF9800);
                
                testManager.testMicrophoneRecording(new AudioTestManager.TestResultCallback() {
                    @Override
                    public void onTestStarted() {
                        statusText.setText("Say something for 3 seconds...");
                        statusText.setTextColor(0xFF4CAF50);
                    }
                    
                    @Override
                    public void onTestProgress(int progress) {
                        statusText.setText("Recording test: " + progress + "%");
                    }
                    
                    @Override
                    public void onTestCompleted(AudioTestManager.TestResult result) {
                        isRecordTestRunning[0] = false;
                        recordTestButton.setText("Recording Test");
                        recordTestButton.setEnabled(true);
                        startTestButton.setEnabled(true);
                        
                        // Show test results
                        showTestResultDialog(context, "Recording Test Results", result);
                    }
                    
                    @Override
                    public void onTestError(String error) {
                        isRecordTestRunning[0] = false;
                        recordTestButton.setText("Recording Test");
                        recordTestButton.setEnabled(true);
                        startTestButton.setEnabled(true);
                        statusText.setText("Recording test error: " + error);
                        statusText.setTextColor(0xFFFF5722);
                    }
                });
            }
        });
        
        // Cleanup when dialog is dismissed
        dialog.setOnDismissListener(d -> {
            if (isLevelTestRunning[0]) {
                testManager.stopAudioLevelTest();
            }
        });
        
        dialog.show();
        
        // Make dialog larger
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9),
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }
    }
    
    /**
     * Show auto-calibration dialog with progress
     */
    public static void showAutoCalibrationDialog(Context context, AudioTestManager testManager) {
        // Create custom layout
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);
        
        // Title
        TextView titleText = new TextView(context);
        titleText.setText("Auto Calibration");
        titleText.setTextColor(0xFFFFFFFF);
        titleText.setTextSize(18);
        titleText.setPadding(0, 0, 0, 20);
        layout.addView(titleText);
        
        // Instructions
        TextView instructionText = new TextView(context);
        instructionText.setText("This will automatically adjust your microphone settings based on your environment. Please remain quiet during the calibration process.");
        instructionText.setTextColor(0xFFCCCCCC);
        instructionText.setTextSize(14);
        instructionText.setPadding(0, 0, 0, 20);
        layout.addView(instructionText);
        
        // Progress bar
        ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(progressBar);
        
        // Status text
        TextView statusText = new TextView(context);
        statusText.setText("Ready to calibrate");
        statusText.setTextColor(0xFF4CAF50);
        statusText.setTextSize(14);
        statusText.setPadding(0, 10, 0, 20);
        layout.addView(statusText);
        
        // Control buttons
        LinearLayout buttonLayout = new LinearLayout(context);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button startButton = new Button(context);
        startButton.setText("Start Calibration");
        startButton.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        
        buttonLayout.addView(startButton);
        layout.addView(buttonLayout);
        
        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .create();
        
        // Calibration state
        final boolean[] isCalibrating = {false};
        
        startButton.setOnClickListener(v -> {
            if (!isCalibrating[0]) {
                isCalibrating[0] = true;
                startButton.setText("Calibrating...");
                startButton.setEnabled(false);
                
                testManager.performAutoCalibration(new AudioTestManager.CalibrationCallback() {
                    @Override
                    public void onCalibrationStarted() {
                        statusText.setText("Calibration started");
                        statusText.setTextColor(0xFF4CAF50);
                    }
                    
                    @Override
                    public void onCalibrationProgress(int progress, String status) {
                        progressBar.setProgress(progress);
                        statusText.setText(status);
                    }
                    
                    @Override
                    public void onCalibrationCompleted(AudioTestManager.CalibrationResult result) {
                        isCalibrating[0] = false;
                        progressBar.setProgress(100);
                        
                        if (result.successful) {
                            statusText.setText("Calibration completed successfully!");
                            statusText.setTextColor(0xFF4CAF50);
                            
                            // Show results and ask to apply
                            showCalibrationResultDialog(context, result, testManager);
                            dialog.dismiss();
                        } else {
                            statusText.setText("Calibration failed: " + result.message);
                            statusText.setTextColor(0xFFFF5722);
                            startButton.setText("Retry Calibration");
                            startButton.setEnabled(true);
                        }
                    }
                    
                    @Override
                    public void onCalibrationError(String error) {
                        isCalibrating[0] = false;
                        statusText.setText("Error: " + error);
                        statusText.setTextColor(0xFFFF5722);
                        startButton.setText("Retry Calibration");
                        startButton.setEnabled(true);
                        progressBar.setProgress(0);
                    }
                });
            }
        });
        
        dialog.show();
        
        // Make dialog larger
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85),
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }
    }
    
    /**
     * Show test result dialog
     */
    private static void showTestResultDialog(Context context, String title, AudioTestManager.TestResult result) {
        StringBuilder message = new StringBuilder();
        message.append("Test Result: ").append(result.passed ? "PASSED" : "FAILED").append("\n\n");
        message.append(result.message).append("\n\n");
        
        if (result.averageLevel > 0) {
            message.append(String.format(Locale.US, "Average Level: %.1f%%\n", result.averageLevel * 100));
            message.append(String.format(Locale.US, "Peak Level: %.1f%%\n", result.peakLevel * 100));
            message.append(String.format(Locale.US, "Noise Floor: %.1f%%\n\n", result.noiseFloor * 100));
        }
        
        if (!result.recommendations.isEmpty()) {
            message.append("Recommendations:\n");
            for (String recommendation : result.recommendations) {
                message.append("• ").append(recommendation).append("\n");
            }
        }
        
        SettingsDialogs.showInfoDialog(context, title, message.toString());
    }
    
    /**
     * Show calibration result dialog with option to apply settings
     */
    private static void showCalibrationResultDialog(Context context, AudioTestManager.CalibrationResult result, 
                                                   AudioTestManager testManager) {
        StringBuilder message = new StringBuilder();
        message.append(result.message).append("\n\n");
        
        message.append("Recommended Settings:\n");
        message.append(String.format(Locale.US, "• Microphone Sensitivity: %.1f\n", result.optimalSensitivity));
        message.append(String.format(Locale.US, "• VAD Threshold: %.0f\n", result.vadThreshold));
        message.append(String.format(Locale.US, "• Silence Duration: %d ms\n\n", result.vadSilenceDuration));
        
        if (!result.recommendations.isEmpty()) {
            message.append("Environment Analysis:\n");
            for (String recommendation : result.recommendations) {
                message.append("• ").append(recommendation).append("\n");
            }
            message.append("\n");
        }
        
        message.append("Would you like to apply these optimized settings?");
        
        new AlertDialog.Builder(context)
                .setTitle("Calibration Results")
                .setMessage(message.toString())
                .setPositiveButton("Apply Settings", (dialog, which) -> {
                    testManager.applyCalibrationResults(result);
                    SettingsDialogs.showInfoDialog(context, "Settings Applied", 
                        "Your audio settings have been optimized based on the calibration results.");
                })
                .setNegativeButton("Keep Current Settings", null)
                .show();
    }
    
    /**
     * Show speaker test dialog
     */
    public static void showSpeakerTestDialog(Context context, AudioTestManager testManager) {
        SettingsDialogs.showInfoDialog(context, "Speaker Test", 
            "Speaker testing functionality will be implemented in a future update. " +
            "This will include:\n\n" +
            "• Test tone generation\n" +
            "• Volume level testing\n" +
            "• Left/Right channel verification\n" +
            "• Frequency response testing\n\n" +
            "For now, you can test speakers by using the voice chat feature in the main app.");
    }
    
    /**
     * Custom view for real-time audio level visualization
     */
    public static class AudioLevelMeter extends View {
        private Paint levelPaint;
        private Paint peakPaint;
        private Paint backgroundPaint;
        private Paint textPaint;
        
        private float currentLevel = 0.0f;
        private float currentPeak = 0.0f;
        private float peakHold = 0.0f;
        private long lastPeakTime = 0;
        
        private static final long PEAK_HOLD_TIME = 1000; // 1 second
        
        public AudioLevelMeter(Context context) {
            super(context);
            init();
        }
        
        public AudioLevelMeter(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }
        
        private void init() {
            levelPaint = new Paint();
            levelPaint.setColor(0xFF4CAF50); // Green
            levelPaint.setStyle(Paint.Style.FILL);
            
            peakPaint = new Paint();
            peakPaint.setColor(0xFFFF9800); // Orange
            peakPaint.setStyle(Paint.Style.FILL);
            
            backgroundPaint = new Paint();
            backgroundPaint.setColor(0xFF333333); // Dark gray
            backgroundPaint.setStyle(Paint.Style.FILL);
            
            textPaint = new Paint();
            textPaint.setColor(0xFFFFFFFF); // White
            textPaint.setTextSize(24);
            textPaint.setAntiAlias(true);
        }
        
        public void updateLevels(float level, float peak) {
            currentLevel = Math.max(0, Math.min(1, level));
            currentPeak = Math.max(0, Math.min(1, peak));
            
            // Update peak hold
            if (currentPeak > peakHold) {
                peakHold = currentPeak;
                lastPeakTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - lastPeakTime > PEAK_HOLD_TIME) {
                peakHold = Math.max(peakHold - 0.01f, currentPeak);
            }
            
            invalidate();
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            int width = getWidth();
            int height = getHeight();
            
            // Draw background
            canvas.drawRect(0, 0, width, height, backgroundPaint);
            
            // Draw level bar
            float levelWidth = width * currentLevel;
            if (levelWidth > 0) {
                // Color based on level
                if (currentLevel < 0.7f) {
                    levelPaint.setColor(0xFF4CAF50); // Green
                } else if (currentLevel < 0.9f) {
                    levelPaint.setColor(0xFFFF9800); // Orange
                } else {
                    levelPaint.setColor(0xFFFF5722); // Red
                }
                canvas.drawRect(0, 0, levelWidth, height, levelPaint);
            }
            
            // Draw peak hold line
            if (peakHold > 0) {
                float peakX = width * peakHold;
                peakPaint.setColor(0xFFFFEB3B); // Yellow
                canvas.drawRect(peakX - 2, 0, peakX + 2, height, peakPaint);
            }
            
            // Draw scale marks
            Paint scalePaint = new Paint();
            scalePaint.setColor(0xFF666666);
            scalePaint.setStrokeWidth(1);
            
            for (int i = 1; i < 10; i++) {
                float x = width * i / 10.0f;
                canvas.drawLine(x, 0, x, height, scalePaint);
            }
            
            // Draw percentage text
            String levelText = String.format(Locale.US, "%.0f%%", currentLevel * 100);
            float textWidth = textPaint.measureText(levelText);
            canvas.drawText(levelText, (width - textWidth) / 2, height / 2 + 8, textPaint);
        }
    }
}

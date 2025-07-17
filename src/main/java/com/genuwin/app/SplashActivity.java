package com.genuwin.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Splash screen activity that displays the app logo and version for 2 seconds
 * before transitioning to the main activity.
 */
public class SplashActivity extends AppCompatActivity {
    
    private static final int SPLASH_DURATION = 2000; // 2 seconds
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // Set up the version text dynamically from AppVersion class
        TextView versionText = findViewById(R.id.version_text);
        versionText.setText(AppVersion.getVersionString());
        
        // Set up the logo image
        ImageView logoImage = findViewById(R.id.splash_logo);
        logoImage.setImageResource(R.mipmap.ic_launcher);
        
        // Transition to MainActivity after 2 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close splash activity
        }, SPLASH_DURATION);
    }
    
    @Override
    public void onBackPressed() {
        // Disable back button during splash screen
        // Do nothing
    }
}

package com.genuwin.app.homeassistant;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeAssistantManager {
    private static final String TAG = "HomeAssistantManager";
    private final OkHttpClient client = new OkHttpClient();
    private final String baseUrl;
    private final String apiKey;

    public HomeAssistantManager(Context context) {
        Properties properties = new Properties();
        try (InputStream is = context.getAssets().open("config.properties")) {
            properties.load(is);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load config.properties", e);
        }
        this.baseUrl = properties.getProperty("BASE_URL");
        String rawApiKey = properties.getProperty("API_KEY");
        // Remove quotes if present
        this.apiKey = rawApiKey != null ? rawApiKey.replaceAll("^\"|\"$", "") : null;
    }

    public void callService(String domain, String service, String entityId) {
        callService(domain, service, entityId, null);
    }
    
    public void callService(String domain, String service, String entityId, String additionalData) {
        new Thread(() -> {
            try {
                String url = baseUrl + "/api/services/" + domain + "/" + service;
                String json;
                if (additionalData != null && !additionalData.isEmpty()) {
                    // Merge entity_id with additional data
                    json = "{\"entity_id\": \"" + entityId + "\", " + additionalData + "}";
                } else {
                    json = "{\"entity_id\": \"" + entityId + "\"}";
                }
                
                Log.d(TAG, "Calling HA service: " + url + " with data: " + json);
                
                RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + apiKey)
                        .post(body)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Failed to call service: " + response);
                    } else {
                        Log.d(TAG, "Successfully called service: " + domain + "." + service);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to call service", e);
            }
        }).start();
    }
    
    public void setTemperature(String entityId, double temperature) {
        callService("climate", "set_temperature", entityId, "\"temperature\": " + temperature);
    }
}

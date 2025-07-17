package com.genuwin.app.tools.impl;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.genuwin.app.tools.ToolDefinition;
import com.genuwin.app.tools.ToolExecutor;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WikipediaTool implements ToolExecutor {
    private static final String TAG = "WikipediaTool";
    
    private final Context context;
    private final OkHttpClient client;
    private String authToken;
    private String baseUrl;

    public WikipediaTool(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
        loadConfig();
    }

    private void loadConfig() {
        Properties properties = new Properties();
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("config.properties");
            properties.load(inputStream);
            this.authToken = properties.getProperty("AUDIO_API_KEY");
            this.baseUrl = properties.getProperty("WIKIPEDIA_BASE_URL");
            if (this.baseUrl == null || this.baseUrl.trim().isEmpty()) {
                Log.e(TAG, "WIKIPEDIA_BASE_URL not configured in config.properties");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load config", e);
        }
    }

    @Override
    public ToolDefinition getToolDefinition() {
        Map<String, ToolDefinition.ParameterDefinition> params = new HashMap<>();
        params.put("query", new ToolDefinition.ParameterDefinition("string", "The search query", true));
        return new ToolDefinition("search_wikipedia", "Search Wikipedia for articles", params);
    }

    @Override
    public ValidationResult validateParameters(JsonObject parameters) {
        if (parameters == null || !parameters.has("query")) {
            return ValidationResult.invalid("Missing required parameter: query");
        }
        return ValidationResult.valid();
    }

    @Override
    public void execute(JsonObject parameters, ToolExecutionCallback callback) {
        if (authToken == null || authToken.isEmpty()) {
            callback.onError("Authorization token not found.");
            return;
        }

        String query = parameters.get("query").getAsString();
        String endpoint = "/search_wikipedia";

        new Thread(() -> {
            try {
                JsonObject requestBodyJson = new JsonObject();
                requestBodyJson.addProperty("query", query);
                RequestBody body = RequestBody.create(
                    requestBodyJson.toString(),
                    MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(baseUrl + endpoint)
                        .post(body)
                        .addHeader("Authorization", "Bearer " + authToken)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        callback.onSuccess(responseBody);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "API call failed with code " + response.code() + ": " + errorBody);
                        callback.onError("Failed to search Wikipedia: " + response.message());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error executing Wikipedia search", e);
                callback.onError("Error executing Wikipedia search: " + e.getMessage());
            }
        }).start();
    }
}

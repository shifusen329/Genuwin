package com.genuwin.app.searxng;

import android.content.Context;
import android.util.Log;

import com.genuwin.app.api.services.SearxngService;
import com.google.gson.JsonObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Manager class for SearXNG search integration
 */
public class SearxngManager {
    private static final String TAG = "SearxngManager";
    
    private final Context context;
    private final SearxngService searxngService;
    private final String authorizationToken;
    
    public interface SearxngCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    
    public SearxngManager(Context context) {
        this.context = context;
        
        // Load authorization token from config.properties
        this.authorizationToken = loadAuthorizationToken();
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getSearxngBaseUrl())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        searxngService = retrofit.create(SearxngService.class);
    }
    
    private String loadAuthorizationToken() {
        try {
            java.io.InputStream inputStream = context.getAssets().open("config.properties");
            java.util.Properties properties = new java.util.Properties();
            properties.load(inputStream);
            return properties.getProperty("AUDIO_API_KEY");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load authorization token from config.properties", e);
            return null;
        }
    }
    
    private String getSearxngBaseUrl() {
        try {
            java.io.InputStream inputStream = context.getAssets().open("config.properties");
            java.util.Properties properties = new java.util.Properties();
            properties.load(inputStream);
            String configuredUrl = properties.getProperty("SEARXNG_BASE_URL");
            if (configuredUrl != null && !configuredUrl.trim().isEmpty()) {
                return configuredUrl;
            } else {
                Log.e(TAG, "SEARXNG_BASE_URL not configured in config.properties");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load SearXNG base URL from config.properties", e);
            return null;
        }
    }
    
    public void search(JsonObject searchParams, SearxngCallback callback) {
        String query = searchParams.get("query").getAsString();
        int limit = searchParams.has("limit") ? searchParams.get("limit").getAsInt() : 10;
        String language = searchParams.has("language") ? searchParams.get("language").getAsString() : "en";
        String categories = searchParams.has("categories") ? searchParams.get("categories").getAsString() : "general";
        String engines = searchParams.has("engines") ? searchParams.get("engines").getAsString() : null;
        int pageNumber = searchParams.has("pageno") ? searchParams.get("pageno").getAsInt() : 1;
        String timeRange = searchParams.has("time_range") ? searchParams.get("time_range").getAsString() : null;
        int safeSearch = searchParams.has("safesearch") ? searchParams.get("safesearch").getAsInt() : 0;
        
        Log.d(TAG, "SearXNG search request: " + query + " (limit: " + limit + ", lang: " + language + ")");
        
        Call<SearxngService.SearxngResponse> call = searxngService.search(
                "Bearer " + authorizationToken, query, "json", categories, engines, language, pageNumber, timeRange, safeSearch
        );
        
        call.enqueue(new Callback<SearxngService.SearxngResponse>() {
            @Override
            public void onResponse(Call<SearxngService.SearxngResponse> call, 
                                 Response<SearxngService.SearxngResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SearxngService.SearxngResponse searxngResponse = response.body();
                    String formattedResponse = formatSearchResults(searxngResponse, limit);
                    Log.d(TAG, "SearXNG search response: " + formattedResponse);
                    callback.onSuccess(formattedResponse);
                } else {
                    String errorMsg = "SearXNG API error: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += " - " + response.errorBody().string();
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to read error body", e);
                        }
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }
            
            @Override
            public void onFailure(Call<SearxngService.SearxngResponse> call, Throwable t) {
                String errorMsg = "SearXNG network error: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }
    
    private String formatSearchResults(SearxngService.SearxngResponse response, int limit) {
        StringBuilder formatted = new StringBuilder();
        formatted.append("SEARXNG_RESULTS:\n\n");
        
        formatted.append("Query: ").append(response.query != null ? response.query : "Unknown").append("\n");
        formatted.append("Total results: ").append(response.number_of_results).append("\n\n");
        
        // Add answers if available
        if (response.answers != null && response.answers.length > 0) {
            formatted.append("ANSWERS:\n");
            for (String answer : response.answers) {
                formatted.append("- ").append(answer).append("\n");
            }
            formatted.append("\n");
        }
        
        // Add infoboxes if available
        if (response.infoboxes != null && response.infoboxes.length > 0) {
            formatted.append("INFOBOXES:\n");
            for (SearxngService.SearxngResponse.Infobox infobox : response.infoboxes) {
                formatted.append("Title: ").append(infobox.infobox != null ? infobox.infobox : "No title").append("\n");
                formatted.append("Content: ").append(infobox.content != null ? infobox.content : "No content").append("\n");
                if (infobox.urls != null && infobox.urls.length > 0) {
                    formatted.append("URLs:\n");
                    for (SearxngService.SearxngResponse.Infobox.Url url : infobox.urls) {
                        formatted.append("  - ").append(url.title).append(": ").append(url.url).append("\n");
                    }
                }
                formatted.append("\n");
            }
        }
        
        // Add search results
        if (response.results != null && response.results.length > 0) {
            formatted.append("SEARCH RESULTS:\n");
            int count = Math.min(response.results.length, limit);
            for (int i = 0; i < count; i++) {
                SearxngService.SearxngResponse.Result result = response.results[i];
                formatted.append("Result ").append(i + 1).append(":\n");
                formatted.append("Title: ").append(result.title != null ? result.title : "No title").append("\n");
                formatted.append("URL: ").append(result.url != null ? result.url : "No URL").append("\n");
                formatted.append("Content: ").append(result.content != null ? result.content : "No content").append("\n");
                
                if (result.engines != null && result.engines.length > 0) {
                    formatted.append("Engines: ").append(String.join(", ", result.engines)).append("\n");
                }
                
                if (result.category != null) {
                    formatted.append("Category: ").append(result.category).append("\n");
                }
                
                formatted.append("\n");
            }
        } else {
            formatted.append("No search results found.\n");
        }
        
        // Add suggestions if available
        if (response.suggestions != null && response.suggestions.length > 0) {
            formatted.append("SUGGESTIONS:\n");
            for (String suggestion : response.suggestions) {
                formatted.append("- ").append(suggestion).append("\n");
            }
        }
        
        return formatted.toString();
    }
}

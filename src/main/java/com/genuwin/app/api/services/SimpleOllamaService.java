package com.genuwin.app.api.services;

import com.genuwin.app.api.models.OllamaChatResponse;
import com.genuwin.app.api.models.SimpleChatRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Simplified Ollama service interface without tools support
 */
public interface SimpleOllamaService {
    
    @POST("api/chat")
    Call<OllamaChatResponse> chat(@Body SimpleChatRequest request);
}

package com.genuwin.app.api.services;

import com.genuwin.app.api.models.ChatRequest;
import com.genuwin.app.api.models.OllamaChatResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Retrofit service interface for Ollama API and OpenAI Responses API
 */
public interface OllamaService {
    @POST("/api/chat")
    Call<OllamaChatResponse> chat(@Body ChatRequest request);
    
    @POST("/v1/responses")
    Call<OllamaChatResponse> openaiResponses(@Header("Authorization") String authorization, 
                                             @Header("Content-Type") String contentType,
                                             @Body ChatRequest request);
}

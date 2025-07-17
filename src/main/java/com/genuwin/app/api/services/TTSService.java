package com.genuwin.app.api.services;

import com.genuwin.app.api.models.TTSRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Retrofit service interface for TTS API
 */
public interface TTSService {
    @POST("/v1/audio/speech")
    Call<ResponseBody> generateSpeech(
        @Header("Authorization") String authorization,
        @Body TTSRequest request
    );
}

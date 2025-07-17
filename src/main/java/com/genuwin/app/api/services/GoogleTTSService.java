package com.genuwin.app.api.services;

import com.genuwin.app.api.models.GoogleTTSRequest;
import com.genuwin.app.api.models.GoogleTTSResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit service interface for Google Cloud Text-to-Speech API
 */
public interface GoogleTTSService {
    @POST("v1/text:synthesize")
    Call<GoogleTTSResponse> synthesize(
        @Query("key") String apiKey,
        @Body GoogleTTSRequest request
    );
}

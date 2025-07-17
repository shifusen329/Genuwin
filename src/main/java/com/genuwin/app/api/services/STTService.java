package com.genuwin.app.api.services;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Retrofit service interface for STT API
 */
public interface STTService {
    @Multipart
    @POST("/v1/audio/transcriptions")
    Call<ResponseBody> transcribeAudio(
        @Header("Authorization") String authorization,
        @Part MultipartBody.Part file,
        @Part("model") String model
    );
}

package com.genuwin.app.api.models;

import com.google.gson.annotations.SerializedName;

public class GoogleTTSResponse {

    @SerializedName("audioContent")
    private String audioContent;

    public String getAudioContent() {
        return audioContent;
    }
}

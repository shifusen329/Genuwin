package com.genuwin.app.api.models;

import com.google.gson.annotations.SerializedName;

public class GoogleTTSRequest {

    @SerializedName("input")
    private SynthesisInput input;

    @SerializedName("voice")
    private VoiceSelectionParams voice;

    @SerializedName("audioConfig")
    private AudioConfig audioConfig;

    public GoogleTTSRequest(SynthesisInput input, VoiceSelectionParams voice, AudioConfig audioConfig) {
        this.input = input;
        this.voice = voice;
        this.audioConfig = audioConfig;
    }

    public static class SynthesisInput {
        @SerializedName("text")
        private String text;

        public SynthesisInput(String text) {
            this.text = text;
        }
    }

    public static class VoiceSelectionParams {
        @SerializedName("languageCode")
        private String languageCode;

        @SerializedName("name")
        private String name;

        @SerializedName("ssmlGender")
        private String ssmlGender;

        public VoiceSelectionParams(String languageCode, String name) {
            this.languageCode = languageCode;
            this.name = name;
        }

        public VoiceSelectionParams(String languageCode, String name, String ssmlGender) {
            this.languageCode = languageCode;
            this.name = name;
            this.ssmlGender = ssmlGender;
        }
    }

    public static class AudioConfig {
        @SerializedName("audioEncoding")
        private String audioEncoding;

        public AudioConfig(String audioEncoding) {
            this.audioEncoding = audioEncoding;
        }
    }
}

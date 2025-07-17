package com.genuwin.app.api.models;

/**
 * Request model for TTS API
 */
public class TTSRequest {
    private String model;
    private String input;
    private String voice;
    private String response_format;
    private float speed;
    
    public TTSRequest() {}
    
    public TTSRequest(String model, String input, String voice) {
        this.model = model;
        this.input = input;
        this.voice = voice;
        this.response_format = "mp3";
        this.speed = 1.0f;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getInput() {
        return input;
    }
    
    public void setInput(String input) {
        this.input = input;
    }
    
    public String getVoice() {
        return voice;
    }
    
    public void setVoice(String voice) {
        this.voice = voice;
    }
    
    public String getResponse_format() {
        return response_format;
    }
    
    public void setResponse_format(String response_format) {
        this.response_format = response_format;
    }
    
    public float getSpeed() {
        return speed;
    }
    
    public void setSpeed(float speed) {
        this.speed = speed;
    }
}

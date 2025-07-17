package com.genuwin.app.vad;

public class VADConfig {
    private int silenceThreshold;
    private long minSilenceDuration;

    public VADConfig(int silenceThreshold, long minSilenceDuration) {
        this.silenceThreshold = silenceThreshold;
        this.minSilenceDuration = minSilenceDuration;
    }

    public int getSilenceThreshold() {
        return silenceThreshold;
    }

    public long getMinSilenceDuration() {
        return minSilenceDuration;
    }
}

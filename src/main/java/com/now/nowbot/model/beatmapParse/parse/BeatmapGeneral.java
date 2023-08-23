package com.now.nowbot.model.beatmapParse.parse;

import com.now.nowbot.model.enums.OsuMode;

public class BeatmapGeneral {
    int version;
    String sampleSet;
    Double stackLeniency;
    OsuMode mode;

    public BeatmapGeneral(int version) {
        this.version = version;
    }

    public String getSampleSet() {
        return sampleSet;
    }

    public void setSampleSet(String sampleSet) {
        this.sampleSet = sampleSet;
    }

    public Double getStackLeniency() {
        return stackLeniency;
    }

    public void setStackLeniency(Double stackLeniency) {
        this.stackLeniency = stackLeniency;
    }

    public OsuMode getMode() {
        return mode;
    }

    public void setMode(OsuMode mode) {
        this.mode = mode;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}

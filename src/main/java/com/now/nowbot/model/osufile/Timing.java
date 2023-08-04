package com.now.nowbot.model.osufile;

import com.now.nowbot.model.osufile.timing.TimingEffect;
import com.now.nowbot.model.osufile.timing.TimingSampleSet;

public class Timing {
    int start_time;
    Double beat_length;
    int meter; //节拍
    TimingSampleSet sample_set;
    int sample_parameter;
    int volume;
    boolean isRedLine;
    TimingEffect effect;


    double bpm;

    double slider_velocity;

    public Timing(int start_time, double bpm, boolean isRedLine){
        this.start_time = start_time;
        this.bpm = bpm;
        this.isRedLine = isRedLine;
    }

    public int getStartTime() {
        return start_time;
    }

    public void setStartTime(int start_time) {
        this.start_time = start_time;
    }

    public Double getBeatLength() {
        if (isRedLine) {
            return beat_length;
        } else {
            return 0.0;
        }
    }

    public void setBeatLength(Double beat_length) {
        this.beat_length = beat_length;
    }

    public int getMeter() {
        return meter;
    }

    public void setMeter(int meter) {
        this.meter = meter;
    }

    public TimingSampleSet getSampleSet() {
        return sample_set;
    }

    public void setSampleSet(TimingSampleSet sample_set) {
        this.sample_set = sample_set;
    }

    public int getSampleParameter() {
        return sample_parameter;
    }

    public void setSample_parameter(int sample_parameter) {
        this.sample_parameter = sample_parameter;
    }



    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public boolean isRedLine() {
        return isRedLine;
    }

    public void setRedLine(boolean redLine) {
        isRedLine = redLine;
    }

    public TimingEffect getEffect() {
        return effect;
    }

    public void setEffect(TimingEffect effect) {
        this.effect = effect;
    }

    public double getBpm() {
        return (60000f / beat_length);
    }

    public void setBpm(double bpm) {
        this.bpm = bpm;
    }

    public double getSliderVelocity() {
        if (isRedLine) {
            return 1.0f;
        } else {
            return 100 / (-1f * beat_length);
        }
    }

    public void setSliderVelocity(double slider_velocity) {
        this.slider_velocity = slider_velocity;
    }
}

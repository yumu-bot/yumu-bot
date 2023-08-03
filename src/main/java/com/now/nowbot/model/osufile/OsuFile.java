package com.now.nowbot.model.osufile;

import com.now.nowbot.model.enums.OsuMode;

import java.util.List;

public abstract class OsuFile {
    protected int file_version;

    protected int circle_count;
    protected int slider_count;
    protected int spinner_count;

    protected OsuMode mode;

    protected double AR;
    protected double CS;
    protected double OD;
    protected double HP;

    protected double slider_base_velocity;
    protected double slider_tick_rate;
    protected Double stack_leniency = -1D;

    List<HitObject> hitObjects;

    public static OsuFile getInstance(OsuMode mode, OsuFile file) {
        OsuFile newB = null;
        switch (mode) {
            case OSU:
                newB = new OsuFileMania(file);
                break;
            case TAIKO:
                newB = new OsuFileMania(file);
                break;
            case CATCH:
                newB = new OsuFileMania(file);
                break;
            case MANIA:
                newB = new OsuFileMania(file);
                break;
        }
        return newB;
    }

    public int getFileVersion() {
        return file_version;
    }

    public void setFileVersion(int version) {
        this.file_version = version;
    }

    public int getCircleCount() {
        return circle_count;
    }

    public void setCircleCount(int count) {
        this.circle_count = count;
    }

    public int getSliderCount() {
        return slider_count;
    }

    public void setSliderCount(int count) {
        this.slider_count = count;
    }

    public int getSpinnerCount() {
        return spinner_count;
    }

    public void setSpinnerCount(int count) {
        this.slider_count = count;
    }

    public double getAR() {
        return AR;
    }

    public void setAR(double AR) {
        this.AR = AR;
    }

    public double getCS() {
        return CS;
    }

    public void setCS(double CS) {
        this.CS = CS;
    }

    public double getOD() {
        return OD;
    }

    public void setOD(double OD) {
        this.OD = OD;
    }

    public double getHP() {
        return HP;
    }

    public void setHP(double HP) {
        this.HP = HP;
    }

    public double getSV() {
        return slider_base_velocity;
    }

    public void setSV(double base_sv) {
        this.slider_base_velocity = base_sv;
    }

    public double getTickRate() {
        return slider_tick_rate;
    }

    public void setTickRate(double tick_rate) {
        this.slider_tick_rate = tick_rate;
    }

    public Double getSL() {
        return stack_leniency;
    }

    public void setSL(Double stack_leniency) {
        this.stack_leniency = stack_leniency;
    }

    public OsuMode getMode() {
        return mode;
    }

    public void setMode(OsuMode mode) {
        this.mode = mode;
    }

    public List<HitObject> getHitObjects() {
        return hitObjects;
    }

    public void setHitObjects(List<HitObject> hitObjects) {
        this.hitObjects = hitObjects;
    }
}


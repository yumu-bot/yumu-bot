package com.now.nowbot.model.beatmapParse.parse;

import com.now.nowbot.model.beatmapParse.HitObject;
import com.now.nowbot.model.beatmapParse.Timing;
import com.now.nowbot.model.beatmapParse.timing.TimingEffect;
import com.now.nowbot.model.beatmapParse.timing.TimingSampleSet;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("all")
public class OsuBeatmapAttributes {
    static final double HIT_WINDOW_OSU_MAX = 80;
    static final double HIT_WINDOW_OSU_MID = 50;
    static final double HIT_WINDOW_OSU_MIN = 20;

    static final double HIT_WINDOW_TAIKO_MAX = 50;
    static final double HIT_WINDOW_TAIKO_MID = 35;
    static final double HIT_WINDOW_TAIKO_MIN = 20;

    static final double AR_MS_MAX = 1800;
    static final double AR_MS_MID = 1200;
    static final double AR_MS_MIN = 450;

    protected Integer version;

    protected Integer circleCount;
    protected Integer sliderCount;
    protected Integer spinnerCount;

    protected OsuMode mode;

    protected Double AR;
    protected Double CS;
    protected Double OD;
    protected Double HP;

    protected Double sliderBaseVelocity;
    protected Double sliderTickRate;
    protected Double sliderMultiplier;
    protected Double stackLeniency = -1D;

    protected int length = 0;

    List<HitObject> hitObjects;

    List<Timing> timings;

    /**
     * 逐行读取
     *
     * @param read    osu file
     * @param general 元信息
     * @throws io exception {@link IOException}
     */
    public OsuBeatmapAttributes(BufferedReader read, BeatmapGeneral general) throws IOException {

        String line;
        // 逐行
        while ((line = read.readLine()) != null) {
            if (line.startsWith("[Difficulty]")) {
                // 读取 Difficulty 块
                parseDifficulty(read);
            } else if (line.startsWith("[TimingPoints]")) {
                // 读取 TimingPoints 块
                parseTiming(read);
            } else if (line.startsWith("[HitObjects]")) {
                parseHitObject(read);
            }

        }
    }

    boolean parseDifficulty(BufferedReader reader) throws IOException {
        boolean empty = true;
        String line;
        while ((line = reader.readLine()) != null && !line.equals("")) {
            var entity = line.split(":");
            if (entity.length == 2) {
                var key = entity[0].trim();
                var val = entity[1].trim();

                if (key.equals("ApproachRate")) {
                    AR = Double.parseDouble(val);
                    empty = false;
                }
                if (key.equals("OverallDifficulty")) {
                    OD = Double.parseDouble(val);
                    empty = false;
                }
                if (key.equals("CircleSize")) {
                    CS = Double.parseDouble(val);
                    empty = false;
                }
                if (key.equals("HPDrainRate")) {
                    HP = Double.parseDouble(val);
                    empty = false;
                }
                if (key.equals("SliderTickRate")) {
                    sliderTickRate = Double.parseDouble(val);
                    empty = false;
                }
                if (key.equals("SliderMultiplier")) {
                    sliderMultiplier = Double.parseDouble(val);
                    empty = false;
                }
            }
        }
        return empty;
    }

    boolean parseTiming(BufferedReader reader) throws IOException {
        boolean empty = true;
        String line;
        timings = new LinkedList<>();
        while ((line = reader.readLine()) != null && !line.equals("")) {
            var entity = line.split(",");
            if (entity.length < 8) throw new IOException("解析 [TimingPoints] 错误");

            int start_time = (int) Math.floor(Double.parseDouble(entity[0]));
            Double beat_length = Double.parseDouble(entity[1]);
            int meter = Integer.parseInt(entity[2]); //节拍
            TimingSampleSet sample_set = TimingSampleSet.getType(Integer.parseInt(entity[3]));
            int sample_parameter = Integer.parseInt(entity[4]);
            int volume = Integer.parseInt(entity[5]);
            boolean isRedLine = Boolean.parseBoolean(entity[6]);
            TimingEffect effect = TimingEffect.getType(Integer.parseInt(entity[7]));

            var obj = new Timing(start_time, beat_length, meter, sample_set, sample_parameter, volume, isRedLine, effect);
            timings.add(obj);
        }
        return empty;
    }


    boolean parseHitObject(BufferedReader reader) throws IOException {
        boolean empty = true;
        String line;
        hitObjects = new LinkedList<>();
        while ((line = reader.readLine()) != null && !line.equals("")) {
            var entity = line.split(",");
            if (entity.length < 3) throw new IOException("解析 [HitObjects] 错误");
            int x = Integer.parseInt(entity[0]);
            int y = Integer.parseInt(entity[1]);
            int time = Integer.parseInt(entity[2]);

            var obj = new HitObject(x, y, time);
            hitObjects.add(obj);
        }

        if (! CollectionUtils.isEmpty(hitObjects)) {
            length = hitObjects.getLast().getEndTime() - hitObjects.getFirst().getStartTime();
        }

        return empty;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getCircleCount() {
        return circleCount;
    }

    public void setCircleCount(Integer circleCount) {
        this.circleCount = circleCount;
    }

    public Integer getSliderCount() {
        return sliderCount;
    }

    public void setSliderCount(Integer sliderCount) {
        this.sliderCount = sliderCount;
    }

    public Integer getSpinnerCount() {
        return spinnerCount;
    }

    public void setSpinnerCount(Integer spinnerCount) {
        this.spinnerCount = spinnerCount;
    }

    public OsuMode getMode() {
        return mode;
    }

    public void setMode(OsuMode mode) {
        this.mode = mode;
    }

    public Double getAR() {
        return AR;
    }

    public void setAR(Double AR) {
        this.AR = AR;
    }

    public Double getCS() {
        return CS;
    }

    public void setCS(Double CS) {
        this.CS = CS;
    }

    public Double getOD() {
        return OD;
    }

    public void setOD(Double OD) {
        this.OD = OD;
    }

    public Double getHP() {
        return HP;
    }

    public void setHP(Double HP) {
        this.HP = HP;
    }

    public Double getSliderBaseVelocity() {
        return sliderBaseVelocity;
    }

    public void setSliderBaseVelocity(Double sliderBaseVelocity) {
        this.sliderBaseVelocity = sliderBaseVelocity;
    }

    public Double getSliderTickRate() {
        return sliderTickRate;
    }

    public void setSliderTickRate(Double sliderTickRate) {
        this.sliderTickRate = sliderTickRate;
    }

    public Double getSliderMultiplier() {
        return sliderMultiplier;
    }

    public void setSliderMultiplier(Double sliderMultiplier) {
        this.sliderMultiplier = sliderMultiplier;
    }

    public Double getStackLeniency() {
        return stackLeniency;
    }

    public void setStackLeniency(Double stackLeniency) {
        this.stackLeniency = stackLeniency;
    }

    public List<HitObject> getHitObjects() {
        return hitObjects;
    }

    public void setHitObjects(List<HitObject> hitObjects) {
        this.hitObjects = hitObjects;
    }

    public List<Timing> getTimings() {
        return timings;
    }

    public void setTimings(List<Timing> timings) {
        this.timings = timings;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public boolean isConverted() {
        return this.getClass() != OsuBeatmapAttributes.class && mode == OsuMode.OSU;
    }

    public double getArHitWindow(int mods, double clockRate) {
        double arValue = getAR();
        if (Mod.hasHr(mods)) {
            arValue = Math.min(arValue * 1.4, 10d);
        } else if (Mod.hasEz(mods)) {
            arValue *= 0.5;
        }

        return difficultyRange(arValue, AR_MS_MIN, AR_MS_MID, AR_MS_MAX) / clockRate;
    }

    public double getOdHitWindow(int mods, double clockRate) {
        double odValue = getOD();
        if (Mod.hasHr(mods)) {
            odValue = Math.min(odValue * 1.4, 10d);
        } else if (Mod.hasEz(mods)) {
            odValue *= 0.5;
        }
        double window = 0;
        switch (mode) {
            case OSU, CATCH -> {
                window = difficultyRange(
                        odValue,
                        HIT_WINDOW_OSU_MAX,
                        HIT_WINDOW_OSU_MID,
                        HIT_WINDOW_OSU_MIN
                ) / clockRate;
            }
            case TAIKO -> {
                window = difficultyRange(
                        odValue,
                        HIT_WINDOW_TAIKO_MAX,
                        HIT_WINDOW_TAIKO_MID,
                        HIT_WINDOW_TAIKO_MIN
                ) / clockRate;
            }
            case MANIA -> {
                if (!isConverted()) {
                    window = 34.0 + 3.0 * (Math.min(10, Math.max(0,(10.0 - getOD()))));
                } else if (getOD() > 4) {
                    window = 34;
                } else {
                    window = 47;
                }
                if (Mod.hasHr(mods)) {
                    window /= 1.4;
                } else if (Mod.hasEz(mods)) {
                    window *= 1.4;
                }
            }/*
            case null, default -> {
                throw new RuntimeException("?");
            }
            */
        }
        return Math.ceil((window * Math.floor(clockRate)) / clockRate);
    }

    private double difficultyRange(double difficulty, double min, double mid, double max) {
        if (difficulty > 5) {
            return mid + (max - mid) * (difficulty - 5) / 5;
        } else if (difficulty < 5){
            return mid - (mid - min) * (5 - difficulty) / 5;
        } else {
            return mid;
        }
    }
}

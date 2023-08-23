package com.now.nowbot.model.beatmapParse.parse;

import com.now.nowbot.model.beatmapParse.HitObject;
import com.now.nowbot.model.beatmapParse.Timing;
import com.now.nowbot.model.beatmapParse.timing.TimingEffect;
import com.now.nowbot.model.beatmapParse.timing.TimingSampleSet;
import com.now.nowbot.model.enums.OsuMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class OsuBeatmapAttributes {
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

    List<HitObject> hitObjects;

    List<Timing> timings;

    /**
     * 逐行读取
     *
     * @param read    osu file
     * @param general 元信息
     * @throws {@link IOException} io exception
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
        String line = "";
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
}
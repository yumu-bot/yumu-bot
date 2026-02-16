package com.now.nowbot.model.beatmapParse.parse;

import com.now.nowbot.model.beatmapParse.HitObject;
import com.now.nowbot.model.beatmapParse.Timing;
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectPosition;
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectType;
import com.now.nowbot.model.beatmapParse.timing.TimingEffect;
import com.now.nowbot.model.beatmapParse.timing.TimingSampleSet;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.util.ContextUtil;
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

    protected double clockRate = 1d;

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

    List<HitObject> hitObjects = new LinkedList<>();

    List<Timing> timings = new LinkedList<>();

    private static String THREAD_KEY = "R6a8s4d/*9";

    void parseDifficulty(String line) {
        var entity = line.split(":");
        if (entity.length == 2) {
            var key = entity[0].trim();
            var val = entity[1].trim();

            switch (key) {
                case "ApproachRate" -> AR = Double.parseDouble(val);
                case "OverallDifficulty" -> OD = Double.parseDouble(val);
                case "CircleSize" -> CS = Double.parseDouble(val);
                case "HPDrainRate" -> HP = Double.parseDouble(val);
                case "SliderTickRate" -> sliderTickRate = Double.parseDouble(val);
                case "SliderMultiplier" -> sliderMultiplier = Double.parseDouble(val);
            }
        }
    }

    void parseTiming(String line) {
        var entity = line.split(",");
        if (entity.length < 8) throw new RuntimeException("解析 [TimingPoints] 错误");

        int startTime = (int) Math.floor(Double.parseDouble(entity[0]));
        Double beatLength = Double.parseDouble(entity[1]);
        int meter = Integer.parseInt(entity[2]); //节拍
        TimingSampleSet timingSampleSet = TimingSampleSet.getType(Integer.parseInt(entity[3]));
        int sampleParameter = Integer.parseInt(entity[4]);
        int volume = Integer.parseInt(entity[5]);
        boolean isRedLine = Boolean.parseBoolean(entity[6]);
        TimingEffect effect = TimingEffect.getType(Integer.parseInt(entity[7]));

        var obj = new Timing(startTime, beatLength, meter, timingSampleSet, sampleParameter, volume, isRedLine, effect);
        timings.add(obj);
    }

    /**
     * 逐行读取
     *
     * @param read    osu file
     * @param general 元信息
     * @throws io exception {@link IOException}
     */
    public OsuBeatmapAttributes(BufferedReader read, BeatmapGeneral general) throws IOException {
        String line;
        String section = "";
        // 逐行
        while ((line = read.readLine()) != null) {
            if (line.startsWith("[")) {
                section = line;
                line = read.readLine();
            }
            if (line == null || line.isBlank()) { //空谱面会 null
                continue;
            }
            switch (section) {
                case "[Difficulty]" -> {
                    // 读取 Difficulty 块
                    parseDifficulty(line);
                }
                case "[TimingPoints]" -> {
                    // 读取 TimingPoints 块
                    parseTiming(line);
                }
                case "[HitObjects]" -> {
                    // 读取 HitObjects 块
                    parseHitObject(line);
                }
            }
        }
        if (! CollectionUtils.isEmpty(hitObjects)) {
            length = hitObjects.getLast().getEndTime() - hitObjects.getFirst().getStartTime();
        }
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

    public double getClockRate() {
        return clockRate;
    }

    // 备注：这个倍率假如说是 DT 给的 1.5 倍，作用在图上，就是图被缩短（除法）了。
    public void setClockRate(double clockRate) {
        this.clockRate = clockRate;

        if (clockRate != 1d && clockRate > 0d) {
            this.setLength((int) (this.getLength() / clockRate));

            for (var h : hitObjects) {
                h.setStartTime((int) (h.getStartTime() / clockRate));
                h.setEndTime((int) (h.getEndTime() / clockRate));
            }

            for (var t : timings) {
                t.setBeatLength(t.getBeatLength() / clockRate);
                t.setBpm(t.getBpm() * clockRate);
                t.setStartTime((int) (t.getStartTime() / clockRate));
            }
        }
    }

    public double getArHitWindow(int mods, double clockRate) {
        double arValue = getAR();
        if ((mods & 1 << 4) != 0) {
            arValue = Math.min(arValue * 1.4, 10d);
        } else if ((mods & 1 << 1) != 0) {
            arValue *= 0.5;
        }

        return difficultyRange(arValue, AR_MS_MIN, AR_MS_MID, AR_MS_MAX) / clockRate;
    }

    public double getOdHitWindow(int mods, double clockRate) {
        double odValue = getOD();
        if ((mods & 1 << 4) != 0) {
            odValue = Math.min(odValue * 1.4, 10d);
        } else if ((mods & 1 << 1) != 0) {
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
                if ((mods & 1 << 4) != 0) {
                    window /= 1.4;
                } else if ((mods & 1 << 1) != 0) {
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

    void parseHitObject(String line) {
        // line 就是 '320,192,153921,1,0,0:0:0:0:' 这种格式的字符串
        var entity = line.split(",");
        if (entity.length < 4) throw new RuntimeException("解析 [HitObjects] 错误");
        // 解析类型
        var type = HitObjectType.getType(Integer.parseInt(entity[3]));
        var startTime = Integer.parseInt(entity[2]);
        var hit = new HitObject();
        hit.setType(type);

        switch (type) {
            // 普通泡泡
            case CIRCLE -> {
                int x = Integer.parseInt(entity[0]);
                int y = Integer.parseInt(entity[1]);
                hit.setPosition(new HitObjectPosition(x, y));
                hit.setStartTime(startTime);
                // 普通泡泡没有结束时间
                hit.setEndTime(startTime);
            }
            // 滑条
            case SLIDER -> {
                int x = Integer.parseInt(entity[0]);
                int y = Integer.parseInt(entity[1]);
                // 滑条计算 time = length / (SliderMultiplier * 100 * SV) * beatLength
                double length = Double.parseDouble(entity[7]);
                double sliderMultiplier = getSliderMultiplier();
                var timing = getBeforeTiming(startTime);
                int sliderTime;
                if (timing.isRedLine()) {
                    sliderTime = (int) Math.round(length / (sliderMultiplier * 100 * 1) * timing.getBeatLength());
                } else {
                    double sv = timing.getBeatLength() / - 100;
                    sliderTime = (int) Math.round(length / (sliderMultiplier * 100 * sv) * timing.getBeatLength());
                }
                hit.setPosition(new HitObjectPosition(x, y));
                hit.setStartTime(startTime);
                hit.setEndTime(startTime + sliderTime);
            }
            // 转盘
            case SPINNER -> {
                hit.setPosition(new HitObjectPosition(256, 192));
                hit.setStartTime(startTime);
                hit.setEndTime(Integer.parseInt(entity[5]));
            }
            // mania 长条
            case LONGNOTE -> {
                int x = Integer.parseInt(entity[0]);
                // 骂娘的长条不看 y (does not affect holds. It defaults to the centre of the playfield)
                hit.setPosition(new HitObjectPosition(x, 192));
                hit.setStartTime(startTime);
                hit.setEndTime(Integer.parseInt(entity[5].split(":")[0]));
            }
        }
        hitObjects.add(hit);
    }

    private Timing getBeforeTiming(int time) {
        int n = ContextUtil.getContext(THREAD_KEY, 0, Integer.class);
        int size = timings.size();
        if (n >= (size - 1)) return timings.get(n);
        int i = n;
        while (i < (size - 1) && timings.get(i).getStartTime() < time) {
            i++;
        }
        ContextUtil.setContext(THREAD_KEY, i);
        return timings.get(i);
    }
}

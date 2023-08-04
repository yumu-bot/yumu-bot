package com.now.nowbot.model.osufile;

import com.now.nowbot.model.enums.OsuMode;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class OsuFile {
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

    public static OsuFile getInstance() {
//        不应该直接新建, 而是通过解析谱面创建这个对象
//        而且 mode 由谱面文件内容 [General] Mode 确定
//        建议暂时不要考虑转谱
        return null;
    }

    public OsuFile(String osuFileStr) throws IOException {
//        转化为 BufferedReader 逐行读取
        new OsuFile(new BufferedReader(
                new InputStreamReader(
                        new ByteArrayInputStream(
                                osuFileStr.getBytes(StandardCharsets.UTF_8)
                        )
                )
        ));
    }

    /**
     * 逐行读取
     * @param read osu file
     * @throws IOException
     */
    public OsuFile(BufferedReader read) throws IOException {
        // 读取 version
        var verson = read.readLine();
        if (verson != null && verson.startsWith("osu file format v")) {
            this.version = Integer.parseInt(verson.substring(17));
        } else {
            throw new RuntimeException("解析错误,文件无效");
        }
        String line;
        // 逐行
        while ((line = read.readLine()) != null) {
            if (line.equals("")) continue;
            if (line.startsWith("[General]")) {
                // 读取 General 块
                parseGeneral(read);
            }
            if (line.startsWith("[Difficulty]")) {
                // 读取 Difficulty 块
                parseDifficulty(read);
            }
            if (line.startsWith("[TimingPoints]")) {
                // todo
            }
            if (line.startsWith("[HitObjects]")) {
                // todo
            }

        }
    }




    private boolean parseGeneral(BufferedReader reader) throws IOException {
        boolean empty = true;
        String line = "";
        while ((line = reader.readLine()) != null && !line.equals("")) {
            var entity = line.split(":");
            if (entity.length == 2) {
                var key = entity[0].trim();
                var val = entity[1].trim();

                if (key.equals("Mode")) {
                    mode = OsuMode.getMode(val);
                    empty = false;
                }
                if (key.equals("StackLeniency")) {
                    stackLeniency = Double.parseDouble(val);
                    empty = false;
                }
            }
        }
        return empty;
    }

    private boolean parseDifficulty(BufferedReader reader) throws IOException {
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

    public int getFileVersion() {
        return version;
    }

    public void setFileVersion(int version) {
        this.version = version;
    }

    public int getCircleCount() {
        return circleCount;
    }

    public void setCircleCount(int count) {
        this.circleCount = count;
    }

    public int getSliderCount() {
        return sliderCount;
    }

    public void setSliderCount(int count) {
        this.sliderCount = count;
    }

    public int getSpinnerCount() {
        return spinnerCount;
    }

    public void setSpinnerCount(int count) {
        this.sliderCount = count;
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
        return sliderBaseVelocity;
    }

    public void setSV(double base_sv) {
        this.sliderBaseVelocity = base_sv;
    }

    public double getTickRate() {
        return sliderTickRate;
    }

    public void setTickRate(double tick_rate) {
        this.sliderTickRate = tick_rate;
    }

    public Double getSL() {
        return stackLeniency;
    }

    public void setSL(Double stack_leniency) {
        this.stackLeniency = stack_leniency;
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


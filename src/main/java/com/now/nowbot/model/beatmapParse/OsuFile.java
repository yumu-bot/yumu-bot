package com.now.nowbot.model.beatmapParse;

import com.now.nowbot.model.beatmapParse.parse.*;
import com.now.nowbot.model.enums.OsuMode;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class OsuFile {
    private BeatmapGeneral general;

    private BufferedReader reader;

    public static OsuFile getInstance(String osuFileStr) throws IOException {
        return new OsuFile(new BufferedReader(
                new InputStreamReader(
                        new ByteArrayInputStream(
                                osuFileStr.getBytes(StandardCharsets.UTF_8)
                        )
                )
        ));
    }

    public static OsuFile getInstance(BufferedReader read) throws IOException {
        return new OsuFile(read);
    }

    public OsuBeatmapAttributes getOsu() throws IOException {
        switch (getMode()) {
            case OSU -> {
                return new OsuBeatmapAttributes(reader, general);
            }
            case TAIKO -> {
                return new TaikoBeatmapAttributes(reader, general);
            }
            case CATCH -> {
                return new CatchBeatmapAttributes(reader, general);
            }
            case MANIA -> {
                return new ManiaBeatmapAttributes(reader, general);
            }
        }
        throw new RuntimeException("mode type error");
    }


    public ManiaBeatmapAttributes getCatch() throws IOException {
        if (getMode() != OsuMode.CATCH && getMode() != OsuMode.OSU) throw new RuntimeException("mode error");
        return new ManiaBeatmapAttributes(reader, general);
    }

    public ManiaBeatmapAttributes getTaiko() throws IOException {
        if (getMode() != OsuMode.TAIKO && getMode() != OsuMode.OSU) throw new RuntimeException("mode error");
        return new ManiaBeatmapAttributes(reader, general);
    }

    public ManiaBeatmapAttributes getMania() throws IOException {
        if (getMode() != OsuMode.MANIA && getMode() != OsuMode.OSU) throw new RuntimeException("mode error");
        return new ManiaBeatmapAttributes(reader, general);
    }

    OsuFile(String osuFileStr) throws IOException {
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
     *
     * @param read osu file
     * @throws {@link IOException} io exception
     */
    @SuppressWarnings("")
    OsuFile(BufferedReader read) throws IOException {
        // 这里将 version 与 general 读取拆分出来, 然后解析出的所有属性放入 Attributes 类中
        // 一是在读取的时候可以自动判断并生成对应的类型, 防止读取未知类型谱面的时候错过其他类型的解析, 比如骂娘的 Column
        // 另外也能将 osu 转谱其他模式
        // 读取 version
        var versionStr = read.readLine();
        int versionInt;
        if (versionStr != null && versionStr.startsWith("osu file format v")) {
            versionInt = Integer.parseInt(versionStr.substring(17));
        } else {
            throw new RuntimeException("解析错误,文件无效");
        }
        this.general = new BeatmapGeneral(versionInt);
        this.reader = read;
        String line;
        // 逐行
        while ((line = read.readLine()) != null && line.equals("")) ;
        if (line != null && line.startsWith("[General]")) {
            // 读取 General 块
            parseGeneral(read);
        } else {
            throw new RuntimeException("解析错误,缺失 [General] 块");
        }
    }


    void parseGeneral(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !line.equals("")) {
            var entity = line.split(":");
            if (entity.length == 2) {
                var key = entity[0].trim();
                var val = entity[1].trim();

                switch (key) {
                    case "Mode" -> general.setMode(OsuMode.getMode(val));
                    case "StackLeniency" -> general.setStackLeniency(Double.parseDouble(val));
                    case "SampleSet" -> general.setSampleSet(val);
                }
            }
        }
    }

    public OsuMode getMode() {
        return general.getMode();
    }
}


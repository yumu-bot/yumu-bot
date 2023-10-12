package com.now.nowbot.model.beatmapParse;

import com.now.nowbot.entity.BeatMapFileLite;
import com.now.nowbot.entity.BeatmapLite;
import com.now.nowbot.model.beatmapParse.parse.*;
import com.now.nowbot.model.enums.OsuMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class OsuFile {
    static final Logger         log =  LoggerFactory.getLogger(OsuFile.class.getName());
    private      BeatmapGeneral general;

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

    public static BeatMapFileLite parseInfo(BufferedReader read) throws IOException {
        var versionStr = read.readLine();
        if (versionStr == null || !versionStr.startsWith("osu file format v")) {
            log.error("解析错误,文件无效 第一行为:[{}]", versionStr);
            throw new RuntimeException("解析错误,文件无效");
        }
        var bf = new BeatMapFileLite();
        HashMap<String, String> info = new HashMap<>(5);
        info.put("AudioFilename", null);
        info.put("Mode", null);
        info.put("BeatmapID", null);
        info.put("BeatmapSetID", null);
        String line;
        // 逐行
        while ((line = read.readLine()) != null) {
            if (line.startsWith("[General]") || line.startsWith("[Metadata]")) {
                // 读取 General 块
                parseAny(read, info);
            } if (line.startsWith("[Events]")) {
                break;
            }
        }

        while ((line = read.readLine()) != null) {
            if (line.startsWith("//") || line.isBlank()) {
                continue;
            } else if (line.startsWith("[")) {
                break;
            }

            int start = line.indexOf('"');
            int end = line.lastIndexOf('"');

            bf.setBackground(line.substring(start + 1, end));
            break;
        }

        if (info.get("AudioFilename") != null) {
            bf.setAudio(info.get("AudioFilename"));
        }
        if (info.get("Mode") != null) {
            bf.setMode(Integer.parseInt(info.get("Mode")));
        }
        if (info.get("BeatmapID") != null) {
            bf.setBid(Long.parseLong(info.get("BeatmapID")));
        }
        if (info.get("BeatmapSetID") != null) {
            bf.setSid(Long.parseLong(info.get("BeatmapSetID")));
        }

        read.close();
        return bf;
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
            if (entity.length != 2) {
                continue;
            }
            var key = entity[0].trim();
            var val = entity[1].trim();

            switch (key) {
                case "Mode" -> general.setMode(OsuMode.getMode(val));
                case "StackLeniency" -> general.setStackLeniency(Double.parseDouble(val));
                case "SampleSet" -> general.setSampleSet(val);
            }
        }
    }

    private static void parseAny(BufferedReader reader, Map<String, String> parseMap) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !line.equals("")) {
            var entity = line.split(":");
            if (entity.length != 2) {
                continue;
            }
            var key = entity[0].trim();
            var val = entity[1].trim();

            if (parseMap != null) {
                if (parseMap.containsKey(key)){
                    parseMap.put(key, val);
                }
            }
        }
    }

    public OsuMode getMode() {
        return general.getMode();
    }
}


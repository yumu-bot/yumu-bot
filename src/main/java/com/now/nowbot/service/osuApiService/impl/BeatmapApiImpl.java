package com.now.nowbot.service.osuApiService.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.config.FileConfig;
import com.now.nowbot.dao.BeatMapDao;
import com.now.nowbot.entity.BeatmapObjectCountLite;
import com.now.nowbot.mapper.BeatmapObjectCountMapper;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.json.*;
import com.now.nowbot.service.BsApiService;
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService;
import com.now.nowbot.util.AsyncMethodExecutor;
import com.now.nowbot.util.JacksonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BeatmapApiImpl implements OsuBeatmapApiService {
    private static final Logger log = LoggerFactory.getLogger(BeatmapApiImpl.class);

    private final OsuApiBaseService        base;
    private final BeatMapDao               beatMapDao;
    private final Path                     osuDir;
    private final BsApiService             bsApiService;
    private final BeatmapObjectCountMapper beatmapObjectCountMapper;

    public BeatmapApiImpl(OsuApiBaseService baseService, FileConfig config, BeatMapDao mapDao, BsApiService bs, BeatmapObjectCountMapper beatmapObjectCountMapper) {
        base = baseService;
        osuDir = Path.of(config.getOsuFilePath());
        beatMapDao = mapDao;
        bsApiService = bs;
        this.beatmapObjectCountMapper = beatmapObjectCountMapper;
    }

    @Override
    public boolean hasBeatMapFileFromDirectory(long bid) {
        var path = osuDir.resolve(bid + ".osu");
        return Files.isRegularFile(path);
    }

    @Nullable
    private String getBeatMapFileFromDirectory(long bid) {
        var path = osuDir.resolve(bid + ".osu");

        if (hasBeatMapFileFromDirectory(bid)) {
            try {
                return Files.readString(path, StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("osu 谱面 API：读取本地谱面文件失败: ", e);
                return null;
            }
        } else {
            return null;
        }
    }

    @Nullable
    private String getBeatMapFileFromLocalService(long bid) {
        try {
            return bsApiService.getOsuFile(bid);
        } catch (Exception e) {
            log.error("osu 谱面 API：获取本地服务谱面失败: ", e);
            return null;
        }
    }

    @Nullable
    private String getBeatMapFileFromOfficialWebsite(long bid) {
        try {
            String osuStr = base.osuApiWebClient.get().uri("https://osu.ppy.sh/osu/{bid}", bid).retrieve()
                                                .bodyToMono(String.class).onErrorReturn("").block();

            if (StringUtils.hasText(osuStr)) {
                return osuStr;
            } else {
                return null;
            }

        } catch (WebClientResponseException e) {
            log.error("osu 谱面 API：请求官网谱面失败: ", e);
            return null;
        }
    }

    @Override
    public boolean refreshBeatMapFileFromDirectory(long bid) {
        var path = osuDir.resolve(bid + ".osu");

        if (hasBeatMapFileFromDirectory(bid)) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                log.error("osu 谱面 API：删除本地谱面文件失败: ", e);
                return false;
            }

            String str = getBeatMapFileString(bid);

            if (StringUtils.hasText(str)) {
                return writeBeatMapFileToDirectory(str, bid);
            }
        }

        return false;
    }

    private boolean writeBeatMapFileToDirectory(String str, long bid) {
        var path = osuDir.resolve(bid + ".osu");

        if (StringUtils.hasText(str)) {
            try {
                Files.writeString(path, str, StandardCharsets.UTF_8);
                return true;
            } catch (IOException e) {
                log.error("osu 谱面 API：写入本地谱面文件失败: ", e);
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public byte[] getBeatMapFileByte(long bid) {
        return Objects.requireNonNullElse(getBeatMapFileString(bid), "").getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Nullable
    public String getBeatMapFileString(long bid) {
        String str = null;

        if (hasBeatMapFileFromDirectory(bid)) {
            str = getBeatMapFileFromDirectory(bid);
        }

        if (StringUtils.hasText(str)) {
            return str;
        } else {
            try {
                str = AsyncMethodExecutor.execute(() -> getBeatMapFileFromLocalService(bid),
                                                  "_getBeatMapFileFromLocalService" + bid, (String) null);
            } catch (Exception e) {
                str = null;
            }
        }

        if (StringUtils.hasText(str)) {
            return str;
        } else {
            try {
                str = AsyncMethodExecutor.execute(() -> getBeatMapFileFromOfficialWebsite(bid),
                                                  "_getBeatMapFileFromWebsite" + bid, (String) null);
            } catch (Exception e) {
                str = null;
            }
        }

        return str;
    }

    // 查一下文件是否跟 checksum 是否对得上
    @Override
    public boolean checkBeatMap(BeatMap beatMap) throws IOException {
        if (beatMap == null) return false;
        return checkBeatMap(beatMap.getBeatMapID(), beatMap.getMd5());
    }

    @Override
    public boolean checkBeatMap(long bid, String checkStr) throws IOException {
        var path = osuDir.resolve(bid + ".osu");
        if (Files.isRegularFile(path) && StringUtils.hasText(checkStr)) {
            return getBeatMapMD5(Files.readString(path)).equals(checkStr);
        }
        return false;
    }

    @Override
    public boolean checkBeatMap(BeatMap beatMap, String fileStr) {
        return getBeatMapMD5(fileStr).equals(beatMap.getMd5());
    }

    private String getBeatMapMD5(String fileStr) {
        return DigestUtils.md5DigestAsHex(fileStr.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Long id, OsuMode mode) {
        Map<String, Object> body = new HashMap<>();
        if (!OsuMode.isDefaultOrNull(mode)) {
            body.put("ruleset_id", mode.getModeValue());
        }
        return base.osuApiWebClient.post().uri("beatmaps/{id}/attributes", id).headers(base::insertHeader)
                                   .bodyValue(body).retrieve().bodyToMono(JsonNode.class).mapNotNull(
                        j -> JacksonUtil.parseObject(j.get("attributes"), BeatmapDifficultyAttributes.class)).block();
    }

    @Override
    public BeatMap getBeatMap(long bid) {
        return base.osuApiWebClient.get().uri("beatmaps/{bid}", bid).headers(base::insertHeader).retrieve()
                                   .bodyToMono(BeatMap.class).map(b -> {
                    beatMapDao.saveMap(b);
                    return b;
                }).block();
    }

    @Override
    public BeatMapSet getBeatMapSet(long sid) {
        return base.osuApiWebClient.get().uri("beatmapsets/{sid}", sid).headers(base::insertHeader).retrieve()
                                   .bodyToMono(BeatMapSet.class).map(s -> {
                    beatMapDao.saveMapSet(s);
                    return s;
                }).block();
    }

    @Override
    public BeatMap getBeatMapFromDataBase(long bid) {
        try {
            var lite = beatMapDao.getBeatMapLite(bid);
            return BeatMapDao.fromBeatmapLite(lite);
        } catch (Exception e) {
            return getBeatMap(bid);
        }
    }

    @Override
    public boolean isNewbieMap(long bid) {
        try {
            var map = beatMapDao.getBeatMapLite(bid);
            return map.getStatus().equalsIgnoreCase("ranked") && map.getDifficultyRating() < 5.7;
        } catch (Exception ignore) {
        }

        try {
            var map = getBeatMap(bid);
            return map.getStatus().equalsIgnoreCase("ranked") && map.getStarRating() < 5.7;
        } catch (WebClientResponseException.NotFound e) {
            return false;
        }
    }

    /**
     * @throws IndexOutOfBoundsException 谱面不完整的时候会丢这个
     */
    private List<Integer> getMapObjectList(String mapStr) throws IndexOutOfBoundsException {
        int start = mapStr.indexOf("[HitObjects]") + 12;
        int end = mapStr.indexOf("[", start);
        String hit;
        if (end > start) {
            hit = mapStr.substring(start, end);
        } else {
            hit = mapStr.substring(start);
        }

        var hitObjects = hit.split("\\s+");
        var hitObjectStr = new ArrayList<String>();
        for (var x : hitObjects) {
            if (!x.trim().isEmpty()) {
                hitObjectStr.add(x);
            }
        }

        var p = Pattern.compile("^\\d+,\\d+,(\\d+)");

        return hitObjectStr.stream().map((m) -> {
            var m2 = p.matcher(m);
            if (m2.find()) {
                return Integer.parseInt(m2.group(1));
            } else {
                return 0;
            }
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Integer> getGrouping(List<Integer> x, int groups) {
        if (groups < 1) throw new IllegalArgumentException();
        var steps = (x.getLast() - x.getFirst()) / (groups + 1);
        var out = new LinkedList<Integer>();
        int m = x.getFirst() + steps;
        short sum = 0;
        for (var i : x) {
            if (i < m) {
                sum++;
            } else {
                out.add((int) sum);
                sum = 0;
                m += steps;
            }
        }

        return out;
    }

    BeatmapObjectCountLite getCount(long bid) {
        var result = new BeatmapObjectCountLite();
        result.setBid(bid);
        var file = getBeatMapFileString(bid);
        if (file == null) {
            refreshBeatMapFileFromDirectory(bid);
            file = getBeatMapFileString(bid);
            if (file == null) {
                return null;
            }
        }
        String md5 = getBeatMapMD5(file);

        result.setCheck(md5);

        List<Integer> objectList;

        try {
            objectList = getMapObjectList(file);
        } catch (IndexOutOfBoundsException e) {
            refreshBeatMapFileFromDirectory(bid);
            try {
                objectList = getMapObjectList(file);
            } catch (IndexOutOfBoundsException e1) {
                result.setTimestamp(new int[0]);
                result.setDensity(new int[0]);
                return result;
            }
        }

        result.setTimestamp(objectList.stream().mapToInt(Integer::intValue).toArray());
        // ??? debug 看起来结果数组长度是25, 不知道你那边有没有校验, 先参数 +1
        // 你妈的 上面忘记加括号了吧
        var grouping = getGrouping(objectList, 26);
        result.setDensity(grouping.stream().mapToInt(Integer::intValue).toArray());

        return result;
    }

    private static int getScoreJudgeCount(@NonNull Score score) {
        var mode = score.getMode();

        var s = score.getStatistics();
        var n320 = s.getCountGeki();
        var n300 = s.getCount300();
        var n200 = s.getCountKatu();
        var n100 = s.getCount100();
        var n50 = s.getCount50();
        var n0 = s.getCountMiss();

        return switch (mode) {
            case OSU -> n300 + n100 + n50 + n0;
            case TAIKO -> n300 + n100 + n0;
            case CATCH -> n300 + n0; //目前问题是，这个玩意没去掉miss中果，会偏大
            //const attr = await getMapAttributes(bid, 0, 2, reload);
            //return attr.nFruits || n300 + n0;

            default -> n320 + n300 + n200 + n100 + n50 + n0;
        };
    }

    @Override
    @NonNull
    public int[] getBeatmapObjectGrouping26(BeatMap b) {
        int[] result = null;
        if (StringUtils.hasText(b.getMd5())) {
            var r = beatmapObjectCountMapper.getDensityByBidAndCheck(b.getBeatMapID(), b.getMd5());
            if (!r.isEmpty()) result = r.getFirst();
        } else {
            var r = beatmapObjectCountMapper.getDensityByBid(b.getBeatMapID());
            if (!r.isEmpty()) result = r.getFirst();
        }

        if (result == null) {
            BeatmapObjectCountLite dataObj;

            dataObj = getCount(b.getBeatMapID());
            dataObj = beatmapObjectCountMapper.saveAndFlush(dataObj);

            result = dataObj.getDensity();
        }

        return Objects.requireNonNullElseGet(result, () -> new int[0]);
    }

    @Override
    public int getFailTime(long bid, int passObj) {
        if (passObj <= 0) return 0;
        var time = beatmapObjectCountMapper.getTimeStampByBidAndIndex(bid, passObj);
        if (time == null) {
            try {
                var dataObj = getCount(bid);
                if (dataObj == null) return 0;
                if (Objects.requireNonNull(dataObj.getTimestamp()).length < passObj) return 0;
                dataObj = beatmapObjectCountMapper.saveAndFlush(dataObj);
                var start = Objects.requireNonNull(dataObj.getTimestamp())[0];
                return Objects.requireNonNull(dataObj.getTimestamp())[passObj] - start;
            } catch (Exception e) {
                return 0;
            }
        }
        return (time / 1000);
    }

    /**
     * 计算成绩f时, 打到的进度
     *
     * @param score 成绩
     * @return double 0-1
     */
    @Override
    public double getPlayPercentage(Score score) {
        if (!Objects.equals("F", score.getRank())) return 1d;
        var n = getScoreJudgeCount(score);
        var playPercentage =
                beatmapObjectCountMapper.getTimeStampPercentageByBidAndIndex(score.getBeatMap().getBeatMapID(), n);
        if (playPercentage == null) {
            try {
                getBeatmapObjectGrouping26(score.getBeatMap());
            } catch (Exception e) {
                log.error("计算或存储物件数据失败", e);
                return 1d;
            }
            playPercentage =
                    beatmapObjectCountMapper.getTimeStampPercentageByBidAndIndex(score.getBeatMap().getBeatMapID(), n);
        }
        // 仍然失败, 取消计算
        if (playPercentage == null) return 1d;
        return playPercentage;
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Long id, OsuMode mode, int modsValue) {
        Map<String, Object> body = new HashMap<>();
        if (!OsuMode.isDefaultOrNull(mode)) {
            body.put("ruleset_id", mode.getModeValue());
        }
        if (modsValue != 0) {
            body.put("mods", modsValue);
        }
        return base.osuApiWebClient.post().uri("beatmaps/{id}/attributes", id).headers(base::insertHeader)
                                   .bodyValue(body).retrieve().bodyToMono(JsonNode.class).mapNotNull(
                        j -> JacksonUtil.parseObject(j.get("attributes"), BeatmapDifficultyAttributes.class)).block();
    }

    @Override
    public JsonNode lookupBeatmap(String checksum, String filename, Long id) {
        return base.osuApiWebClient.get().uri(u -> u.path("beatmapsets/lookup")
                                                    .queryParamIfPresent("checksum", Optional.ofNullable(checksum))
                                                    .queryParamIfPresent("filename", Optional.ofNullable(filename))
                                                    .queryParamIfPresent("id", Optional.ofNullable(id)).build())
                                   .headers(base::insertHeader).retrieve().bodyToMono(JsonNode.class).block();
    }

    @Override
    public BeatMapSetSearch searchBeatMapSet(Map<String, Object> query) {
        return base.osuApiWebClient.get().uri(u -> {
            u.path("beatmapsets/search");
            query.forEach((k, v) -> {
                if (Objects.nonNull(v)) u.queryParam(k, v);
                else u.queryParam(k);
            });
            return u.build();
        }).headers(base::insertHeader).retrieve().bodyToMono(BeatMapSetSearch.class).block();
    }
}

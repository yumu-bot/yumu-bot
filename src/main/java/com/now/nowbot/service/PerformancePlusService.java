package com.now.nowbot.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.config.FileConfig;
import com.now.nowbot.entity.PerformancePlusLite;
import com.now.nowbot.mapper.PerformancePlusLiteRepository;
import com.now.nowbot.model.LazerMod;
import com.now.nowbot.model.json.LazerScore;
import com.now.nowbot.model.json.PPPlus;
import com.now.nowbot.service.osuApiService.impl.BeatmapApiImpl;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.AsyncMethodExecutor;
import com.now.nowbot.util.JacksonUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service("PP_PLUS_SEV")
public class PerformancePlusService {
    private static final Logger log        = LoggerFactory.getLogger(PerformancePlusService.class);
    private static       String API_SCHEME = "http";// 不用改了
    private static       String API_HOST   = "localhost";
    private static       String API_PORT   = "46880";

    public static void runDevelopment() {
        API_SCHEME = "https";
        API_HOST = "ppp.365246692.xyz";
        API_PORT = "443";
    }

    private final Path OSU_FILE_DIR;

    @Resource
    PerformancePlusLiteRepository performancePlusLiteRepository;
    @Resource
    WebClient                     webClient;
    @Resource
    BeatmapApiImpl                beatmapApi;

    public PerformancePlusService(FileConfig config) {
        OSU_FILE_DIR = Path.of(config.getOsuFilePath());
    }

    public PPPlus getMapPerformancePlus(Long beatmapId, List<LazerMod> modList) {
        checkFile(beatmapId);
        Optional<String> mods;
        if (CollectionUtils.isEmpty(modList)) {
            mods = Optional.empty();
        } else {
            var s = JacksonUtil.toJson(modList);
            mods = Optional.of(URLEncoder.encode(s, StandardCharsets.UTF_8));
        }
        var p = performancePlusLiteRepository.findBeatMapPPPById(beatmapId);
        if (p.isPresent()) {
            var result = new PPPlus();
            result.setDifficulty(p.get().toStats());
            return result;
        }
        return webClient.get()
                .uri(
                        u -> u.scheme(API_SCHEME)
                                .host(API_HOST)
                                .port(API_PORT)
                                .path("/api/calculation")
                                .queryParam("BeatmapId", beatmapId)
                                .queryParamIfPresent("Mods", mods)
                                .build()
                )
                .retrieve()
                .bodyToMono(PPPlus.class)
                .block();
    }

    public void clearCache(String beatmapId) {
        var p = new ProcessBuilder("pkill", "-9", "-f", "Difficalcy.PerformancePlus");
        Thread.startVirtualThread(() -> {
            try {
                p.start();
            } catch (IOException e) {
                log.error("", e);
            }
        });
    }

    public PPPlus.Stats calculateUserPerformance(List<LazerScore> bps) throws TipsException {
        List<PPPlus> ppPlus;
        ppPlus = getScorePerformancePlus(bps);

        double aim = 0;
        double jumpAim = 0;
        double flowAim = 0;
        double precision = 0;
        double speed = 0;
        double stamina = 0;
        double accuracy = 0;
        double total = 0;

        List<AsyncMethodExecutor.Supplier<String>> suppliers = new ArrayList<>(7);
        Map<String, List<Double>> ppPlusMap = new ConcurrentHashMap<>(7);


        // 逐个排序
        suppliers.add(createSupplier("aim", ppPlusMap, ppPlus.stream(), PPPlus.Stats::aim));
        suppliers.add(createSupplier("jumpAim", ppPlusMap, ppPlus.stream(), PPPlus.Stats::jumpAim));
        suppliers.add(createSupplier("flowAim", ppPlusMap, ppPlus.stream(), PPPlus.Stats::flowAim));
        suppliers.add(createSupplier("precision", ppPlusMap, ppPlus.stream(), PPPlus.Stats::precision));
        suppliers.add(createSupplier("speed", ppPlusMap, ppPlus.stream(), PPPlus.Stats::speed));
        suppliers.add(createSupplier("stamina", ppPlusMap, ppPlus.stream(), PPPlus.Stats::stamina));
        suppliers.add(createSupplier("accuracy", ppPlusMap, ppPlus.stream(), PPPlus.Stats::accuracy));
        suppliers.add(createSupplier("total", ppPlusMap, ppPlus.stream(), PPPlus.Stats::total));

        AsyncMethodExecutor.AsyncSupplier(suppliers);

        // 计算加权和
        double weight = 1d / 0.95d;

        for (int n = 0; n < ppPlus.size(); n++) {
            weight *= 0.95d;

            aim += ppPlusMap.get("aim").get(n) * weight;
            jumpAim += ppPlusMap.get("jumpAim").get(n) * weight;
            flowAim += ppPlusMap.get("flowAim").get(n) * weight;
            precision += ppPlusMap.get("precision").get(n) * weight;
            speed += ppPlusMap.get("speed").get(n) * weight;
            stamina += ppPlusMap.get("stamina").get(n) * weight;
            accuracy += ppPlusMap.get("accuracy").get(n) * weight;
            total += ppPlusMap.get("total").get(n) * weight;
        }

        return new PPPlus.Stats(aim, jumpAim, flowAim, precision, speed, stamina, accuracy, total);
    }

    public List<PPPlus> getScorePerformancePlus(Iterable<LazerScore> scores) throws TipsException {
        var scoreIds = StreamSupport.stream(scores.spliterator(), true).map(LazerScore::getScoreID).toList();
        var ppPlusList = performancePlusLiteRepository.findScorePPP(scoreIds);
        var ppPlusMap = ppPlusList.stream().collect(Collectors.toMap(PerformancePlusLite::getId, p -> p));

        // 挑选出没有记录的 score
        List<ScorePerformancePlus> body = new ArrayList<>();
        var postDataId = new LinkedList<Long>();
        var allScoreIdList = new LinkedList<Long>();
        for (var score : scores) {
            allScoreIdList.add(score.getScoreID());
            if (ppPlusMap.containsKey(score.getScoreID())) {
                continue;
            }
            postDataId.add(score.getScoreID());
            checkFile(score.getBeatMap().getBeatMapID());
            var combo = score.getMaxCombo();
            var misses = Objects.requireNonNullElse(score.getStatistics().getMiss(), 0);
            // 这俩我猜测是 50 和 100 的数量
            var mehs = Objects.requireNonNullElse(score.getStatistics().getMeh(), 0);
            var oks = Objects.requireNonNullElse(score.getStatistics().getOk(), 0);
            body.add(new ScorePerformancePlus(score.getBeatMap().getBeatMapID() + "", score.getMods(), combo, misses, mehs, oks));
        }

        if (body.isEmpty()) {
            return allScoreIdList.stream().map(ppPlusMap::get).map(stats -> {
                var ppp = new PPPlus();
                ppp.setPerformance(stats.toStats());
                return ppp;
            }).collect(Collectors.toList());
        }
        List<PPPlus> result;

        try {
            result = getScorePerformancePlus(body);
        } catch (WebClientResponseException e) {
            var n = findErrorBid(body);
            getMapPerformancePlus(Long.parseLong(n), List.of());
            beatmapApi.refreshBeatMapFileFromDirectory(Long.parseLong(n));
            Thread.startVirtualThread(() -> this.clearCache(n));
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "PP+：谱面编号 " + n);
        }

        int i = 0;
        var data = new ArrayList<PerformancePlusLite>(postDataId.size());
        for (var scoreId : postDataId) {
            data.add(new PerformancePlusLite(scoreId, Objects.requireNonNull(result).get(i).getPerformance(), PerformancePlusLite.SCORE));
            i++;
        }
        performancePlusLiteRepository.saveAll(data);
        var allScorePPP = new ArrayList<PPPlus>(allScoreIdList.size());
        i = 0;
        for (var id : allScoreIdList) {
            if (postDataId.contains(id)) {
                allScorePPP.add(result.get(i));
                i++;
            } else {
                var lite = ppPlusMap.get(id);
                var ppp = new PPPlus();
                ppp.setPerformance(lite.toStats());
                allScorePPP.add(ppp);
            }
        }
        return allScorePPP;
    }

    // beatmapId 居然要 String ??? [https://difficalcy.syrin.me/api-reference/difficalcy-osu/#post-apibatchcalculation](啥玩意)
    record ScorePerformancePlus(
            @JsonProperty("beatmapId")
            String beatmapId,
            List<LazerMod> mods,
            int combo,
            int misses,
            int mehs,
            int oks
    ) {
    }

    private List<PPPlus> getScorePerformancePlus(List<ScorePerformancePlus> body) {
        return webClient.post()
                .uri(u -> u.scheme(API_SCHEME).host(API_HOST).port(API_PORT).path("/api/batch/calculation").build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(JacksonUtil.toJson(body))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(node -> JacksonUtil.parseObjectList(node, PPPlus.class))
                .block();
    }

    private void checkFile(Long beatmapId) {
        var beatmapFiles = OSU_FILE_DIR.resolve(beatmapId + ".osu");
        if (!Files.isRegularFile(beatmapFiles)) {
            try {
                var fileStr = beatmapApi.getBeatMapFileString(beatmapId);

                if (fileStr == null) throw new RuntimeException();

                Files.writeString(beatmapFiles, fileStr);
            } catch (Throwable e) {
                log.error("下载谱面文件失败", e);
                throw new RuntimeException("下载谱面文件失败");
            }
        }
    }

    private String findErrorBid(List<ScorePerformancePlus> x) {
        if (x.size() > 2) {
            var mid = x.size() / 2;
            var left = x.subList(0, mid);
            var right = x.subList(mid, x.size());
            if (testScorePerformancePlus(left)) {
                return findErrorBid(left);
            } else {
                return findErrorBid(right);
            }
        } else if (x.size() == 2) {
            if (testScorePerformancePlus(x.subList(0,1))) {
                return x.getFirst().beatmapId;
            } else {
                return x.getLast().beatmapId;
            }
        } else {
            return x.getFirst().beatmapId;
        }
    }

    private boolean testScorePerformancePlus(List<ScorePerformancePlus> allPPP) {
        try {
            getScorePerformancePlus(allPPP);
        }  catch (Exception e) {
            return true;
        }
        return false;
    }

    private AsyncMethodExecutor.Supplier<String> createSupplier(String key, Map<String, List<Double>> ppPlusMap, Stream<PPPlus> stream, Function<PPPlus.Stats, Double> function) {
        return () -> {
            ppPlusMap.put(key, stream
                    .map(PPPlus::getPerformance)
                    .map(function)
                    .sorted(Comparator.reverseOrder())
                    .toList()
            );
            return key;
        };
    }
}

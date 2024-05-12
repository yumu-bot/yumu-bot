package com.now.nowbot.service.OsuApiService.impl;

import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.PPPlus;
import com.now.nowbot.model.JsonData.Statistics;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuPPPlusApiService;
import org.jetbrains.annotations.Nullable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PPPlusApiImpl implements OsuPPPlusApiService {
    private static final int[] standard = {5800, 1400, 3200, 2800, 3800, 1200};

    OsuApiBaseService base;
    OsuBeatmapApiService beatmapApiService;

    public PPPlusApiImpl(OsuApiBaseService baseService, OsuBeatmapApiService beatmapApiService) {
        base = baseService;
        this.beatmapApiService = beatmapApiService;
    }

    @Override
    public List<PPPlus> getBeatMapsPPPlus(List<BeatMap> beatMaps) {
        List<PPPlus> list = new ArrayList<>();

        for (var b : beatMaps) {
            if (b != null) {
                list.add(getPPPlus(b.getId(), b.hasLeaderBoard(), OsuMode.getMode(b.getMode()), null, null, null));
            }
        }

        return list;
    }

    @Override
    public PPPlus getUserPPPlus(long uid) {
        for (var s : standard) {
            break;
        }
        return null;
    }

    @Override
    public PPPlus getBeatMapPPPlus(long bid, boolean hasLeaderBoard, OsuMode mode, @Nullable Integer modsInt, @Nullable Integer combo, @Nullable Statistics stat) {
        return getPPPlus(bid, hasLeaderBoard, mode, modsInt, combo, stat);
    }

    /**
     * 通过本地的方式计算 Syrin me 的 pp plus (PP+)
     * @param bid 谱面编号
     * @param isRanked 是否上架。如果谱面未上架，则需要刷新。
     * @param mode 谱面模组。如果输入 DEFAULT，则无需输入以下三个参数
     * @param modsInt 谱面模组数字
     * @param combo 连击数
     * @param stat 需要计算的成绩详细信息，比如判定
     * @return PP+
     */
    @Retryable(retryFor = {SocketTimeoutException.class, ConnectException.class, UnknownHttpStatusCodeException.class},
            //超时类 SocketTimeoutException, 连接失败ConnectException, 其他未知异常UnknownHttpStatusCodeException
            maxAttempts = 5, backoff = @Backoff(delay = 5000L, random = true, multiplier = 1))

    //??
    @SuppressWarnings("all")
    private PPPlus getPPPlus(long bid, boolean isRanked, OsuMode mode, @Nullable Integer modsInt, @Nullable Integer combo, @Nullable Statistics stat) {

        if (! this.beatmapApiService.downloadBeatMapFile(bid)) {
            throw new RuntimeException(STR."谱面\{bid}下载失败！");
        }

        var url = UriComponentsBuilder.fromHttpUrl("https://ppp.365246692.xyz/api/calculation")
                .queryParam("BeatmapId", String.valueOf(bid))
                .queryParamIfPresent("Mod", Optional.ofNullable(modsInt));

        URI uri;

        try {
            uri = switch (mode) {
                case OSU -> url
                        .queryParamIfPresent("Combo", Optional.ofNullable(combo))
                        .queryParam("Mehs", stat.getCount50())
                        .queryParam("Misses", stat.getCountMiss())
                        .queryParam("Oks", stat.getCount100())
                        .build().encode().toUri();
                case TAIKO -> url
                        .queryParamIfPresent("Combo", Optional.ofNullable(combo))
                        .queryParam("Misses", stat.getCountMiss())
                        .queryParam("Oks", stat.getCount100())
                        .build().encode().toUri();
                case CATCH -> url
                        .queryParam("LargeDroplets", stat.getCount300())
                        .queryParam("SmallDroplets", stat.getCount50())
                        .queryParam("Misses", stat.getCountMiss() + stat.getCountKatu())
                        .build().encode().toUri();
                case MANIA -> url
                        .queryParam("Greats", stat.getCount300() + stat.getCountGeki())
                        .queryParam("Goods", stat.getCountKatu())
                        .queryParam("Oks", stat.getCount100())
                        .queryParam("Mehs", stat.getCount50())
                        .queryParam("Misses", stat.getCountMiss())
                        .build().encode().toUri();
                case null, default -> url.build().encode().toUri();
            };
        } catch (Exception e) {
            throw new RuntimeException("PP+：URI 编码错误！");
        }

        /*
        URI uri = UriComponentsBuilder.fromHttpUrl("https://syrin.me/pp+/api/user/")
                .queryParam("BeatmapId", String.valueOf(bid))

                .build().encode().toUri();

         */

        return base.osuApiWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(PPPlus.class)
                .block();
    }
}

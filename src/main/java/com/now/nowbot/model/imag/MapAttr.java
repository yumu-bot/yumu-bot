package com.now.nowbot.model.imag;

import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.throwable.TipsException;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class MapAttr {
    Long id;
    Long bid;
    Integer mods;

    Float ar;
    Float od;
    Float cs;
    Float hp;

    Float arWindow;
    Float odWindow;

    Float stars;
    Float bpm;
    Integer combo;

    public Long getId() {
        return id;
    }

    public Long getBid() {
        return bid;
    }

    public void setBid(Long bid) {
        this.bid = bid;
    }

    public Integer getMods() {
        return mods;
    }

    public void setMods(Integer mods) {
        this.mods = mods;
    }

    public Float getAr() {
        return ar;
    }

    public void setAr(Float ar) {
        this.ar = ar;
    }

    public Float getOd() {
        return od;
    }

    public void setOd(Float od) {
        this.od = od;
    }

    public Float getCs() {
        return cs;
    }

    public void setCs(Float cs) {
        this.cs = cs;
    }

    public Float getHp() {
        return hp;
    }

    public void setHp(Float hp) {
        this.hp = hp;
    }

    public Float getArWindow() {
        return arWindow;
    }

    public void setArWindow(Float arWindow) {
        this.arWindow = arWindow;
    }

    public Float getOdWindow() {
        return odWindow;
    }

    public void setOdWindow(Float odWindow) {
        this.odWindow = odWindow;
    }

    public Float getStars() {
        return stars;
    }

    public void setStars(Float stars) {
        this.stars = stars;
    }

    public Float getBpm() {
        return bpm;
    }

    public void setBpm(Float bpm) {
        this.bpm = bpm;
    }

    public Integer getCombo() {
        return combo;
    }

    public void setCombo(Integer combo) {
        this.combo = combo;
    }


    public static void applyModChangeForBeatMap(BeatMap beatMap, int modsValue, @NonNull ImageService imageService) throws TipsException {
        if (! Mod.hasChangeRating(modsValue)) return;

        var mapAttrGet = new MapAttrGet(OsuMode.getMode(beatMap.getModeInt()));

        mapAttrGet.addMap(beatMap.getSID(), beatMap.getId(), modsValue, beatMap.getRanked());


        // 数据交换
        Map<Long, MapAttr> changedAttrsMap;

        try {
            changedAttrsMap = imageService.getMapAttr(mapAttrGet);
        } catch (ResourceAccessException | HttpServerErrorException.InternalServerError e) {
            throw new TipsException("无法获取星数变化的谱面数据：无法连接到绘图服务器！");
        } catch (HttpServerErrorException | WebClientResponseException e) {
            throw new TipsException("无法获取星数变化的谱面数据：超时（太多了），如果你是第一次见到这条消息，第二次通常就会恢复了。");
        }

        var attr = changedAttrsMap.get(beatMap.getSID().longValue());

        beatMap.setStarRating(attr.getStars());
        beatMap.setBPM(attr.getBpm());
        beatMap.setAR(attr.getAr());
        beatMap.setCS(attr.getCs());
        beatMap.setOD(attr.getOd());
        beatMap.setHP(attr.getHp());
        if (Mod.hasDt(modsValue)) {
            beatMap.setTotalLength(Math.round(beatMap.getTotalLength() / 1.5f));
        } else if (Mod.hasHt(modsValue)) {
            beatMap.setTotalLength(Math.round(beatMap.getTotalLength() / 0.75f));
        }
    }

    public static void applyModChangeForScore(@NonNull Score score, @NonNull ImageService imageService) throws TipsException {
        applyModChangeForBeatMap(score.getBeatMap(), Mod.getModsValueFromStr(score.getMods()), imageService);
    }


    public static void applyModChangeForScores(@Nullable List<Score> scoreList, @NonNull OsuMode mode, @NonNull ImageService imageService) throws TipsException {
        if (CollectionUtils.isEmpty(scoreList)) return;

        var mapAttrGet = new MapAttrGet(mode);

        // 第一次遍历：收集星数变化的谱面
        for (var s : scoreList) {
            var v = Mod.getModsValueFromStr(s.getMods());

            if (Mod.hasChangeRating(v)) {
                var b = s.getBeatMap();

                mapAttrGet.addMap(s.getScoreID(), b.getId(), v, b.getRanked());
            }
        }

        // 数据交换
        Map<Long, MapAttr> changedAttrsMap;

        try {
            changedAttrsMap = imageService.getMapAttr(mapAttrGet);
        } catch (ResourceAccessException | HttpServerErrorException.InternalServerError e) {
            throw new TipsException("无法获取星数变化的谱面数据：无法连接到绘图服务器！");
        } catch (HttpServerErrorException | WebClientResponseException e) {
            throw new TipsException("无法获取星数变化的谱面数据：超时（太多了），如果你是第一次见到这条消息，第二次通常就会恢复了。");
        }

        // 第二次遍历：赋值星数变化的谱面
        for (var s : scoreList) {
            var v = Mod.getModsValueFromStr(s.getMods());

            if (Mod.hasChangeRating(v)) {
                var b = s.getBeatMap();
                var attr = changedAttrsMap.get(s.getScoreID());

                b.setStarRating(attr.getStars());
                b.setBPM(attr.getBpm());
                b.setAR(attr.getAr());
                b.setCS(attr.getCs());
                b.setOD(attr.getOd());
                b.setHP(attr.getHp());
                if (Mod.hasDt(v)) {
                    b.setTotalLength(Math.round(b.getTotalLength() / 1.5f));
                } else if (Mod.hasHt(v)) {
                    b.setTotalLength(Math.round(b.getTotalLength() / 0.75f));
                }
            }
        }
    }
}

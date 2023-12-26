package com.now.nowbot.dao;

import com.now.nowbot.entity.OsuUserInfoArchiveLite;
import com.now.nowbot.mapper.OsuUserInfoMapper;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Statistics;
import com.now.nowbot.model.enums.OsuMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class OsuUserInfoDao {
    OsuUserInfoMapper osuUserInfoMapper;

    @Autowired
    public OsuUserInfoDao(OsuUserInfoMapper mapper) {
        osuUserInfoMapper = mapper;
    }

    public void saveUser(OsuUser user) {
        var u = fromModel(user);
        osuUserInfoMapper.save(u);
    }

    public void saveUsers(List<MicroUser> users) {
        var all = users.stream()
                .flatMap(microUser -> {
                    var osu = fromStatustucs(microUser.getRulesets().getOsu(), OsuMode.OSU);
                    if (osu != null) osu.setOsuID(microUser.getId());
                    var taiko = fromStatustucs(microUser.getRulesets().getTaiko(), OsuMode.TAIKO);
                    if (taiko != null) taiko.setOsuID(microUser.getId());
                    var fruits = fromStatustucs(microUser.getRulesets().getFruits(), OsuMode.CATCH);
                    if (fruits != null) fruits.setOsuID(microUser.getId());
                    var mania = fromStatustucs(microUser.getRulesets().getMania(), OsuMode.OSU);
                    if (mania != null) mania.setOsuID(microUser.getId());

                    return Stream.of(osu, taiko, fruits, mania);
                })
                .filter(su -> su != null)
                .toList();

        osuUserInfoMapper.saveAllAndFlush(all);
    }

    public static OsuUser fromArchive(OsuUserInfoArchiveLite archive) {
        OsuUser user = new OsuUser();
        user.setPlayMode(archive.getMode().getName());
        user.setUID(archive.getOsuID());

        Statistics statistics = new Statistics();
        statistics.setA(statistics.getA());
        statistics.setS(statistics.getS());
        statistics.setSS(statistics.getSS());
        statistics.setSH(statistics.getSH());
        statistics.setSSH(statistics.getSSH());

        statistics.setTotalScore(statistics.getTotalScore());
        statistics.setTotalHits(statistics.getTotalHits());
        statistics.setRankedScore(statistics.getRankedScore());
        statistics.setAccuracy(archive.getHit_accuracy());
        statistics.setPlayCount(archive.getPlay_count());
        statistics.setPlayTime(archive.getPlay_time());
        statistics.setLevelCurrent(archive.getLevel_current());
        statistics.setLevelProgress(archive.getLevel_progress());
        statistics.setMaxCombo(archive.getMaximum_combo());
        statistics.setPp(archive.getPP());

        user.setStatistics(statistics);
        return user;
    }

    /**
     * 取那一天最后的数据
     *
     * @param date 当天
     * @return
     */
    public Optional<OsuUserInfoArchiveLite> getLast(Long uid, OsuMode mode, LocalDate date) {
        return osuUserInfoMapper.selectDayLast(uid, mode, date);
    }

    /**
     * 取 到那一天为止 最后的数据 (默认向前取一年)
     *
     * @param date 那一天
     * @return
     */
    public Optional<OsuUserInfoArchiveLite> getLastFrom(Long uid, OsuMode mode, LocalDate date) {
        var time = LocalDateTime.of(date, LocalTime.MAX);
        return osuUserInfoMapper.selectDayLast(uid, mode, time.minusYears(1), time);
    }

    public static OsuUserInfoArchiveLite fromModel(OsuUser data) {
        var out = new OsuUserInfoArchiveLite();
        var statistics = data.getStatistics();
        out.setOsuID(data.getUID());
        setOut(out, statistics);

        out.setPlay_count(data.getPlayCount());
        out.setPlay_time(data.getPlayTime());
        out.setMode(data.getPlayMode());
        out.setTime(LocalDateTime.now());
        return out;
    }

    private static void setOut(OsuUserInfoArchiveLite out, Statistics statistics) {
        out.setGlobal_rank(statistics.getGlobalRank());
        out.setCountry_rank(statistics.getCountryRank());
        out.setTotal_score(statistics.getTotalScore());
        out.setRanked_score(statistics.getRankedScore());
        out.setGrade_counts_a(statistics.getA());
        out.setGrade_counts_s(statistics.getS());
        out.setGrade_counts_sh(statistics.getSH());
        out.setGrade_counts_ss(statistics.getSS());
        out.setGrade_counts_ssh(statistics.getSSH());

        out.setHit_accuracy(statistics.getAccuracy());
        out.setPP(statistics.getPP());
        out.setLevel_current(statistics.getLevelCurrent());
        out.setLevel_progress(statistics.getLevelProgress());
        out.setIs_ranked(statistics.getRanked());
        out.setMaximum_combo(statistics.getMaxCombo());
        out.setTotal_hits(statistics.getTotalHits());
    }

    private static OsuUserInfoArchiveLite fromStatustucs(Statistics s, OsuMode mode){
        if (s == null) return null;
        var out = new OsuUserInfoArchiveLite();
        out.setPlay_count(s.getPlayCount());
        out.setPlay_time(s.getPlayTime());
        out.setMode(mode);
        out.setTime(LocalDateTime.now());
        setOut(out, s);

        return out;
    }

    public Optional<OsuUserInfoArchiveLite> getLast(Long uid, OsuMode mode) {
        return osuUserInfoMapper.selectLast(uid, mode);
    }
}

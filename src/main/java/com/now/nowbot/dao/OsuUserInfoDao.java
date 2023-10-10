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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
                .map(microUser -> {
                    var osu = fromStatustucs(microUser.getRulesets().getOsu(), OsuMode.OSU);
                    if (osu != null) osu.setOsuID(microUser.getId());
                    var taiko = fromStatustucs(microUser.getRulesets().getTaiko(), OsuMode.TAIKO);
                    if (taiko != null) taiko.setOsuID(microUser.getId());
                    var fruits = fromStatustucs(microUser.getRulesets().getFruits(), OsuMode.CATCH);
                    if (fruits != null) fruits.setOsuID(microUser.getId());
                    var mania = fromStatustucs(microUser.getRulesets().getMania(), OsuMode.OSU);
                    if (mania != null) mania.setOsuID(microUser.getId());

                    return List.of(osu, taiko, fruits, mania);
                })
                .flatMap(Collection::stream)
                .filter(su -> su != null)
                .toList();

        osuUserInfoMapper.saveAllAndFlush(all);
    }

    public Optional<OsuUserInfoArchiveLite> getLast(Long uid, LocalDate date) {
        return osuUserInfoMapper.selectDayLast(uid, LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
    }

    public Optional<OsuUserInfoArchiveLite> getLast(Long uid) {
        return osuUserInfoMapper.selectLast(uid);
    }

    public static OsuUserInfoArchiveLite fromModel(OsuUser data) {
        var out = new OsuUserInfoArchiveLite();
        var statistics = data.getStatistics();
        out.setOsuID(data.getUID());
        out.setGlobal_rank(statistics.getGlobalRank());
        out.setCountry_rank(statistics.getCountryRank());
        out.setTotal_score(statistics.getTotalScore());
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

        out.setPlay_count(data.getPlayCount());
        out.setPlay_time(data.getPlayTime());
        out.setMode(data.getPlayMode());
        out.setTime(LocalDateTime.now());
        return out;
    }
    private static OsuUserInfoArchiveLite fromStatustucs(Statistics s, OsuMode mode){
        if (s == null) return null;
        var out = new OsuUserInfoArchiveLite();
        out.setPlay_count(s.getPlayCount());
        out.setPlay_time(s.getPlayTime());
        out.setMode(mode);
        out.setTime(LocalDateTime.now());
        out.setGlobal_rank(s.getGlobalRank());
        out.setCountry_rank(s.getCountryRank());
        out.setTotal_score(s.getTotalScore());
        out.setGrade_counts_a(s.getA());
        out.setGrade_counts_s(s.getS());
        out.setGrade_counts_sh(s.getSH());
        out.setGrade_counts_ss(s.getSS());
        out.setGrade_counts_ssh(s.getSSH());

        out.setHit_accuracy(s.getAccuracy());
        out.setPP(s.getPP());
        out.setLevel_current(s.getLevelCurrent());
        out.setLevel_progress(s.getLevelProgress());
        out.setIs_ranked(s.getRanked());
        out.setMaximum_combo(s.getMaxCombo());

        return out;
    }
}

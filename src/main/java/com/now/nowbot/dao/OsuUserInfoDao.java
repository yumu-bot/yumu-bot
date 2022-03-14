package com.now.nowbot.dao;

import com.now.nowbot.entity.OsuUserModeScoreLite;
import com.now.nowbot.mapper.OsuUserInfoMapper;
import com.now.nowbot.model.JsonData.OsuUser;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

public class OsuUserInfoDao {
    OsuUserInfoMapper osuUserInfoMapper;

    @Autowired
    public OsuUserInfoDao(OsuUserInfoMapper mapper){
        osuUserInfoMapper = mapper;
    }

    public static OsuUserModeScoreLite fromModel(OsuUser data){
        var out = new OsuUserModeScoreLite();
        var statustucs = data.getStatustucs();
        out.setGlobal_rank(statustucs.getGlobalRank());
        out.setCountry_rank(statustucs.getCountryRank());
        out.setTotal_score(statustucs.getTotalScore());
        out.setGrade_counts_a(statustucs.getA());
        out.setGrade_counts_s(statustucs.getS());
        out.setGrade_counts_sh(statustucs.getSH());
        out.setGrade_counts_ss(statustucs.getSS());
        out.setGrade_counts_ssh(statustucs.getSSH());

        out.setHit_accuracy(statustucs.getAccuracy());
        out.setPp(statustucs.getPp());
        out.setLevel_current(statustucs.getLevelCurrent());
        out.setLevel_progress(statustucs.getLevelProgress());
        out.setIs_ranked(statustucs.getRanked());
        out.setMaximum_combo(statustucs.getMaxCombo());
        out.setPlay_count(data.getPlagCount());
        out.setPlay_time(data.getPlatTime());
        out.setMode(data.getPlayMode());
        out.setTime(LocalDateTime.now());
        return out;
    }
}

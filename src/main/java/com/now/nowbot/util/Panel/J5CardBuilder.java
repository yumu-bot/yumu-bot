package com.now.nowbot.util.Panel;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.Color;
import org.jetbrains.skija.Font;
import org.jetbrains.skija.Typeface;

public class J5CardBuilder extends PanelBuilder {
    public J5CardBuilder(OsuUser user) {
        super(430, 335);

        drawBaseRRect();
        drawUserIndex(user);
        drawUserData(user);
    }

    private void drawBaseRRect(){
        canvas.clear(Color.makeRGB(56, 46, 50));
    }

    private void drawUserIndex(OsuUser user) {
        //画数据指标
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS24 = new Font(TorusSB, 24);
        Font fontS36 = new Font(TorusSB, 36);

        String Jlu1t = "Data";
        String Jll1t = "R.Score"; // Ranked Score
        String Jll2t = "T.Score"; // Total Score
        String Jll3t = "PC"; // PlayCount
        String Jll4t = "PT"; // PlayTime

        double Jml1t = 0; //这里是以下值与上次的变化值，需要数据库一类的东西，我不会写

        double Jl1t = DataUtil.getRoundedNumber(user.getStatistics().getRankedScore(), 2);
        double Jl2t = DataUtil.getRoundedNumber(user.getStatistics().getTotalScore(), 2);
        double Jl3t = DataUtil.getRoundedNumber(user.getStatistics().getPlayCount(), 2);
        double Jl4t = DataUtil.getRoundedNumber(user.getStatistics().getPlayTime(), 2);
        String Jr1t = "Map.PC"; // Beatmap Playcount
        String Jr2t = "Rep.WC"; // Replay Watched by others count
        String Jr3t = "Fans"; // Follower
        String Jr4t = "Map.Fans"; // Map Follower
        double Jrr1t = DataUtil.getRoundedNumber(user.getBeatmapSetCountPlaycounts(), 2);
        double Jrr2t = DataUtil.getRoundedNumber(user.getStatistics().getReplaysWatchedByOthers(), 2);
        double Jrr3t = DataUtil.getRoundedNumber(user.getFollowerCount(), 2);
        double Jrr4t = DataUtil.getRoundedNumber(user.getMappingFollowerCount(), 2);
    }

    private void drawUserData(OsuUser user) {
        //画数据

    }
}

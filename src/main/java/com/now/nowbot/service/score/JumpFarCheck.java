package com.now.nowbot.service.score;

import com.now.nowbot.model.osu.OsuUser;
import com.now.nowbot.model.osu.Score;

public class JumpFarCheck implements ScoreChecker{
    @Override
    public int getWeight(long bid) {
        return 0;
    }

    @Override
    public boolean canHandle(OsuUser user, Score score) {
        double pp = user.getPp();
        return !(pp < 3000 || pp > 4500 || score.getBeatmap().getBeatmapID() != 2459394);
    }

    @Override
    public float getConst(OsuUser user, Score score) {
        int comboScore = 70;
        int noMissScore = 30;

        // 120 160 195 268 400 430
        // 0   25  45  60  70  100

        //
        return 0;
    }

    @Override
    public String getName() {
        return "TS-JUMP-F1";
    }
}

package com.now.nowbot.service.score;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;

public interface ScoreChecker {
    int getWeight(long bid);
    boolean canHandle(OsuUser user, Score score);
    float getConst(OsuUser user, Score score);
    String getName();
}

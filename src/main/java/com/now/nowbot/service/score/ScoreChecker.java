package com.now.nowbot.service.score;

import com.now.nowbot.model.jsonData.OsuUser;
import com.now.nowbot.model.jsonData.Score;

public interface ScoreChecker {
    int getWeight(long bid);
    boolean canHandle(OsuUser user, Score score);
    float getConst(OsuUser user, Score score);
    String getName();
}

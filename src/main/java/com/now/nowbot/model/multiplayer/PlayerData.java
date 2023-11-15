package com.now.nowbot.model.multiplayer;

import com.now.nowbot.model.JsonData.MicroUser;

import java.util.ArrayList;
import java.util.List;

public class PlayerData {
    Match match;

    MicroUser player;
    String team;

    List<Integer> scoreList = new ArrayList<>();

    Integer totalScore = 0;

    //标准化的单场个人得分，即标准分 = score/TotalScore
    List<Double> RRAs = new ArrayList<>();

    //平均每局胜利分配 RWS v3.4添加
    List<Double> RWSs = new ArrayList<>();


}

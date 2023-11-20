package com.now.nowbot.model.multiplayer;

import com.now.nowbot.model.JsonData.MicroUser;

import java.util.ArrayList;
import java.util.List;

public class PlayerData {
    MicroUser player;
    String team;

    //最初的分数
    List<Integer> scores = new ArrayList<>();
    //totalScore
    Integer TTS = 0;
    //标准化的单场个人得分 RRAs，即标准分 = score/TotalScore
    List<Double> RRAs = new ArrayList<>();
    //总得斗力点 TMG，也就是RRAs的和
    Double TMG;
    //场均标准分
    Double AMG;
    //AMG/Average(AMG) 场均标准分的相对值
    Double MQ;
    Double ERA;
    //(TMG*playerNumber)/参赛人次
    Double DRA;
    //MRA = 0.7 * ERA + 0.3 * DRA
    Double MRA;
    //平均每局胜利分配 RWS v3.4添加
    List<Double> RWSs = new ArrayList<>();
    Double RWS;

    PlayerClass playerClass;

    String className;
    String classNameCN;
    int classColor;

    double ERAIndex;
    double DRAIndex;
    double RWSIndex;

    int ranking;

    //胜负场次
    Integer win = 0;
    Integer lose = 0;

    //输入筛选好的对局，玩家名，这场比赛内所有玩家的数量（去重。
    public PlayerData(MicroUser player) {
        this.player = player;
    }
    public PlayerData() {
        this.player = new MicroUser();
    }

    public void calculateTTS() {
        TTS = 0;

        for (Integer score : scores) {
            TTS += score;
        }
    }

    public void calculateAMG() {
        TMG = 0d;
        AMG = 0d;

        for (Double RRA : RRAs) {
            TMG += RRA;
        }

        if (!RRAs.isEmpty()) {
            AMG = TMG / RRAs.size();
        }
    }

    //aAMG是AMG的平均值
    public void calculateMQ(double aAMG) {
        MQ = AMG / aAMG;
    }

    public void calculateERA(double minMQ, double ScalingFactor) {
        ERA = (MQ - minMQ * ScalingFactor) / (1 - minMQ * ScalingFactor);
    }

    public void calculateDRA(int playerCount, int scoreCount) {
        DRA = (TMG / scoreCount) * playerCount;
    }

    public void calculateMRA() {
        MRA = 0.7 * ERA + 0.3 * DRA;
    }

    public void calculateRWS(int roundCount) {
        var tRWS = 0d;

        for (Double rRWS : RWSs) {
            tRWS += rRWS;
        }

        if (!RWSs.isEmpty() && roundCount > 0) {
            RWS = tRWS / roundCount;
        } else {
            RWS = 0d;
        }
    }

    public void calculateClass() {
        playerClass = new PlayerClass(ERAIndex, DRAIndex, RWSIndex);
        this.className = playerClass.getName();
        this.classNameCN = playerClass.getNameCN();
        this.classColor = playerClass.getColor();
    }

    // get set
    public MicroUser getPlayer() {
        return player;
    }

    public void setPlayer(MicroUser player) {
        this.player = player;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public List<Integer> getScores() {
        return scores;
    }

    public void setScores(List<Integer> scores) {
        this.scores = scores;
    }

    public Integer getTTS() {
        return TTS;
    }

    public void setTTS(Integer TTS) {
        this.TTS = TTS;
    }

    public List<Double> getRRAs() {
        return RRAs;
    }

    public void setRRAs(List<Double> RRAs) {
        this.RRAs = RRAs;
    }

    public Double getTMG() {
        return TMG;
    }

    public void setTMG(Double TMG) {
        this.TMG = TMG;
    }

    public Double getAMG() {
        return AMG;
    }

    public void setAMG(Double AMG) {
        this.AMG = AMG;
    }

    public Double getMQ() {
        return MQ;
    }

    public void setMQ(Double MQ) {
        this.MQ = MQ;
    }

    public Double getERA() {
        return ERA;
    }

    public void setERA(Double ERA) {
        this.ERA = ERA;
    }

    public Double getDRA() {
        return DRA;
    }

    public void setDRA(Double DRA) {
        this.DRA = DRA;
    }

    public Double getMRA() {
        return MRA;
    }

    public void setMRA(Double MRA) {
        this.MRA = MRA;
    }

    public List<Double> getRWSs() {
        return RWSs;
    }

    public void setRWSs(List<Double> RWSs) {
        this.RWSs = RWSs;
    }

    public Double getRWS() {
        return RWS;
    }

    public void setRWS(Double RWS) {
        this.RWS = RWS;
    }

    public PlayerClass getPlayerClass() {
        return playerClass;
    }

    public void setPlayerClass(PlayerClass playerClass) {
        this.playerClass = playerClass;
    }

    public double getERAIndex() {
        return ERAIndex;
    }

    public void setERAIndex(double ERAIndex) {
        this.ERAIndex = ERAIndex;
    }

    public double getDRAIndex() {
        return DRAIndex;
    }

    public void setDRAIndex(double DRAIndex) {
        this.DRAIndex = DRAIndex;
    }

    public double getRWSIndex() {
        return RWSIndex;
    }

    public void setRWSIndex(double RWSIndex) {
        this.RWSIndex = RWSIndex;
    }

    public int getRanking() {
        return ranking;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }

    public Integer getWin() {
        return win;
    }

    public void setWin(Integer win) {
        this.win = win;
    }

    public Integer getLose() {
        return lose;
    }

    public void setLose(Integer lose) {
        this.lose = lose;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassNameCN() {
        return classNameCN;
    }

    public void setClassNameCN(String classNameCN) {
        this.classNameCN = classNameCN;
    }

    public int getClassColor() {
        return classColor;
    }

    public void setClassColor(int classColor) {
        this.classColor = classColor;
    }
}

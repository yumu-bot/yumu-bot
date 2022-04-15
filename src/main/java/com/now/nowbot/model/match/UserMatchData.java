package com.now.nowbot.model.match;

import org.jetbrains.skija.Color;

import java.util.ArrayList;
import java.util.List;

public class UserMatchData {
    Integer id;
    String team;
    String username;
    List<Integer> scores = new ArrayList<>();
    //标准化的单场个人得分，即标准分 = score/TotalScore
    List<Double> RRAs = new ArrayList<>();
    //整场比赛的总标准分
    Double TMG;
    //场均标准分
    Double AMG;
    //AMG/Average(AMG) 场均标准分的相对值
    Double MQ;
    //(MQ - min(MQ))/(1 - min(MQ))
    Double ERA;
    //(TMG*playerNumber)/参赛人次
    Double DRA;
    //MRA = 0.7 * ERA + 0.3 * DRA
    Double MRA;

    //与MRA,MDRA相同，范围扩大到正常联赛的多次match
    Double SERA;
    Double SDRA;
    Double SMRA;

    //胜负场次
    Integer wins = 0;
    Integer lost = 0;

    double ERA_index;
    double DRA_index;

    public void calculateAMG() {
        TMG = 0d;
        for (Double RRA : RRAs)
            TMG += RRA;
        AMG = TMG / RRAs.size();
    }


    public void calculateMQ(double averageAMG) {
        MQ = AMG / averageAMG;
    }

    public void calculateERA(double minMQ) {
        ERA = (MQ - minMQ) / (1 - minMQ);
    }

    public void calculateDRA(int playerNum, int scoreNum) {
        DRA = (TMG / scoreNum) * playerNum;
    }

    public void calculateMRA() {
        MRA = 0.7 * ERA + 0.3 * DRA;
    }

    public Double getTotalScore() {
        double totalScore = 0L;
        for (var s : scores) {
            totalScore += s;
        }
        return totalScore/1000000;
    }

    public UserMatchData(Integer id, String username) {
        this.id = id;
        this.username = username;
    }

    public Integer getWins() {
        return wins;
    }

    public void setWins(Integer wins) {
        this.wins = wins;
    }

    public Integer getLost() {
        return lost;
    }

    public void setLost(Integer lost) {
        this.lost = lost;
    }

    public List<Integer> getScores() {
        return scores;
    }

    public void setScores(List<Integer> scores) {
        this.scores = scores;
    }

    public String getTeam(){return team;}

    public void setTeam(String team) {
        this.team = team;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public void setMRA(Double MRA) {
        this.MRA = MRA;
    }

    public Double getMRA() {
        return MRA;
    }

    public Double getSERA() {
        return SERA;
    }

    public void setSERA(Double SERA) {
        this.SERA = SERA;
    }

    public Double getSDRA() {
        return SDRA;
    }

    public void setSDRA(Double SDRA) {
        this.SDRA = SDRA;
    }


    public void setERA_index(double ERA_index) {
        this.ERA_index = ERA_index;
    }

    public void setDRA_index(double DRA_index) {
        this.DRA_index = DRA_index;
    }

    public Rating getRating(){
        double sum = ERA_index + DRA_index;
        if (sum <= 0.33){
            return Rating.BC;
        } else if (sum <= 0.66){
            return Rating.CA;
        } else if (sum > 1.66){
            return Rating.FU;
        } else if (sum > 1.33){
            return Rating.NO;
        } else {
            if (ERA_index <= 0.33){
                if (DRA_index <= 0.66) return Rating.MF;
                else return Rating.SP;
            } else if (DRA_index <= 0.33){
                if (ERA_index <= 0.66) return Rating.WF;
                else return Rating.SP;
            } else if (ERA_index > 0.66) return Rating.SG;
            else if (DRA_index > 0.66) return Rating.GU;
            else return Rating.GE;

        }

    }

    public enum Rating{
        BC("BC", Color.makeRGB(254,246,103)),
        CA("CA", Color.makeRGB(240,148,80)),
        MF("MF", Color.makeRGB(48,181,115)),
        SP("SP", Color.makeRGB(170,212,110)),
        WF("WF", Color.makeRGB(49,68,150)),
        GE("GE", Color.makeRGB(180,180,180)),
        GU("GU", Color.makeRGB(62,188,239)),
        SU("SU", Color.makeRGB(106,80,154)),
        SG("SG", Color.makeRGB(236,107,158)),
        NO("NO", Color.makeRGB(234,107,72)),
        FU("FU", Color.makeRGB(150,0,20)),;

        String name;
        int color;

        Rating(String name, int color) {
            this.name = name;
            this.color = color;
        }
    }
}

package com.now.nowbot.model.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserMatchData {
    Integer id;
    String team;
    String username;
    //标准化的单场个人得分，即标准分 = score/TotalScore
    List<Double> RRAs = new ArrayList<>();
    //整场比赛的总标准分
    Double TMG;
    //场均标准分
    Double AMG;
    //AMG/Average(AMG) 场均标准分的相对值
    Double MQ;
    //(MQ - min(MQ))/(1 - min(MQ))
    Double MRA;
    //(TMG*playerNumber)/参赛人次
    Double MDRA;

    //与MRA,MDRA相同，范围扩大到正常联赛的多次match
    Double SRA;
    Double SDRA;

    public void calculateAMG(){
        TMG = 0d;
        for(Double RRA:RRAs)
            TMG+=RRA;
        AMG = TMG/RRAs.size();
    }

    public void calculateMQ(double averageAMG){
        MQ = AMG/averageAMG;
    }


    public void calculateMRA(double minMQ){
        MRA = (MQ - minMQ)/(1 - minMQ);
    }

    public void calculateMDRA(int playerNum, int scoreNum){
        MDRA = TMG*playerNum/scoreNum;
    }

    public UserMatchData(Integer id, String username) {
        this.id = id;
        this.username = username;
    }

    public String getTeam() {
        return team;
    }

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

    public Double getMRA() {
        return MRA;
    }

    public void setMRA(Double MRA) {
        this.MRA = MRA;
    }

    public Double getMDRA() {
        return MDRA;
    }

    public void setMDRA(Double MDRA) {
        this.MDRA = MDRA;
    }

    public Double getSRA() {
        return SRA;
    }

    public void setSRA(Double SRA) {
        this.SRA = SRA;
    }

    public Double getSDRA() {
        return SDRA;
    }

    public void setSDRA(Double SDRA) {
        this.SDRA = SDRA;
    }
}

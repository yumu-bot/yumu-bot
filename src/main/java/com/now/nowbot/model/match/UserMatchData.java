package com.now.nowbot.model.match;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.JsonData.OsuUser;
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
    //平均每局胜利分配 RWS v3.4添加
    List<Double> RWSs = new ArrayList<>();
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
    //RWS = average(win%)
    Double RWS;
    Double tRWS;

    //与MRA,MDRA相同，范围扩大到正常联赛的多次match
    Double SERA;
    Double SDRA;
    Double SMRA;

    //胜负场次
    Integer wins = 0;
    Integer lost = 0;
    OsuUser userData;

    double ERA_index;
    double DRA_index;
    double RWS_index;

    int indx;

    public void calculateAMG() {
        TMG = 0d;
        for (Double RRA : RRAs)
            TMG += RRA;
        AMG = TMG / RRAs.size();
    }

    public void calculateMQ(double averageAMG) {
        MQ = AMG / averageAMG;
    }

    public void calculateERA(double minMQ, double ScalingFactor) {
        ERA = (MQ - minMQ * ScalingFactor) / (1 - minMQ * ScalingFactor);
        //ERA = MQ;
        //v2.0 ERA放缩取消，现在ERA与MQ等同了
        //v3.2 ERA放缩加回，增加了缩放因子
    }

    public void calculateDRA(int playerNum, int scoreNum) {
        DRA = (TMG / scoreNum) * playerNum;
    }

    public void calculateMRA() {
        MRA = 0.7 * ERA + 0.3 * DRA;
    }

    public void calculateRWS(int roundNum) {
        tRWS = 0d;
        for (Double rRWS : RWSs) tRWS += rRWS;
        if (RWSs.size() == 0) RWS = 0d;
            else RWS = tRWS / roundNum;
    }

    public Double getTotalScore() {
        double totalScore = 0L;
        for (var s : scores) {
            totalScore += s;
        }
        return totalScore/1000000;
    }

    public UserMatchData(OsuUser userdata) {
        this.id = Math.toIntExact(userdata.getId());
        this.username = userdata.getUsername();
        this.userData = userdata;
    }

    public UserMatchData(int id, String name ) {
        this.id = id;
        this.username = name;
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

    public void setRWS_index(double RWS_index) {
        this.RWS_index = RWS_index;
    }

    public OsuUser getUserData() {
        return userData;
    }

    public void setUserData(OsuUser userData) {
        this.userData = userData;
    }

    public int getIndx() {
        return indx;
    }

    public void setIndx(int indx) {
        this.indx = indx;
    }

    public List<Double> getRWSs() {
        return RWSs;
    }

    public void setRWSs(List<Double> RWSs) {
        this.RWSs = RWSs;
    }

    public double getRWS() {
        return RWS;
    }

    public void setRWS(double RWS) {
        this.RWS = RWS;
    }

    public Rating getPlayerLabelV1(){
        if (ERA_index < 1f/6) {
            if (DRA_index < 1f/6) {
                return Rating.BC;
            } else if (DRA_index < 2f/6) {
                return Rating.CA;
            } else if (DRA_index < 4f/6) {
                return Rating.MF;
            } else {
                return Rating.SP;
            }
        } else if (ERA_index < 2f/6) {
            if (DRA_index < 2f/6) {
                return Rating.CA;
            } else if (DRA_index < 4f/6) {
                return Rating.MF;
            } else {
                return Rating.SP;
            }
        } else if (ERA_index < 4f/6) {
            if (DRA_index < 2f/6) {
                return Rating.WF;
            } else if (DRA_index < 4f/6) {
                return Rating.GE;
            } else {
                return Rating.GU;
            }
        } else if (ERA_index < 5f/6) {
            if (DRA_index < 2f/6) {
                return Rating.SU;
            } else if (DRA_index < 4f/6) {
                return Rating.SG;
            } else {
                return Rating.NO;
            }
        } else {
            if (DRA_index < 2f/6) {
                return Rating.SU;
            } else if (DRA_index < 4f/6) {
                return Rating.SG;
            } else if (DRA_index < 5f/6) {
                return Rating.NO;
            } else {
                return Rating.FU;
            }

        }
    }
    public Rating getPlayerLabelV2(){
        if (ERA_index < 1f/6 && DRA_index < 1f/6) {
            if (RWS_index < 1f/3) return Rating.SMA;
                else if (RWS_index < 2f/3) return Rating.CMA;
                else return Rating.IMA;
        }

        if (ERA_index >= 5f/6 && DRA_index >= 5f/6) {
            if (RWS_index < 1f/3) return Rating.LSS;
            else if (RWS_index < 2f/3) return Rating.LSP;
            else return Rating.BDT;
        }

        if (ERA_index < 1f/3 && DRA_index < 1f/3) {
            if (RWS_index < 1f/3) return Rating.IGE;
            else if (RWS_index < 2f/3) return Rating.AGE;
            else return Rating.EGE;
        }

        if (ERA_index < 1f/3 && DRA_index < 2f/3) {
            if (RWS_index < 1f/3) return Rating.EMF;
            else if (RWS_index < 2f/3) return Rating.RMF;
            else return Rating.SMF;
        }

        if (ERA_index < 1f/3 && DRA_index >= 2f/3) {
            if (RWS_index < 1f/3) return Rating.EAS;
            else if (RWS_index < 2f/3) return Rating.NAS;
            else return Rating.FAS;
        }

        if (ERA_index < 2f/3 && DRA_index < 1f/3) {
            if (RWS_index < 1f/3) return Rating.GCW;
            else if (RWS_index < 2f/3) return Rating.WCW;
            else return Rating.BCW;
        }

        if (ERA_index < 2f/3 && DRA_index < 2f/3) {
            if (RWS_index < 1f/3) return Rating.KPS;
            else if (RWS_index < 2f/3) return Rating.CMN;
            else return Rating.PSB;
        }

        if (ERA_index < 2f/3 && DRA_index >= 2f/3) {
            if (RWS_index < 1f/3) return Rating.MAC;
            else if (RWS_index < 2f/3) return Rating.MIC;
            else return Rating.FIG;
        }

        if (ERA_index >= 2f/3 && DRA_index < 1f/3) {
            if (RWS_index < 1f/3) return Rating.SAM;
            else if (RWS_index < 2f/3) return Rating.HAS;
            else return Rating.SIN;
        }

        if (ERA_index >= 2f/3 && DRA_index < 2f/3) {
            if (RWS_index < 1f/3) return Rating.ANI;
            else if (RWS_index < 2f/3) return Rating.MNI;
            else return Rating.LCS;
        }

        if (ERA_index >= 2f/3 && DRA_index >= 2f/3) {
            if (RWS_index < 1f/3) return Rating.LKD;
            else if (RWS_index < 2f/3) return Rating.QAP;
            else return Rating.BGN;
        }

        else {
            return Rating.CMN;
        }
    }


    public String getHeaderUrl(){
        if (userData == null) return NowbotConfig.BG_PATH + "avatar-guest.png";
        return "https://a.ppy.sh/"+id;
    }

    public String getCoverUrl(){
        if (userData != null)
            return userData.getCoverUrl();
        else return NowbotConfig.BG_PATH + "avatar-guest.png";
    }

    public enum Rating{
        // 这一段是 YMRA v1.2 更新内容
        BC("Big Carry", Color.makeRGB(254,246,103)), //大爹
        CA("Carry", Color.makeRGB(240,148,80)), //大哥
        MF("Main Force", Color.makeRGB(48,181,115)), //主力
        SP("Specialized",Color.makeRGB(170,212,110)), //专精
        WF("Work Force", Color.makeRGB(49,68,150)), //打工
        GE("General",Color.makeRGB(180,180,180)), //普通
        GU("Guest", Color.makeRGB(62,188,239)), //客串
        SU("Support",Color.makeRGB(106,80,154)), //抗压
        SG("Scapegoat", Color.makeRGB(236,107,158)), //背锅
        NO("Noob", Color.makeRGB(234,107,72)), //小弟
        FU("Futile", Color.makeRGB(150,0,20)), //炮灰

        SMA("Strongest Marshal", Color.makeRGB(254,246,103)),//最强元帅
        CMA("Competent Marshal", Color.makeRGB(254,246,103)),//称职元帅
        IMA("Indomitable Marshal", Color.makeRGB(254,246,103)),//不屈元帅
        IGE("Invincible General", Color.makeRGB(240,148,80)),//常胜将军
        AGE("Assiduous General", Color.makeRGB(240,148,80)),//勤奋将军
        EGE("Exhausted General", Color.makeRGB(240,148,80)),//尽力将军
        EMF("Effective Main Force", Color.makeRGB(48,181,115)),//突破主力
        RMF("Reliable Main Force", Color.makeRGB(48,181,115)),//可靠主力
        SMF("Staunch Main Force", Color.makeRGB(48,181,115)),//坚守主力
        EAS("Elite Assassin", Color.makeRGB(170,212,110)),//精锐刺客
        NAS("Normal Assassin", Color.makeRGB(170,212,110)),//普通刺客
        FAS("Fake Assassin", Color.makeRGB(170,212,110)),//冒牌刺客
        GCW("Gold Collar Worker", Color.makeRGB(49,68,150)),//金领工人
        WCW("White Collar Worker", Color.makeRGB(49,68,150)),//白领工人
        BCW("Blue Collar Worker", Color.makeRGB(49,68,150)),//蓝领工人
        KPS("Key Person", Color.makeRGB(180,180,180)),//关键人
        CMN("Common Man", Color.makeRGB(180,180,180)),//普通人
        PSB("Passer-by", Color.makeRGB(180,180,180)),//路人甲
        MAC("Major Character", Color.makeRGB(62,188,239)),//主要角色
        MIC("Minor Character", Color.makeRGB(62,188,239)),//次要角色
        FIG("Figurant", Color.makeRGB(62,188,239)),//群众演员
        SAM("Stable as Mountain", Color.makeRGB(106,80,154)),//稳如泰山
        HAS("Hard as Stone", Color.makeRGB(106,80,154)),//坚若磐石
        SIN("Seriously Injured", Color.makeRGB(106,80,154)),//伤痕累累
        ANI("Advanced Ninja", Color.makeRGB(236,107,158)),//上等忍者
        MNI("Mediocre Ninja", Color.makeRGB(236,107,158)),//普通忍者
        LCS("Lower-class", Color.makeRGB(236,107,158)),//不入流
        LKD("Lucky Dog", Color.makeRGB(234,107,72)),//幸运儿
        QAP("Qualified Apprentice", Color.makeRGB(234,107,72)),//合格学徒
        BGN("Beginner", Color.makeRGB(234,107,72)),//初学者
        LSS("Life-saving Straw", Color.makeRGB(150,0,20)),//救命稻草
        LSP("Little Spark", Color.makeRGB(150,0,20)),//点点星火
        BDT("Burnt Dust", Color.makeRGB(150,0,20)),//湮灭尘埃

        ;

        // 这一段是 YMRA v3.4 更新内容

        public final String name;
        public final int color;

        Rating(String name, int color) {
            this.name = name;
            this.color = color;
        }
    }
}

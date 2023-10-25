package com.now.nowbot.model.recommended;

import com.now.nowbot.model.JsonData.BeatMap;

public class RecommendedMap {
    Long bid;
    String bgUrl;
    Float Star;
    Float OD;
    Float AR;
    Float HP;
    Float CS;
    Float bpm;

    Integer Objects;
    Integer length;

    String title;
    String version;

    String introduction;

    Float minPlayerPP;
    Float maxPlayerPP;
    private RecommendedMap(){}

    public static RecommendedMap getMap(BeatMap mapInfo){
        var data = new RecommendedMap();
        data.bid = mapInfo.getBID();
        data.bgUrl = mapInfo.getBeatMapSet().getCovers().getCover2x();
        data.Star = mapInfo.getStarRating().floatValue();
        data.OD = mapInfo.getOD();
        data.AR = mapInfo.getAR();
        data.HP = mapInfo.getHP();
        data.CS = mapInfo.getCS();
        data.bpm = mapInfo.getBPM();
        data.Objects = mapInfo.getSpinners() + mapInfo.getCircles() + mapInfo.getSliders();
        data.length = mapInfo.getTotalLength();
        data.title = mapInfo.getBeatMapSet().getTitleUTF();
        data.version = mapInfo.getVersion();
        data.setPlayerPP();
        return data;
    }

    void setPlayerPP(){
        if (Star < 1.5) {
            minPlayerPP = 0f;
            maxPlayerPP = 500f;
        } else if (Star < 3) {
            minPlayerPP = 300f;
            maxPlayerPP = 800f;
        } else if (Star < 4.5) {
            minPlayerPP = 300f;
            maxPlayerPP = 1000f;
        } else if (Star < 5.3) {
            minPlayerPP = 1000f;
            maxPlayerPP = 2500f;
        } else if (Star < 5.9) {
            minPlayerPP = 2000f;
            maxPlayerPP = 3000f;
        } else if (Star < 6.4) {
            minPlayerPP = 2500f;
            maxPlayerPP = 5000f;
        } else  {
            minPlayerPP = 3500f;
            maxPlayerPP = -1f;
        }
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public Long getBid() {
        return bid;
    }

    public String getBgUrl() {
        return bgUrl;
    }

    public Float getStar() {
        return Star;
    }

    public Float getOD() {
        return OD;
    }

    public Float getAR() {
        return AR;
    }

    public Float getHP() {
        return HP;
    }

    public Float getCS() {
        return CS;
    }

    public Float getBpm() {
        return bpm;
    }

    public Integer getObjects() {
        return Objects;
    }

    public Integer getLength() {
        return length;
    }

    public String getTitle() {
        return title;
    }

    public String getVersion() {
        return version;
    }

    public String getIntroduction() {
        return introduction;
    }

    public Float getMinPlayerPP() {
        return minPlayerPP;
    }

    public Float getMaxPlayerPP() {
        return maxPlayerPP;
    }
}


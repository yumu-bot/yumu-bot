package com.now.nowbot.model.recommended;

import java.util.List;

public class RecommendedMapPool {
    Long recommenderOsuId;
    String recommenderName;
    String introduction;

    Float minPlayerPP;
    Float maxPlayerPP;

    List<RecommendedMap> maps;
    List<RecommendedPass> checks;
}

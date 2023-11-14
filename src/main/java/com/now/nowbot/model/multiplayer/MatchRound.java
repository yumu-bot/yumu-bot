package com.now.nowbot.model.multiplayer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.score.MPScore;

import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchRound {


    @JsonProperty("beatmap_id")
    Long bid;
    Integer id;
    @JsonProperty("start_time")
    OffsetDateTime startTime;
    @JsonProperty("end_time")
    OffsetDateTime endTime;
    String mode;
    @JsonProperty("mod_int")
    Integer modInt;
    @JsonProperty("scoring_type")
    String scoringType;
    @JsonProperty("team_type")
    String teamType;
    String[] mods;
    BeatMap beatmap;
    @JsonProperty("scores")
    List<MPScore> scoreInfoList;
}

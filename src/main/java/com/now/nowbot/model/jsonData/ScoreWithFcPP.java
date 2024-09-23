package com.now.nowbot.model.jsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.beans.BeanUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ScoreWithFcPP extends Score {
    Float   fcPP;
    Integer index;
    Integer indexAfter;

    public static ScoreWithFcPP copyOf(Score score) {
        var result = new ScoreWithFcPP();
        BeanUtils.copyProperties(score, result);
        return result;
    }

    public Float getFcPP() {
        return fcPP;
    }

    public void setFcPP(Float fcPP) {
        this.fcPP = fcPP;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Integer getIndexAfter() {
        return indexAfter;
    }

    public void setIndexAfter(Integer indexAfter) {
        this.indexAfter = indexAfter;
    }
}

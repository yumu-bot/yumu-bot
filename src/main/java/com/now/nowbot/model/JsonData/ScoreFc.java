package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.beans.BeanUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ScoreFc extends Score {
    // 专用的扩展属性用子类去继承, 不然父类会越来越大
    Float fcPP;

    public static ScoreFc copyOf(Score score) {
        var result = new ScoreFc();
        BeanUtils.copyProperties(score, result);
        return result;
    }

    public Float getFcPP() {
        return fcPP;
    }

    public void setFcPP(Float fcPP) {
        this.fcPP = fcPP;
    }

}

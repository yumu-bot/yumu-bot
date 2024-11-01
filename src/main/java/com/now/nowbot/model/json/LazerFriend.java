package com.now.nowbot.model.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class LazerFriend {
    @JsonProperty("target_id")
    Long targetId;

    @JsonProperty("relation_type")
    String relationType;

    @JsonProperty("mutual")
    Boolean isMutual;

    @JsonProperty("target")
    MicroUser target;

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public Boolean getMutual() {
        return isMutual;
    }

    public void setMutual(Boolean mutual) {
        isMutual = mutual;
    }

    public MicroUser getTarget() {
        target.isMutual = isMutual;
        return target;
    }

    public void setTarget(MicroUser target) {
        this.target = target;
    }
}

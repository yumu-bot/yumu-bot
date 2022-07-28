package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Cover {
    @JsonProperty("custom_url")
    public String custom;
    public String url;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Cover{");
        sb.append("custom='").append(custom).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

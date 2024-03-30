package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Cover {
    @JsonProperty("custom_url")
    public String custom;

    public String url;

    @Nullable
    @JsonProperty("id")
    public void setID(@Nullable String idStr) {
        if (StringUtils.hasText(idStr)) {
            try {
                id = Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                id = null;
            }
        } else {
            id = null;
        }
    }

    @JsonIgnoreProperties
    public Integer id;

    public Cover(){}

    public Cover(String custom, String url, Integer id) {
        this.custom = custom;
        this.url = url;
        this.id = id;
    }

    @Override
    public String toString() {
        return STR."Cover{custom='\{custom}\{'\''}, url='\{url}\{'\''}, id='\{id}\{'\''}\{'}'}";
    }
}

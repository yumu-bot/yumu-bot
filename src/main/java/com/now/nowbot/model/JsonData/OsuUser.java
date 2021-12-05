package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OsuUser {
    @JsonIgnore
    String  countryCode;
    @JsonIgnore
    String  countryName;
    @JsonProperty("country")
    void setCountry(Map<String,String> country){
        countryCode = country.get("code");
        countryName = country.get("name");
    }
}

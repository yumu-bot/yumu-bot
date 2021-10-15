package com.now.nowbot.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.entity.OsuUserLite;
import com.now.nowbot.mapper.OsuUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OsuUserDao {
    @Autowired
    OsuUserMapper osuUserMapper;
    public OsuUserLite parsLite(JsonNode node){
        var osuUser = new OsuUserLite();
        osuUser.setOsuID(node.get("id").asInt());
        osuUser.setUserName(node.get("username").asText());
        osuUser = osuUserMapper.save(osuUser);
        return osuUser;
    }
}

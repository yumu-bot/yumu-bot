package com.now.nowbot.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.entity.PPPLite;
import com.now.nowbot.mapper.PPPlusMapper;
import com.now.nowbot.model.PPPlusObject;
import com.now.nowbot.service.OsuGetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Component
public class PPPlusDao {
    @Autowired
    PPPlusMapper ppPlusMapper;
    @Autowired
    OsuGetService osuGetService;

    public PPPlusObject save(JsonNode data){
        var ppPlusObject = new PPPlusObject();
        ppPlusObject.setTime(LocalDateTime.now());
        ppPlusObject.setUid(data.get("UserID").asLong());
        ppPlusObject.setName(data.get("UserName").asText());
        ppPlusObject.setTotal(data.get("PerformanceTotal").asDouble());
        ppPlusObject.setJunp(data.get("JumpAimTotal").asDouble());
        ppPlusObject.setFlow(data.get("FlowAimTotal").asDouble());
        ppPlusObject.setAcc(data.get("AccuracyTotal").asDouble());
        ppPlusObject.setSta(data.get("StaminaTotal").asDouble());
        ppPlusObject.setSpd(data.get("SpeedTotal").asDouble());
        ppPlusObject.setPre(data.get("PrecisionTotal").asDouble());
        ppPlusMapper.save(parse(ppPlusObject));
        return ppPlusObject;
    }
    public PPPlusObject getobject(String uid){
        var p = ppPlusMapper.getFirstByUserIdOrderByDateDesc(Long.parseLong(uid));
        if (p == null){
            var node = osuGetService.ppPlus(uid);
            return save(node);
        }
        return parse(p);
    }
    PPPLite parse(PPPlusObject obj){
        return new PPPLite(obj.getUid(), LocalDateTime.now(), obj.getTotal(), obj.getJunp(), obj.getFlow(), obj.getAcc(), obj.getSta(), obj.getSpd(), obj.getPre());
    }
    PPPlusObject parse(PPPLite pppLite){
        return new PPPlusObject(pppLite.getId(),pppLite.getDate(), pppLite.getTotal(), pppLite.getJunp(), pppLite.getFlow(), pppLite.getAcc(), pppLite.getSta(), pppLite.getSpd(), pppLite.getPre());
    }
    public float[] ppsize(PPPlusObject obj){
        float[] data = new float[6];
        data[0] = obj.getJunp().floatValue();
        data[1] = obj.getFlow().floatValue();
        data[2] = obj.getAcc().floatValue();
        data[3] = obj.getSta().floatValue();
        data[4] = obj.getSpd().floatValue();
        data[5] = obj.getPre().floatValue();
        return osuGetService.ppPlus(data);
    }
}

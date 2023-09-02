package com.now.nowbot.dao;

import com.now.nowbot.entity.PPPLite;
import com.now.nowbot.mapper.PPPlusMapper;
import com.now.nowbot.model.JsonData.PPPlus;
import com.now.nowbot.service.OsuGetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Component
public class PPPlusDao {
    Logger log = LoggerFactory.getLogger("PP+Dao");
    PPPlusMapper ppPlusMapper;
    OsuGetService osuGetService;
    @Autowired
    public PPPlusDao(PPPlusMapper ppPlusMapper, OsuGetService osuGetService){
        this.ppPlusMapper = ppPlusMapper;
        this.osuGetService = osuGetService;
    }

    public PPPlus getobject(String uid){
        var p = ppPlusMapper.getFirstByUserIdOrderByDateDesc(Long.parseLong(uid));
        if (p == null){
            var node = osuGetService.ppPlus(uid);
            ppPlusMapper.save(parse(node));
            return node;
        }
        return parse(p);
    }

    PPPLite parse(PPPlus obj){
        return new PPPLite(obj.getUid(), LocalDateTime.now(), obj.getTotal(), obj.getJump(), obj.getFlow(), obj.getAcc(), obj.getSta(), obj.getSpd(), obj.getPre());
    }
    PPPlus parse(PPPLite pppLite){
        return new PPPlus(pppLite.getId(),pppLite.getDate(), pppLite.getTotal(), pppLite.getJump(), pppLite.getFlow(), pppLite.getAcc(), pppLite.getSta(), pppLite.getSpd(), pppLite.getPre());
    }

    public float[] ppsize(PPPlus obj){
        float[] data = new float[6];
        data[0] = obj.getJump().floatValue();
        data[1] = obj.getFlow().floatValue();
        data[2] = obj.getAcc().floatValue();
        data[3] = obj.getSta().floatValue();
        data[4] = obj.getSpd().floatValue();
        data[5] = obj.getPre().floatValue();
        return osuGetService.ppPlus(data);
    }
}

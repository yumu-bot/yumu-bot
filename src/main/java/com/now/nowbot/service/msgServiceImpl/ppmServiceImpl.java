package com.now.nowbot.service.msgServiceImpl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.entity.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ppmServiceImpl extends MessageService{
    public ppmServiceImpl() {
        super("ppm");
    }
    @Autowired
    OsuGetService osuGetService;

    @Override
    public void handleMsg(MessageEvent event) {
        Contact from;
        if(event instanceof GroupMessageEvent) {
            from = ((GroupMessageEvent) event).getGroup();
        }else {
            from = event.getSender();
        }

        BinUser user = BindingUtil.readUser(event.getSender().getId());
        if (user == null){
            from.sendMessage("您未绑定，请绑定后使用");
        }

        var userdate = osuGetService.getPlayerOsuInfo(user);
        var bpdate = osuGetService.getOsuBestMap(user,0,100);
        dates userinfo= new dates(userdate, bpdate);

        double fa = (userinfo.acc<0.6D?0:(userinfo.acc-0.6)*Math.pow(2.5D,1.776D));
        double ptt;
    }
}
class dates{
    float ppv0=0;
    float ppv45=0;
    float ppv90=0;
    float accv0=0;
    float accv45=0;
    float accv90=0;
    long lengv0=0;
    long lengv45=0;
    long lengv90=0;
    double bpp=0;
    int xd=0;
    int xc=0;
    int xb=0;
    int xa=0;
    int xs=0;
    int xx=0;
    int notfc=0;
    String name;
    float pp ;
    float acc;
    int level;
    int rank ;
    int combo;
    long thit;
    long pcont;
    long ptime;
    dates(JSONObject prd, JSONArray prbp){

        for (int j = 0; j < prbp.size(); j++) {
            var jsb = prbp.getJSONObject(j);
            bpp += jsb.getDoubleValue("pp")*Math.pow(0.95d,j);
            if (jsb.getString("rank").startsWith("D")) xd++;
            if (jsb.getString("rank").startsWith("C")) xc++;
            if (jsb.getString("rank").startsWith("B")) xb++;
            if (jsb.getString("rank").startsWith("A")) xa++;
            if (jsb.getString("rank").startsWith("S")) xs++;
            if (jsb.getString("rank").startsWith("X")) xx++;
            if(!jsb.getBoolean("perfect")) notfc++;
            if(j < 10){
                ppv0 += jsb.getFloatValue("pp");
                accv0 += jsb.getFloatValue("accuracy");
                lengv0 += jsb.getJSONObject("beatmap").getFloatValue("total_length");
            }else if(j>=45 && j<55){
                ppv45 += jsb.getFloatValue("pp");
                accv45 += jsb.getFloatValue("accuracy");
                lengv45 += jsb.getJSONObject("beatmap").getFloatValue("total_length");
            }else if(j>=90){
                ppv90 += jsb.getFloatValue("pp");
                accv90 += jsb.getFloatValue("accuracy");
                lengv90 += jsb.getJSONObject("beatmap").getFloatValue("total_length");
            }
        }
        ppv0 /= 10;
        ppv45 /= 10;
        ppv90 /= 10;
        accv0 /= 10;
        accv45 /= 10;
        accv90 /= 10;
        lengv0 /= 10;
        lengv45 /= 10;
        lengv90 /= 10;
        name = prd.getString("username").replace(' ','_');
        pp = prd.getJSONObject("statistics").getFloatValue("pp");
        acc = prd.getJSONObject("statistics").getFloatValue("hit_accuracy");
        level = prd.getJSONObject("statistics").getJSONObject("level").getIntValue("current");
        rank = prd.getJSONObject("statistics").getIntValue("global_rank");
        combo = prd.getJSONObject("statistics").getIntValue("maximum_combo");
        thit = prd.getJSONObject("statistics").getLongValue("total_hits");
        pcont = prd.getJSONObject("statistics").getLongValue("play_count");
        ptime = prd.getJSONObject("statistics").getLongValue("play_time");
    }
}
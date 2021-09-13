package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.Ymp.Ymp;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("ymp")
public class YmpService implements MessageService{

    @Autowired
    OsuGetService osuGetService;
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        String name = matcher.group("name");
        JSONObject date;
        if(name!=null && !name.equals("")) {
            int id = osuGetService.getOsuId(name);
            var dx = osuGetService.getOsuAllRecent(id,0,1);
            if(dx.size()==0){
                throw new TipsException("24h内无记录");
            }
            date = dx.getJSONObject(0);
        }else {
            BinUser user = BindingUtil.readUser(event.getSender().getId());
            var dx = osuGetService.getOsuAllRecent(user,0,1);
            if(dx.size()==0){
                throw new TipsException("24h内无记录");
            }
            date = dx.getJSONObject(0);
        }
        var d = Ymp.getInstance(date);
        from.sendMessage(d.getOut());
    }
}

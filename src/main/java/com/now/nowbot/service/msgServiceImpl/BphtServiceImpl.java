package com.now.nowbot.service.msgServiceImpl;

import com.alibaba.fastjson.JSONArray;
import com.now.nowbot.entity.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BphtServiceImpl extends MessageService{
    @Autowired
    OsuGetService osuGetService;

    public BphtServiceImpl() {
        super("bpht");
    }

    @Override
    public void handleMsg(MessageEvent event) {
        Contact from;
        if(event instanceof GroupMessageEvent) {
            from = ((GroupMessageEvent) event).getGroup();
        }else {
            from = event.getSender();
        }


        BinUser nu = null;
        Pattern p = Pattern.compile(getKey()+"\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)?");
        Matcher m = p.matcher(event.getMessage().contentToString());
        if(m.find() && m.group("name") != null && !m.group("name").trim().equals("")){
                String name = m.group("name").trim();
                try {
                    nu = new BinUser();
                    nu.setOsuID(osuGetService.getOsuId(name));
                    nu.setOsuName(name);
                } catch (Exception e) {
                    from.sendMessage(e.getMessage());
                    return;
                }
        }else {
            nu = BindingUtil.readUser(event.getSender().getId());
            if(nu == null){
                from.sendMessage("您未绑定，请发送bind绑定");
                return;
            }
            if(nu.getOsuID() == 0){
                osuGetService.getPlayerOsuInfo(nu);
            }
        }
        JSONArray Bps;
        if(nu != null && nu.getAccessToken()!= null){
            try {
                Bps = osuGetService.getOsuBestMap(nu, 0,100);
            } catch (HttpClientErrorException e) {
                from.sendMessage("您的令牌已过期,请私聊bot bind重新绑定！");
                return;
            }
        }else {
            Bps = osuGetService.getOsuBestMap(nu.getOsuID(),0,100);
        }
        if(Bps.size() != 100){
            from.sendMessage(new At(from.getId()).plus("您的BP尚未填满，请打完后尝试"));
            return;
        }

        var dtbf = new StringBuffer(nu.getOsuName()).append("[std]\n");
        double pp = 0;
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        for (int i = 0; i < Bps.size(); i++) {
            var jsb = Bps.getJSONObject(i);

            if(i<5 || i>94){
                dtbf.append("#")
                        .append(i+1)
                        .append(' ')
                        .append(decimalFormat.format(jsb.getFloatValue("pp")))
                        .append(' ')
                        .append(decimalFormat.format(100*(float)(50*jsb.getJSONObject("statistics").getIntValue("count_50") + 100 * jsb.getJSONObject("statistics").getIntValue("count_100") + 300 * jsb.getJSONObject("statistics").getIntValue("count_300")) / 300 / (jsb.getJSONObject("statistics").getIntValue("count_miss") + jsb.getJSONObject("statistics").getIntValue("count_50")+ jsb.getJSONObject("statistics").getIntValue("count_100") + jsb.getJSONObject("statistics").getIntValue("count_300"))))
                        .append('%')
                        .append(' ')
                        .append(jsb.getString("rank"));
                if(jsb.getJSONArray("mods").size() > 0){
                    for (int j = 0; j < jsb.getJSONArray("mods").size(); j++) {
                        dtbf.append(' ').append(jsb.getJSONArray("mods").getString(j));
                    }
                }
                dtbf.append('\n');
            }else if(i == 60) {
                dtbf.append("-------分割线-------\n");
            }
            pp += jsb.getFloatValue("pp");
        }

        dtbf.append("您的BP1与BP100的差为").append(decimalFormat.format(Bps.getJSONObject(0).getFloatValue("pp")-Bps.getJSONObject(99).getFloatValue("pp"))).append("\n");
        dtbf.append("您的平均BP为").append(decimalFormat.format(pp/100));

        from.sendMessage(dtbf.toString());
    }
}

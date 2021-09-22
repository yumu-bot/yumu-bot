package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.regex.Matcher;

@Service("bpht")
public class bphtService implements MessageService{
    @Autowired
    OsuGetService osuGetService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        BinUser nu = null;
        if(matcher.group("name") != null && !matcher.group("name").trim().equals("")){
            //查询其他人 bpht [name]
            String name = matcher.group("name").trim();
                nu = new BinUser();
                //构建只有 name + id 的对象
                nu.setOsuID(osuGetService.getOsuId(name));
                nu.setOsuName(name);
        }else {
            //处理没有参数的情况 查询自身
            nu = BindingUtil.readUser(event.getSender().getId());
        }
        //bp列表
        JSONArray Bps;
        //acc计算器  区分不同模式的计算器
        AccCoun accCoun;
        //分别处理mode
        var mode = matcher.group("mode")==null?"null":matcher.group("mode").toLowerCase();
        switch (mode){
            case"null":
            case"osu":
            case"o":
            case"0":
            default:{
                //getAccessToken()判断token是否存在,未绑定为null 使用本机AccessToken
                if(nu.getAccessToken() != null){
                    Bps = osuGetService.getOsuBestMap(nu, 0,100);

                }else {
                    Bps = osuGetService.getOsuBestMap(nu.getOsuID(),0,100);
                }
                mode = "std";
                //使用std计算器
                accCoun = AccCoun.OSU;
            }break;
            case"taiko":
            case"t":
            case"1":{
                if(nu.getAccessToken() != null){
                    Bps = osuGetService.getTaikoBestMap(nu, 0,100);
                }else {
                    Bps = osuGetService.getTaikoBestMap(nu.getOsuID(),0,100);
                }
                mode = "taiko";
                accCoun = AccCoun.TAIKO;
            }break;
            case"catch":
            case"c":
            case"2":{
                if(nu.getAccessToken() != null){
                    Bps = osuGetService.getCatchBestMap(nu, 0,100);
                }else {
                    Bps = osuGetService.getCatchBestMap(nu.getOsuID(),0,100);
                }
                mode = "ctb";
                accCoun = AccCoun.CATCH;
            }break;
            case"mania":
            case"m":
            case"3":{
                if(nu.getAccessToken() != null){
                    Bps = osuGetService.getBestMap(nu,"mania", 0,100);
                }else {
                    Bps = osuGetService.getBestMap(nu.getOsuID(),"mania",0,100);
                }
                mode = "mania";
                accCoun = AccCoun.MANIA;
            }break;
        }
        //...
        if(Bps.size() != 100){
            from.sendMessage(new At(event.getSender().getId()).plus("您的BP尚未填满，请打完后尝试"));
            return;
        }
        //生成结果
        var dtbf = new StringBuffer(nu.getOsuName()).append('[').append(mode).append(']').append('\n');
        double pp = 0;
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        for (int i = 0; i < Bps.size(); i++) {
            var jsb = Bps.getJSONObject(i);
            //显示前五跟后五的数据
            if(i<5 || i>94){
                dtbf.append("#")
                        .append(i+1)
                        .append(' ')
                        .append(decimalFormat.format(jsb.getFloatValue("pp")))
                        .append(' ')
                        .append(decimalFormat.format(100*jsb.getDoubleValue("accuracy")))
//                        .append(decimalFormat.format(accCoun.getAcc(jsb)))
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
interface AccCoun {
    static AccCoun OSU = new OsuAcc();
    static AccCoun TAIKO = new TaikoAcc();
    static AccCoun CATCH = new CatchAcc();
    static AccCoun MANIA = new ManiaAcc();
    public double getAcc(JSONObject score);
}
class OsuAcc implements AccCoun{
    @Override
    public double getAcc(JSONObject score) {
        var n = score.getJSONObject("statistics");
        return 100d * (50*n.getIntValue("count_50") + 100 * n.getIntValue("count_100") + 300 * n.getIntValue("count_300")) / 300 / (n.getIntValue("count_miss") + n.getIntValue("count_50")+ n.getIntValue("count_100") + n.getIntValue("count_300"));
    }
}
class TaikoAcc implements AccCoun{
    @Override
    public double getAcc(JSONObject score) {
        var n = score.getJSONObject("statistics");
        return 100d * (0.5*n.getIntValue("count_100")+n.getIntValue("count_300"))/(n.getIntValue("count_50")+n.getIntValue("count_100")+n.getIntValue("count_300"));
    }
}
class CatchAcc implements AccCoun{
    @Override
    public double getAcc(JSONObject score) {
        var n = score.getJSONObject("statistics");
        return 100d*(n.getIntValue("count_50")+n.getIntValue("count_100")+n.getIntValue("count_300"))/(n.getIntValue("count_miss")+n.getIntValue("count_50")+n.getIntValue("count_100")+n.getIntValue("count_300")+n.getIntValue("count_katu"));
    }
}
class ManiaAcc implements AccCoun{
    @Override
    public double getAcc(JSONObject score) {
        var n = score.getJSONObject("statistics");
        return 100d*(n.getIntValue("count_50")*50+n.getIntValue("count_100")*100+n.getIntValue("count_katu")*200+(n.getIntValue("count_300")+n.getIntValue("count_geki"))*300)/(300*(n.getIntValue("count_miss")+n.getIntValue("count_50")+n.getIntValue("count_100")+n.getIntValue("count_300")+n.getIntValue("count_katu")+n.getIntValue("count_geki")));
    }
}



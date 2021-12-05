package com.now.nowbot.service.MessageService;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.BpInfo;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;

@Service("bpht")
public class BphtService implements MessageService{
    @Autowired
    OsuGetService osuGetService;
    class intValue{
        int value = 0;
        public intValue add() {
            value++;
            return this;
        }
        public int value() {
            return value;
        }
    }
    @Override
    @CheckPermission()
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
        List<BpInfo> Bps;
        //分别处理mode
        var mode = OsuMode.getMode(matcher.group("mode"));
        TreeMap<String, intValue> modeSum = new TreeMap<>();
        switch (mode){
            default://todo 获取账号默认模式
                mode = OsuMode.OSU;
            case OSU: {
                //getAccessToken()判断token是否存在,未绑定为null 使用本机AccessToken
                if(nu.getAccessToken() != null){
                    Bps = osuGetService.getOsuBestMapNew(nu, 0,100);

                }else {
                    Bps = osuGetService.getOsuBestMapNew(nu.getOsuID(),0,100);
                }
            }break;
            case TAIKO:{
                if(nu.getAccessToken() != null){
                    Bps = osuGetService.getTaikoBestMapNew(nu,  0, 100);
                }else {
                    Bps = osuGetService.getTaikoBestMapNew(nu.getOsuID(),  0, 100);
                }
            }break;
            case CATCH:{
                if(nu.getAccessToken() != null){
                    Bps = osuGetService.getCatchBestMapNew(nu, 0,100);
                }else {
                    Bps = osuGetService.getCatchBestMapNew(nu.getOsuID(), 0,100);
                }
            }break;
            case MANIA:{
                if(nu.getAccessToken() != null){
                    Bps = osuGetService.getManiaBestMapNew(nu, 0,100);
                }else {
                    Bps = osuGetService.getManiaBestMapNew(nu.getOsuID(),0,100);
                }
            }break;
        }
        //...
        //生成结果
        var dtbf = new StringBuffer(nu.getOsuName()).append('[').append(mode).append(']').append('\n');
        double allPp = 0;
        int sSum = 0;
        int xSum = 0;
        int fcSum = 0;
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        for (int i = 0; i < Bps.size(); i++) {
            var jsb = Bps.get(i);
            //显示前五跟后五的数据
            if(i<5 || i>Bps.size() - 5){
                dtbf.append("#")
                        .append(i+1)
                        .append(' ')
                        .append(decimalFormat.format(jsb.getPp()))
                        .append(' ')
                        .append(decimalFormat.format(100*jsb.getAccuracy()))
//                        .append(decimalFormat.format(accCoun.getAcc(jsb)))
                        .append('%')
                        .append(' ')
                        .append(jsb.getRank());
                if(jsb.getMods().size() > 0){
                    for (int j = 0; j < jsb.getMods().size(); j++) {
                        dtbf.append(' ').append(jsb.getMods().get(j));
                    }
                }
                dtbf.append('\n');
            }else if(i == 60) {
                dtbf.append("-------分割线-------\n");
            }
            allPp += jsb.getPp();
            if (jsb.getMods().size() > 0){
                for (int j = 0; j < jsb.getMods().size(); j++) {
                    String mod = jsb.getMods().get(j);
                    if (modeSum.get(mod) == null)  modeSum.put(mod, new intValue()); else modeSum.get(mod).add();
                }
            }
            if (jsb.getRank().contains("S")) sSum++;
            if (jsb.getRank().contains("X")) {sSum++; xSum++;}
            if (jsb.getPerfect()) fcSum++;
        }
        dtbf.append("累计mod有:\n");
        modeSum.forEach((mod, sum)-> dtbf.append(mod).append(' ').append(sum.value).append(';'));
        dtbf.append("\n您bp中S rank及以上有").append(sSum).append("个,达到满cb的fc有").append(fcSum).append('个');
        if (xSum != 0) dtbf.append(",其中SS数量有").append(xSum).append('个');
        dtbf.append("\n您的BP1与BP100的差为").append(decimalFormat.format(Bps.get(0).getPp()-Bps.get(Bps.size()-1).getPp()));
        dtbf.append("\n您的平均BP为").append(decimalFormat.format(allPp/Bps.size()));

        from.sendMessage(dtbf.toString());
    }
}



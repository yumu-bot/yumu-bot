package com.now.nowbot.service.MessageService;

import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.jetbrains.skija.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;

@Service("bpht")
public class BphtService implements MessageService{
    private static final int FONT_SIZE = 30;
    OsuGetService osuGetService;
    Font font;

    @Autowired
    public BphtService(OsuGetService osuGetService){
        this.osuGetService = osuGetService;
    }
    class intValue{
        int value = 1;
        public intValue add() {
            value++;
            return this;
        }
        public int value() {
            return value;
        }
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        BinUser nu = null;
        At at = QQMsgUtil.getType(event.getMessage(), At.class);
        // 是否为绑定用户
        boolean isBind = true;
        if (at != null) {
            nu = BindingUtil.readUser(at.getTarget());
        } if(matcher.group("name") != null && !matcher.group("name").trim().equals("")){
            //查询其他人 bpht [name]
            String name = matcher.group("name").trim();
                nu = new BinUser();
                //构建只有 name + id 的对象
                nu.setOsuID(osuGetService.getOsuId(name));
                nu.setOsuName(name);
                isBind = false;
        } else {
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
                if(isBind){
                    Bps = osuGetService.getOsuBestPerformance(nu, 0,100);

                }else {
                    Bps = osuGetService.getOsuBestPerformance(nu.getOsuID().intValue(),0,100);
                }
            }break;
            case TAIKO:{
                if(isBind){
                    Bps = osuGetService.getTaikoBestPerformance(nu,  0, 100);
                }else {
                    Bps = osuGetService.getTaikoBestPerformance(nu.getOsuID().intValue(),  0, 100);
                }
            }break;
            case CATCH:{
                if(isBind){
                    Bps = osuGetService.getCatchBestPerformance(nu, 0,100);
                }else {
                    Bps = osuGetService.getCatchBestPerformance(nu.getOsuID().intValue(), 0,100);
                }
            }break;
            case MANIA:{
                if(isBind){
                    Bps = osuGetService.getManiaBestPerformance(nu, 0,100);
                }else {
                    Bps = osuGetService.getManiaBestPerformance(nu.getOsuID().intValue(),0,100);
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
            if (jsb.isPerfect()) fcSum++;
        }
        dtbf.append("累计mod有:\n");
        modeSum.forEach((mod, sum)-> dtbf.append(mod).append(' ').append(sum.value).append(';'));
        dtbf.append("\n您bp中S rank及以上有").append(sSum).append("个\n达到满cb的fc有").append(fcSum).append('个');
        if (xSum != 0) dtbf.append("\n其中SS数量有").append(xSum).append('个');
        dtbf.append("\n您的BP1与BP100的差为").append(decimalFormat.format(Bps.get(0).getPp()-Bps.get(Bps.size()-1).getPp()));
        dtbf.append("\n您的平均BP为").append(decimalFormat.format(allPp/Bps.size()));

        var allstr = dtbf.toString().split("\n");
        TextLine[] lines = new TextLine[allstr.length];
        float maxWidth = 0;
        for (int i = 0; i < allstr.length; i++) {
            lines[i] = TextLine.make(allstr[i], getFont());
            if (maxWidth < lines[i].getWidth()){
                maxWidth = lines[i].getWidth();
            }
        }
        int w = (int)maxWidth+50;
        int h = (int)((lines.length+1)*lines[0].getHeight())+50;
        Surface surface = Surface.makeRasterN32Premul((int)maxWidth+50, h);
        Shader shader = Shader.makeLinearGradient(0,0,0,h, SkiaUtil.getRandomColors());
        try (surface;shader){
            var canvas = surface.getCanvas();
            canvas.clear(Color.makeRGB(38,51,57));
//            canvas.drawRect(Rect.makeWH(w,h),new Paint().setShader(shader));
            canvas.translate(25,40);
            for (TextLine line : lines) {
                canvas.drawTextLine(line, 0, line.getCapHeight() + FONT_SIZE * 0.2f, new Paint().setColor(SkiaUtil.getRandomColor()));
                canvas.translate(0, line.getHeight());
            }
        QQMsgUtil.sendImage(from, surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG,70).getBytes());
        }finally {
            for (var line:lines){
                line.close();
            }
        }
//        from.sendMessage(dtbf.toString());
    }

    private Font getFont(){
        if (font == null){
            font = new Font(SkiaUtil.getPUHUITI(),FONT_SIZE);
        }
        return font;
    }
}



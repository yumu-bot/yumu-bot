package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import org.jetbrains.skija.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;

@Service("T-BP")
public class TodayBpService implements MessageService{
    private static final int FONT_SIZE = 30;
    private Font font;
    DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private Font getFont() {
        if (font == null) {
            font = new Font(SkiaUtil.getPUHUITI(), FONT_SIZE);
        }
        return font;
    }
    OsuGetService osuGetService;
    BindDao bindDao;
    DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
    @Autowired
    public TodayBpService(OsuGetService osuGetService, BindDao bindDao){
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        //消息来源
        var from = event.getSubject();
        //用户
        var user = bindDao.getUser(event.getSender().getId());

        //模式
        var mode = OsuMode.getMode(matcher.group("mode"));

        //bp列表获取
        var bpList = osuGetService.getBestPerformance(user, mode, 0,100);
        //输出文本行的集合
        var lines = new ArrayList<TextLine>();
        //第一行 getFont是获取字体
        var t_temp = TextLine.make("24h内bp:", getFont());
        lines.add(t_temp);
        //计算图片宽度
        float maxWidth = t_temp.getWidth();
        // 时间计算
        LocalDateTime dayBefore = LocalDateTime.now().plusDays(-1);
        //文本暂存
        StringBuilder modsb = new StringBuilder();
        //生成所有文字
        for (int i = 0; i < bpList.size(); i++) {
            var bp = bpList.get(i);
            if (dayBefore.isBefore(bp.getTime())){
                bp.getMods().forEach(modsb::append);
                var t = TextLine.make("bp"+(i+1)+' '+decimalFormat.format(bp.getPp())+"pp "+decimalFormat.format(bp.getAccuracy()*100)+"% +"+modsb, getFont());
                modsb.setLength(0);
                //统计文字最宽宽度
                if (t.getWidth() >maxWidth) maxWidth = t.getWidth();
                lines.add(t);
            }
        }
        //没有的话
        if (lines.size() <= 1){
            from.sendMessage("多打打");
            return;//此处结束
        }

        //设置输出大小,宽是最宽文字+50 高是总和+50
        int w = (int) maxWidth + 50;
        int h = (int) ((lines.size() + 1) * t_temp.getHeight()) + 50;

        Surface surface = Surface.makeRasterN32Premul(w, h);
        Shader shader = Shader.makeLinearGradient(0, 0, 0, h, SkiaUtil.getRandomColors());
        try (surface; shader) {
            var canvas = surface.getCanvas();
            canvas.clear(Color.makeRGB(38, 51, 57));
//            canvas.drawRect(Rect.makeWH(w,h),new Paint().setShader(shader));
            canvas.translate(25, 40);
            for (TextLine line : lines) {
                //randomColor 随机颜色
                canvas.drawTextLine(line, 0, line.getCapHeight() + FONT_SIZE * 0.2f, new Paint().setColor(randomColor()));
                //每行往下偏移
                canvas.translate(0, line.getHeight());
            }
            var image = surface.makeImageSnapshot();
            QQMsgUtil.sendImage(from, image);
        } finally {
            for (var line : lines) {
                line.close();
            }
        }


    }

    int randomColor(){
        var t = new Random();
        var t1 = new Random();
        var t2 = new Random();
        return Color.makeRGB(128+t.nextInt(125),128+t1.nextInt(125),128+t2.nextInt(125));
    }

}

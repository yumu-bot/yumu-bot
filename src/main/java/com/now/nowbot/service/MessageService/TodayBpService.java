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
        var from = event.getSubject();
        var user = bindDao.getUser(event.getSender().getId());

        var mode = OsuMode.getMode(matcher.group("mode"));

        var bpList = osuGetService.getBestPerformance(user, mode, 0,100);
        var lines = new ArrayList<TextLine>();
        var t_temp = TextLine.make("24h内bp:", getFont());
        lines.add(t_temp);
        float maxWidth = t_temp.getWidth();
        LocalDateTime dayBefore = LocalDateTime.now().plusDays(-1);
        StringBuilder modsb = new StringBuilder();
        for (int i = 0; i < bpList.size(); i++) {
            var bp = bpList.get(i);
            if (dayBefore.isBefore(bp.getTime())){
                bp.getMods().forEach(modsb::append);
                var t = TextLine.make("bp"+i+' '+decimalFormat.format(bp.getPp())+"pp "+decimalFormat.format(bp.getAccuracy())+" +"+modsb, getFont());
                modsb.setLength(0);
                if (t.getWidth() >maxWidth) maxWidth = t.getWidth();
                lines.add(t);
            }
        }
        if (lines.size() <= 1){
            from.sendMessage("多打打");
            return;
        }

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
                canvas.drawTextLine(line, 0, line.getCapHeight() + FONT_SIZE * 0.2f, new Paint().setColor(SkiaUtil.getRandomColor()));
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

}

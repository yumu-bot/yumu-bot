package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.Panel.BphtPanelBuilder;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.jetbrains.skija.EncodedImageFormat;
import org.jetbrains.skija.Font;
import org.jetbrains.skija.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;

@Service("bpht")
public class BphtService implements MessageService {
    private static final int FONT_SIZE = 30;
    OsuGetService osuGetService;
    BindDao bindDao;
    Font font;

    @Autowired
    public BphtService(OsuGetService osuGetService, BindDao bindDao) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
    }

    class intValue {
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
            nu = bindDao.getUser(at.getTarget());
        }
        if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
            //查询其他人 bpht [name]
            String name = matcher.group("name").trim();
            long id = osuGetService.getOsuId(name);
            try {
                nu = bindDao.getUserFromOsuid(id);
            } catch (BindException e) {
                //do nothing
            }
            if (nu == null){
                //构建只有 name + id 的对象
                nu = new BinUser();
                nu.setOsuID(id);
                nu.setOsuName(name);
                isBind = false;
            }
        } else {
            //处理没有参数的情况 查询自身
            nu = bindDao.getUser(event.getSender().getId());
        }
        //bp列表
        List<Score> Bps;
        //分别处理mode
        var mode = OsuMode.getMode(matcher.group("mode"));
        //处理默认mode
        if (mode == OsuMode.DEFAULT && nu.getMode() != null) mode = nu.getMode();
        Bps = osuGetService.getBestPerformance(nu, mode,0,100);

        Image image;
        if (matcher.group("info") == null){
            if(mode == null || mode == OsuMode.DEFAULT) {
                image = new BphtPanelBuilder().draw(Bps, nu.getOsuName(), "").build();
            }else {
                image = new BphtPanelBuilder().draw(Bps, nu.getOsuName(), mode.getName()).build();
            }
        } else {
            if(mode == null || mode == OsuMode.DEFAULT) {
                image = new BphtPanelBuilder().mf(Bps, nu.getOsuName(), OsuMode.DEFAULT, osuGetService, nu).build();
            }else {
                image = new BphtPanelBuilder().mf(Bps, nu.getOsuName(), mode, osuGetService, nu).build();
            }
        }
        QQMsgUtil.sendImage(from, image.encodeToData(EncodedImageFormat.JPEG).getBytes());
//        from.sendMessage(dtbf.toString());
    }

}



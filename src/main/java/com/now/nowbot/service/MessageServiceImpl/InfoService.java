package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.InfoException;
import com.now.nowbot.util.QQMsgUtil;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.regex.Pattern;

@Service("INFO")
public class InfoService implements MessageService<InfoService.InfoParm> {
    private static final Logger log = LoggerFactory.getLogger(InfoService.class);
    @Autowired
    RestTemplate template;

    @Autowired
    OsuGetService osuGetService;
    @Autowired
    BindDao       bindDao;
    @Autowired
    ImageService  imageService;
    public record InfoParm(String name, Long qq, OsuMode mode){}

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)((ym)?information|yminfo(?![a-zA-Z_])|(ym)?i(?![a-zA-Z_]))+\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)?");
    Pattern pattern4QQ = Pattern.compile("^[!！](?i)i\\s*qq=(?<qq>\\d+)");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<InfoParm> data) {
        var m4qq = pattern4QQ.matcher(event.getRawMessage().trim());
        if (m4qq.find()) {
            data.setValue(new InfoParm(null, Long.parseLong(m4qq.group("qq")), OsuMode.DEFAULT));
            return true;
        }
        var matcher = pattern.matcher(event.getRawMessage().trim());
        if (!matcher.find()) {
            return false;
        }
        OsuMode mode = OsuMode.getMode(matcher.group("mode"));
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);

        if (at != null) {
            data.setValue(new InfoParm(null, at.getTarget(), mode));
            return true;
        }
        String name = matcher.group("name");
        if (Strings.isNotBlank(name)) {
            data.setValue(new InfoParm(name, null, mode));
            return true;
        }
        data.setValue(new InfoParm(null, event.getSender().getId(), mode));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, InfoParm parm) throws Throwable {
        var from = event.getSubject();
        //from.sendMessage("正在查询您的信息");
        BinUser user;
        if (parm.name() != null) {
            long id;
            try {
                id = osuGetService.getOsuId(parm.name().trim());
            } catch (Exception e) {
                log.error("info: ", e);
                throw new InfoException(InfoException.Type.INFO_Player_NotFound);
            }
            user = new BinUser();
            user.setOsuID(id);
            user.setMode(OsuMode.DEFAULT);
        } else {
            user = bindDao.getUserFromQQ(parm.qq());
        }

        //处理默认mode
        var mode = parm.mode();
        if (mode == OsuMode.DEFAULT && user != null && user.getMode() != null) mode = user.getMode();

        OsuUser osuUser;
        List<Score> BPs;
        List<Score> Recents;

        try {
            osuUser = osuGetService.getPlayerInfo(user, mode);
            BPs = osuGetService.getBestPerformance(user, mode, 0, 100);
        } catch (Exception e) {
            log.error("get info error:", e);
            throw new InfoException(InfoException.Type.INFO_Player_NoBP);
        }

        Recents = osuGetService.getRecentN(user, mode, 0, 3);

        try {
            var img = imageService.getPanelD(osuUser, BPs, Recents, mode, osuGetService);
            QQMsgUtil.sendImage(from, img);
        } catch (Exception e) {
            log.error("INFO 数据请求失败", e);
            from.sendMessage("Info 渲染图片超时，请重试。");
        }
    }
}

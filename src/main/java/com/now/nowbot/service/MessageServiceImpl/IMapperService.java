package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.IMapperException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("IM")
public class IMapperService implements MessageService<Matcher> {
    OsuGetService osuGetService;
    BindDao bindDao;
    RestTemplate template;
    ImageService imageService;

    @Autowired
    public IMapperService (OsuGetService osuGetService, BindDao bindDao, RestTemplate template, ImageService image) {
        this.osuGetService = osuGetService;
        this.template = template;
        this.bindDao = bindDao;
        imageService = image;
    }


    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?(friendlegacy|fl(?![a-zA-Z_]))+(\\s*(?<n>\\d+))?(\\s*[:-]\\s*(?<m>\\d+))?");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        OsuUser osuUser;
        String name = matcher.group("name").trim();

        if (name.isEmpty()) {
            BinUser binUser;

            try {
                binUser = bindDao.getUser(event.getSender().getId());
            } catch (Exception e) {
                throw new IMapperException(IMapperException.Type.IM_Me_LoseBind);
            }

            try {
                osuUser = osuGetService.getPlayerInfo(binUser);
            } catch (Exception e) {
                throw new IMapperException(IMapperException.Type.IM_Me_NotFound);
            }

        } else {
            try {
                osuUser = osuGetService.getPlayerInfo(name);
            } catch (Exception e) {
                try {
                    var uid = Long.parseLong(matcher.group("name"));
                    osuUser = osuGetService.getPlayerInfo(uid);
                } catch (Exception e1) {
                    //NowbotApplication.log.error("e1", e1);
                    throw new IMapperException(IMapperException.Type.IM_Player_NotFound);
                }
            }
        }

        try {
            var data = imageService.getPanelM(osuUser, osuGetService);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            NowbotApplication.log.error("IMapper", e);
            throw new IMapperException(IMapperException.Type.IM_Send_Error);
            //from.sendMessage("出错了出错了,问问管理员");
        }
    }
}

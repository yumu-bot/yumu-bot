package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.IMapperException;
import com.now.nowbot.util.Pattern4ServiceImpl;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;

@Service("IMAPPER")
public class IMapperService implements MessageService<Matcher> {
    OsuUserApiService userApiService;
    OsuBeatmapApiService beatmapApiService;
    BindDao bindDao;
    RestTemplate template;
    ImageService imageService;

    @Autowired
    public IMapperService(OsuUserApiService userApiService,
                          OsuBeatmapApiService beatmapApiService,
                          BindDao bindDao, RestTemplate template, ImageService image) {
        this.userApiService = userApiService;
        this.beatmapApiService = beatmapApiService;
        this.template = template;
        this.bindDao = bindDao;
        imageService = image;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = Pattern4ServiceImpl.IMAPPER.matcher(event.getRawMessage().trim());
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
                binUser = bindDao.getUserFromQQ(event.getSender().getId());
            } catch (Exception e) {
                throw new IMapperException(IMapperException.Type.IM_Me_TokenExpired);
            }

            try {
                osuUser = userApiService.getPlayerInfo(binUser);
            } catch (Exception e) {
                throw new IMapperException(IMapperException.Type.IM_Me_NotFound);
            }

        } else {
            try {
                osuUser = userApiService.getPlayerInfo(name);
            } catch (Exception e) {
                try {
                    var uid = Long.parseLong(matcher.group("name"));
                    osuUser = userApiService.getPlayerInfo(uid);
                } catch (Exception e1) {
                    throw new IMapperException(IMapperException.Type.IM_Player_NotFound);
                }
            }
        }
        try {
            var data = imageService.getPanelM(osuUser, userApiService, beatmapApiService);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            NowbotApplication.log.error("IMapper", e);
            throw new IMapperException(IMapperException.Type.IM_Send_Error);
        }
    }
}

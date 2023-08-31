package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.KitaException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;

@Service("Kita")
public class KitaService implements MessageService {
    OsuGetService osuGetService;
    RestTemplate template;
    ImageService imageService;

    @Autowired
    public KitaService (OsuGetService osuGetService, RestTemplate template, ImageService image) {
        this.osuGetService = osuGetService;
        this.template = template;
        imageService = image;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        long bid;
        String mod;
        short position;
        String round;
        BeatMap beatMap;
        boolean hasBG = matcher.group("noBG") == null;

        if (matcher.group("bid") == null) throw new KitaException(KitaException.Type.KITA_Parameter_BidError);
        try {
            bid = Long.parseLong(matcher.group("bid"));
        } catch (NumberFormatException e) {
            throw new KitaException(KitaException.Type.KITA_Parameter_BidError);
        }

        if (matcher.group("mod") == null) {
            mod = "NM";
            position = 1;
        } else {
            try {
                var modStr = matcher.group("mod").toUpperCase();
                mod = modStr.substring(0, 2);
                position = Short.parseShort(modStr.substring(2));
            } catch (NumberFormatException e) {
                throw new KitaException(KitaException.Type.KITA_Parameter_ModError);
            }
        }

        if (matcher.group("round") == null) {
            round = "Unknown";
        } else {
            try {
                round = matcher.group("round");
            } catch (NumberFormatException e) {
                throw new KitaException(KitaException.Type.KITA_Parameter_RoundError);
            }
        }

        try {
            beatMap = osuGetService.getBeatMapInfo(bid);
        } catch (Exception e) {
            throw new KitaException(KitaException.Type.KITA_Map_FetchFailed);
        }

        if (hasBG) {
            try {
                var data = imageService.getPanelDelta(beatMap, round, mod, position, hasBG);
                QQMsgUtil.sendImage(from, data);
            } catch (Exception e) {
                NowbotApplication.log.error("KITA", e);
                throw new KitaException(KitaException.Type.KITA_Send_Error);
                //from.sendMessage("出错了出错了,问问管理员");
            }
        } else {
            if (from instanceof Group group) {
                try {
                    var data = imageService.getPanelDelta(beatMap, round, mod, position, hasBG);
                    group.sendFile(data, matcher.group("id") + ".png");
                } catch (Exception e) {
                    NowbotApplication.log.error("KITA-X", e);
                    throw new KitaException(KitaException.Type.KITA_Send_Error);
                    //from.sendMessage("出错了出错了,问问管理员");
                }
            } else {
                throw new KitaException(KitaException.Type.KITA_Send_NotGroup);
            }
        }

    }
}
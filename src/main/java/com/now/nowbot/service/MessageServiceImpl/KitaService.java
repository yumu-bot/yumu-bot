package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.ServiceException.KitaException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;

@Service("KITA")
public class KitaService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(KitaService.class);
    RestTemplate         template;
    OsuBeatmapApiService beatmapApiService;
    ImageService         imageService;

    @Autowired
    public KitaService (OsuBeatmapApiService beatmapApiService,
                        RestTemplate template, ImageService image) {
        this.beatmapApiService = beatmapApiService;
        this.template = template;
        imageService = image;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instructions.KITA.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        long BID;
        String mod;
        short position;
        String round;
        BeatMap beatMap;
        boolean hasBG = matcher.group("noBG") == null;
        var BIDstr = matcher.group("bid");

        if (BIDstr == null) throw new KitaException(KitaException.Type.KITA_Parameter_NoBid);
        try {
            BID = Long.parseLong(BIDstr);
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
            beatMap = beatmapApiService.getBeatMapInfo(BID);
        } catch (Exception e) {
            throw new KitaException(KitaException.Type.KITA_Map_FetchFailed);
        }

        if (hasBG) {
            try {
                var data = imageService.getPanelDelta(beatMap, round, mod, position, hasBG);
                QQMsgUtil.sendImage(from, data);
            } catch (Exception e) {
                log.error("KITA", e);
                throw new KitaException(KitaException.Type.KITA_Send_Error);
                //from.sendMessage("出错了出错了,问问管理员");
            }
        } else {
            if (from instanceof Group group) {
                try {
                    var data = imageService.getPanelDelta(beatMap, round, mod, position, hasBG);
                    group.sendFile(data, matcher.group("bid") + ' ' + mod + position + ".png");
                } catch (Exception e) {
                    log.error("KITA-X", e);
                    throw new KitaException(KitaException.Type.KITA_Send_Error);
                    //from.sendMessage("出错了出错了,问问管理员");
                }
            } else {
                throw new KitaException(KitaException.Type.KITA_Send_NotGroup);
            }
        }

    }
}
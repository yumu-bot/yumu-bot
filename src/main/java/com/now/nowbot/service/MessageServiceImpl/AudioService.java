package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.ServiceException.AudioException;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Objects;

@Service("AUDIO")
public class AudioService implements MessageService<AudioService.AudioParam> {
    private static final Logger log = LoggerFactory.getLogger(AudioService.class);

    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    WebClient osuApiWebClient;

    public static class AudioParam {
        Boolean isBid;
        Integer id;
        Exception err;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<AudioParam> data) {
        var matcher = Instructions.AUDIO.matcher(messageText);
        if (!matcher.find()) {
            return false;
        }
        var param = new AudioParam();
        var id_str = matcher.group("id");
        var type = matcher.group("type");

        if (Objects.isNull(id_str)) param.err = new AudioException(AudioException.Type.SONG_Parameter_NoBid);


        try {
            param.id = Integer.parseInt(id_str);
        } catch (NumberFormatException e) {
            param.err = new AudioException(AudioException.Type.SONG_Parameter_BidError);
        }

        param.isBid = Objects.equals(type, "b") || Objects.equals(type, "bid");

        data.setValue(param);
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, AudioParam param) throws Throwable {
        var from = event.getSubject();

        if (param.err != null) {
            throw param.err;
        }

        int sid = 0;

        if (param.isBid) {
            try {
                sid = beatmapApiService.getBeatMapInfo(param.id).getSID();
            } catch (Exception e) {
                throw new AudioException(AudioException.Type.SONG_Map_NotFound);
            }
        }

        byte[] voiceData;

        try {
            voiceData = getVoice(sid);
        } catch (Exception e) {
            if (param.isBid) {
                log.error("音频下载失败：", e);
                throw new AudioException(AudioException.Type.SONG_Download_Error);
            } else {
                //输入的不是 SID
                try {
                    sid = beatmapApiService.getBeatMapInfo(param.id).getSID();
                } catch (Exception e1) {
                    throw new AudioException(AudioException.Type.SONG_Map_NotFound);
                }

                try {
                    voiceData = getVoice(sid);
                } catch (Exception e2) {
                    log.error("音频下载失败、附加转换失败：", e2);
                    throw new AudioException(AudioException.Type.SONG_Download_Error);
                }
            }
        }

        try {
            from.sendVoice(voiceData);
        } catch (Exception e) {
            log.error("音频发送失败：", e);
            throw new AudioException(AudioException.Type.SONG_Send_Error);
        }
    }


    private byte[] getVoice(int sid) {
        var url = STR."https://b.ppy.sh/preview/\{sid}.mp3";

        return osuApiWebClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }
}

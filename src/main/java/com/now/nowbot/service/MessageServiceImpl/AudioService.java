package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.BeatMap;
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
    public boolean isHandle(MessageEvent event, DataValue<AudioParam> data) {
        var matcher = Instructions.AUDIO.matcher(event.getRawMessage().trim());
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

        int id = param.id;

        String url;

        if (param.isBid) {
            BeatMap b;
            try {
                b = beatmapApiService.getBeatMapInfo(id);
            } catch (Exception e) {
                throw new AudioException(AudioException.Type.SONG_Map_NotFound);
            }
            url = STR."https://b.ppy.sh/preview/\{b.getBeatMapSet().getSID()}.mp3";
        } else {
            url = STR."https://b.ppy.sh/preview/\{id}.mp3";
        }

        byte[] voiceData;

        try {
            voiceData = osuApiWebClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (Exception e) {
            log.error("下载音频出现错误", e);
            throw new AudioException(AudioException.Type.SONG_Download_Error);
        }

        /*
            if (from instanceof AudioSupported){
                try {
                    Audio audio = ((AudioSupported) from).uploadAudio(ExternalResource.create(voicedate));
                    from.sendMessage(audio);
                } catch (Exception e) {
                    log.error("语音上传失败",e);
                    throw new TipsException("语音上传失败,请稍后再试");
                }
            }
             */

        try {
            from.sendVoice(voiceData);
        } catch (Exception e) {
            log.error("Audio:", e);
            throw new AudioException(AudioException.Type.SONG_Send_Error);
        }
    }
}

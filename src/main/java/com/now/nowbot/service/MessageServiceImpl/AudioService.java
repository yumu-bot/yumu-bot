package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.ServiceException.AudioException;
import com.now.nowbot.util.Pattern4ServiceImpl;
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

    static class AudioParam {
        Boolean isBid;
        Integer id;
        Exception err;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<AudioParam> data) {
        var matcher = Pattern4ServiceImpl.AUDIO.matcher(event.getRawMessage().trim());
        if (!matcher.find()) {
            return false;
        }
        var param = new AudioParam();
        try {
            var id_str = matcher.group("id");
            var type = matcher.group("type");

            if (id_str == null) throw new AudioException(AudioException.Type.SONG_Parameter_NoBid);
            param.id = Integer.parseInt(id_str);
            if (Objects.equals(type, "s") || Objects.equals(type, "sid")) param.isBid = false;
        } catch (NumberFormatException e) {
            param.err = new AudioException(AudioException.Type.SONG_Parameter_BidError);
        } catch (Exception e) {
            param.err = e;
        }
        data.setValue(param);
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, AudioParam param) throws Throwable {

        var from = event.getSubject();
        //BinUser user = bindDao.getUser(event.getSender().getId());
        if (param.err != null) {
            throw param.err;
        }
        boolean isBid = Boolean.TRUE.equals(param.isBid);
        int id = param.id;

        String url;
        if (isBid) {
            BeatMap mapinfo;
            try {
                mapinfo = beatmapApiService.getBeatMapInfo(id);
            } catch (Exception e) {
                throw new AudioException(AudioException.Type.SONG_Map_NotFound);
            }
            url = "https:" + mapinfo.getBeatMapSet().getMusicUrl();
        } else {
            url = "https://b.ppy.sh/preview/" + id + ".mp3";
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
            //log.error("voice download failed", e);
            //throw new TipsException("下载失败!");
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
            NowbotApplication.log.error("Audio:", e);
            throw new AudioException(AudioException.Type.SONG_Send_Error);
        }
    }
}

package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.AudioException;
import com.now.nowbot.util.Instructions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.regex.Pattern;

@Service("Audio")
public class AudioService implements MessageService<AudioService.AudioParam> {
    private static final Logger log = LoggerFactory.getLogger(AudioService.class);

    @Autowired
    OsuGetService osuGetService;
    @Autowired
    BindDao bindDao;

    Pattern reg = Instructions
            .getRegexBuilder("[!！]\\s*(?i)(ym)?(song|audio|a(?!\\w))")

            .groupStart()
            .addColon()
                .groupStart("type")
                .addWord1P()
                .groupEnd()
            .i01()
            .groupEnd()

            .addSpace0P()
                .groupStart("id")
                .addNumber1P()
                .groupEnd()
            .i01()

            .build();
    Pattern p1 = Pattern.compile("^[!！]\\s*(?i)(ym)?(song|audio|a(?![a-zA-Z_]))+\\s*([:：](?<type>[\\w\\d]+))?\\s*(?<id>\\d+)?");

    static class AudioParam {
        Boolean isBid;
        Integer id;
        Exception err;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<AudioParam> data) {
        var matcher = p1.matcher(event.getRawMessage().trim());
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

        URL url;
        if (isBid) {
            BeatMap mapinfo;
            try {
                mapinfo = osuGetService.getBeatMapInfo(id);
            } catch (Exception e) {
                throw new AudioException(AudioException.Type.SONG_Map_NotFound);
            }
            url = new URL("http:" + mapinfo.getBeatMapSet().getMusicUrl());
        } else {
            url = new URL("http://b.ppy.sh/preview/" + id + ".mp3");
        }

        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

        try {
            httpConn.connect();
        } catch (ConnectException e) {
            throw new AudioException(AudioException.Type.SONG_Connect_TimeOut);
            //log.error("connection timed out", e);
            //throw new TipsException("连接超时!");
        }

        byte[] voiceData;

        try (InputStream cin = httpConn.getInputStream()) {
            voiceData = cin.readAllBytes();
        } catch (IOException e) {
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
            log.error("Audio:", e);
            throw new AudioException(AudioException.Type.SONG_Send_Error);
        }
    }
}

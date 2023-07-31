package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.AudioException;
import com.now.nowbot.throwable.TipsException;
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
import java.util.regex.Matcher;

@Service("Audio")
public class AudioService implements MessageService{
    private static final Logger log = LoggerFactory.getLogger(AudioService.class);

    @Autowired
    OsuGetService osuGetService;
    @Autowired
    BindDao bindDao;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {

        var from = event.getSubject();
        //BinUser user = bindDao.getUser(event.getSender().getId());
        boolean isBid = true;

        var id_str = matcher.group("id");
        var type = matcher.group("type");
        int id;

        if (id_str != null) {
            id = Integer.parseInt(id_str);
        } else {
            throw new AudioException(AudioException.Type.SONG_Parameter_NoBid);
            //if (id == 0) from.sendMessage("参数为<bid>或者指定sid/bid查询bid:<bid>/sid:<sid>");
        }

        if (Objects.equals(type, "s") || Objects.equals(type, "sid")) isBid = false;

        URL url;
        try {
            if (isBid) {
                var mapinfo = osuGetService.getMapInfo(id);
                url = new URL("http:"+mapinfo.getBeatMapSet().getMusicUrl());
            } else {
                url = new URL("http://b.ppy.sh/preview/"+id+".mp3");
            }

            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

            try {
                httpConn.connect();
            } catch (ConnectException e) {
                throw new AudioException(AudioException.Type.SONG_Connect_TimeOut);
                //log.error("connection timed out", e);
                //throw new TipsException("连接超时!");
            }

            try (InputStream cin = httpConn.getInputStream()) {
                byte[] voicedate = cin.readAllBytes();
                from.sendVoice(voicedate);
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

        } catch (Exception e) {
            log.error("Audio:", e);
            throw new AudioException(AudioException.Type.SONG_Send_Error);
            //throw new LogException("song",e);
        }
    }
}

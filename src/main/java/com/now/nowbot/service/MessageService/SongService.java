package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.LogException;
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
import java.util.regex.Matcher;

@Service("song")
public class SongService implements MessageService{
    private static final Logger log = LoggerFactory.getLogger(SongService.class);

    @Autowired
    OsuGetService osuGetService;
    @Autowired
    BindDao bindDao;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {

        var from = event.getSubject();
        BinUser user = bindDao.getUser(event.getSender().getId());
        int id = 0;
        boolean isBid = true;
        if (matcher.group("id") != null) {
            id = Integer.parseInt(matcher.group("id"));
        }else
        if (matcher.group("bid") != null) {
            id = Integer.parseInt(matcher.group("bid"));
        }else
        if (matcher.group("sid") != null) {
            id = Integer.parseInt(matcher.group("sid"));
            isBid = false;
        }
        if(id == 0) from.sendMessage("参数为<bid>或者指定sid/bid查询bid:<bid>/sid:<sid>");

        URL url;
        try {
            if(isBid){
                var mapinfo = osuGetService.getMapInfo(id);
                url = new URL("http:"+mapinfo.getBeatMapSet().getMusicUrl());
            }else {
                url = new URL("http://b.ppy.sh/preview/"+id+".mp3");
            }
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            try {
                httpConn.connect();
            } catch (ConnectException e) {
                log.error("connect err", e);
                throw new TipsException("连接失败!");
            }
            InputStream cin = httpConn.getInputStream();
            try {
                byte[] voicedate = cin.readAllBytes();
                from.sendVoice(voicedate);
            } catch (IOException e) {
                log.error("voice download err", e);
                throw new TipsException("下载失败!");
            } finally {
                cin.close();
            }
            /*
            if (from instanceof AudioSupported){
                try {
                    Audio audio = ((AudioSupported) from).uploadAudio(ExternalResource.create(voicedate));
//                    from.sendMessage(audio);
                } catch (Exception e) {
                    log.error("语音上传失败",e);
                    throw new TipsException("语音上传失败,请稍后再试");
                }
            }
             */


        } catch (Exception e) {
            throw new LogException("song",e);
        }
    }
}

package com.now.nowbot.service.MessageService;

import com.now.nowbot.model.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.service.StarService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.utils.ExternalResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;

@Service("song")
public class songService implements MessageService{
    @Autowired
        StarService starService;
    @Autowired
    OsuGetService osuGetService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        
        var from = event.getSubject();
        BinUser user = BindingUtil.readUser(event.getSender().getId());
        StarService.Score score = starService.getScore(user);
        if(!starService.delStart(score,0.5f)){
            from.sendMessage("您的积分不够!请多刷pp!");
            return;
        }
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
                url = new URL("http:"+mapinfo.getJSONObject("beatmapset").getString("preview_url"));
            }else {
                url = new URL("http://b.ppy.sh/preview/"+id+".mp3");
            }
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.connect();
            InputStream cin = httpConn.getInputStream();
            byte[] voicedate = cin.readAllBytes();
            from.sendMessage(ExternalResource.uploadAsVoice(ExternalResource.create(voicedate),from));
            cin.close();
        } catch (Exception e) {
            starService.addStart(score,0.5f);
            throw e;
        }
    }
}

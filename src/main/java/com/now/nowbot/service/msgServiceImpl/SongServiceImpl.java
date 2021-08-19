package com.now.nowbot.service.msgServiceImpl;

import com.now.nowbot.entity.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.service.StarService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.utils.ExternalResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SongServiceImpl extends MessageService{
    @Autowired
    OsuGetService osuGetService;
    @Autowired
    StarService starService;

    public SongServiceImpl(){
        super("song");
    }

    @Override
    public void handleMsg(MessageEvent event) {
        Contact from;
        if(event instanceof GroupMessageEvent) {
            from = ((GroupMessageEvent) event).getGroup();
        }else {
            from = event.getSender();
        }

        BinUser user = BindingUtil.readUser(event.getSender().getId());
        if(user == null) {
            from.sendMessage("您未绑定，禁止使用！！！");
            return;
        }



        Pattern p = Pattern.compile("((sid:(?<sid>\\d+))|(bid:(?<bid>\\d+)))|(?<id>\\d+)");
        Matcher m = p.matcher(event.getMessage().contentToString());
        int id = 0;
        boolean isBid = true;
        if(m.find()){
            if (m.group("id") != null) {
                id = Integer.parseInt(m.group("id"));
            }else
            if (m.group("bid") != null) {
                id = Integer.parseInt(m.group("bid"));
            }else
            if (m.group("sid") != null) {
                id = Integer.parseInt(m.group("sid"));
                isBid = false;
            }
        }else {
            from.sendMessage("参数为<bid>或者指定sid/bid查询bid:<bid>/sid:<sid>");
            return;
        }
        StarService.score score = starService.getScore(user);
        if(!starService.delStart(score,0.5f)){
            from.sendMessage("您的积分够!请多刷pp!");
            return;
        }
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
            from.sendMessage("下载失败，请稍后再试");
            starService.addStart(score,0.5f);
        }

    }
}

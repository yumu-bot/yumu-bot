package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.Ymp;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.TipsException;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.utils.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;

@Service("ymp")
public class YmpService implements MessageService{
    private static final Logger log = LoggerFactory.getLogger(YmpService.class);

    RestTemplate template;
    OsuGetService osuGetService;
    BindDao bindDao;
    @Autowired
    public YmpService(RestTemplate restTemplate, OsuGetService osuGetService, BindDao bindDao){
        template = restTemplate;
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        boolean isAll = matcher.group("isAll").toLowerCase().charAt(0) == 'r';
        //from.sendMessage(isAll?"正在查询24h内的所有成绩":"正在查询24h内的pass成绩");
        String name = matcher.group("name");
        JSONArray dates;
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        BinUser user = null;
        Long id = 0L;
        if (at != null){
            user = bindDao.getUser(at.getTarget());
        }else {
            if (matcher.group("name") != null && !matcher.group("name").trim().equals("")){
                id = osuGetService.getOsuId(matcher.group("name").trim());
            }else {
                user = bindDao.getUser(event.getSender().getId());
            }
        }
        var mode = OsuMode.getMode(matcher.group("mode"));
        //处理默认mode
        if (mode == OsuMode.DEFAULT && user != null && user.getMode() != null) mode = user.getMode();
        if (user != null){
            dates = getDates(user,mode,isAll);
        }else {
            dates = getDates(id,mode,isAll);
        }
        if(dates.size()==0){
            throw new TipsException("24h内无记录");
        }
        JSONObject date = dates.getJSONObject(0);
        var d = Ymp.getInstance(date);
        HttpEntity<Byte[]> httpEntity = (HttpEntity<Byte[]>) HttpEntity.EMPTY;
        var bytes = template.exchange(d.getUrl(), HttpMethod.GET, httpEntity, byte[].class).getBody();
        Image img = from.uploadImage(ExternalResource.create(bytes));
        from.sendMessage(img.plus(d.getOut()));
//        if (user != null){
//            log.info(starService.ScoreToStar(user, date));
//        }
    }
    private JSONArray getDates(BinUser user, OsuMode mode, boolean isAll){
        if (isAll)
            return osuGetService.getAllRecent(user, mode, 0, 1);
        else
            return osuGetService.getRecent(user, mode, 0, 1);
    }
    private JSONArray getDates(Long id, OsuMode mode, boolean isAll){
        if (isAll)
            return osuGetService.getAllRecent(Math.toIntExact(id), mode, 0, 1);
        else
            return osuGetService.getRecent(Math.toIntExact(id), mode, 0, 1);
    }
}

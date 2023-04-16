package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.ScoreException;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;

@Service("score")
public class ScoreService implements MessageService {
    OsuGetService osuGetService;
    BindDao bindDao;
    RestTemplate template;
    @Autowired
    public ScoreService(OsuGetService osuGetService, BindDao bindDao, RestTemplate template){
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.template = template;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        BinUser user = null;

        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        if (at != null) {
            try {
                user = bindDao.getUser(at.getTarget());
            } catch (Exception e) {
                throw new ScoreException(ScoreException.Type.SCORE_Player_NoBind);
                //throw new TipsException("该玩家没有绑定");
            }
        } else {
            user = bindDao.getUser(event.getSender().getId());
        }

        var mode = OsuMode.getMode(matcher.group("mode"));
        //处理默认mode
        if (mode == OsuMode.DEFAULT && user != null && user.getMode() != null) mode = user.getMode();

        var bid = Long.parseLong(matcher.group("bid"));

        Score score = null;
        try {
            score = osuGetService.getScore(bid, user, mode).getScore();
        } catch (Exception e) {
            throw new ScoreException(ScoreException.Type.SCORE_Score_NotFound);
            //from.sendMessage("你没打过这张图"); return;
        }
        var userInfo = osuGetService.getPlayerInfo(user, mode);

        try {
            var data = YmpService.postImage(userInfo, score, osuGetService, template);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            throw new ScoreException(ScoreException.Type.SCORE_Send_Error);
            //from.sendMessage("出错了出错了,问问管理员");
        }
    }
}

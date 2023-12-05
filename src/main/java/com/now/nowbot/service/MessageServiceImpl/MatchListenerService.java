package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.multiplayer.Match;
import com.now.nowbot.model.multiplayer.MatchEvent;
import com.now.nowbot.model.multiplayer.MatchListener;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import com.now.nowbot.throwable.ServiceException.MatchListenerException;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Objects;

@Service("MATCHLISTENER")
public class MatchListenerService implements MessageService<MatchListenerService.ListenerParam> {

    @Resource
    OsuMatchApiService osuMatchApiService;

    public static class ListenerParam {
        Integer id = null;
        String operate = null;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<ListenerParam> data) throws Throwable {
        var matcher = Instructions.LISTENER.matcher(event.getRawMessage().trim());
        var param = new MatchListenerService.ListenerParam();
        var id = matcher.group("matchid");
        var op = matcher.group("operate");

        if (matcher.find()) {
            if (Objects.nonNull(id) && !id.isBlank()) {
                try {
                    param.id = Integer.parseInt(matcher.group("matchid"));
                } catch (NumberFormatException e) {
                    throw new MatchListenerException(MatchListenerException.Type.ML_MatchID_RangeError);
                }
            } else {
                throw new MatchListenerException(MatchListenerException.Type.ML_Parameter_None);
            }

            if (Objects.nonNull(op) && !op.isBlank()) {
                param.operate = op;
            } else {
                param.operate = "start";
            }

            data.setValue(param);
            return true;
        }

        return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, ListenerParam param) throws Throwable {
        Match match;
        MatchEvent matchEvent;

        try {
            match = osuMatchApiService.getMatchInfo(param.id, 10);
        } catch (HttpClientErrorException e) {
            throw new MatchListenerException(MatchListenerException.Type.ML_MatchID_NotFound);
        }

        if (Objects.equals(match.getEvents().getLast().getDetail().type(), "match-disbanded")) {
            throw new MatchListenerException(MatchListenerException.Type.MR_Match_End);
        }
        MatchListener listener = null;
        if (Objects.equals(param.operate, "start")) {
            listener = new MatchListener(match, osuMatchApiService);

            listener.addEventListener((eventList, newMatch) -> {
                // 这里是在其他线程里调用的
                // 使用 matchEvents
                eventList.forEach(e -> System.out.println(e.getDetail().type()));
                // 使用 newMatch
                System.out.println(newMatch.getMatchStat().getName());

                // 这里换成发送你生成的图
                if (!eventList.isEmpty()) event.getSubject().sendMessage("比赛信息更新了 [图片]");
            });

            listener.startListener();

            // 过一段时间或者其他线程来停止监听
            listener.stopListener();
        }

        var threadLock = ASyncMessageUtil.getLock(event);
        MessageEvent newEvent;
        while (true) {
            newEvent = threadLock.get();
            // 线程锁只能用一次, 记得重复取锁
            threadLock = ASyncMessageUtil.getLock(newEvent);
            // 如果是收到 stop <match id> 就停止 这里只是做个示范
            if (newEvent.getRawMessage().equals("stop " + param.id) && Objects.nonNull(listener)) {
                listener.stopListener();
            }
        }

    }

    public void getMatchRound () {

    }

}

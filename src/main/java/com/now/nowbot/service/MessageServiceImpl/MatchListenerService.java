package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.multiplayer.Match;
import com.now.nowbot.model.multiplayer.MatchListener;
import com.now.nowbot.model.multiplayer.MatchRound;
import com.now.nowbot.model.multiplayer.MatchStat;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import com.now.nowbot.throwable.ServiceException.MatchListenerException;
import com.now.nowbot.throwable.ServiceException.MatchRoundException;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;

@Service("MATCHLISTENER")
public class MatchListenerService implements MessageService<MatchListenerService.ListenerParam> {
    Logger log = LoggerFactory.getLogger(MatchListenerService.class);

    @Resource
    OsuMatchApiService osuMatchApiService;
    @Resource
    ImageService imageService;


    public static class ListenerParam {
        Integer id = null;
        String operate = null;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<ListenerParam> data) throws Throwable {
        var matcher = Instructions.LISTENER.matcher(event.getRawMessage().trim());
        var param = new MatchListenerService.ListenerParam();

        if (matcher.find()) {
            var id = matcher.group("matchid");
            var op = matcher.group("operate");

            if (Objects.nonNull(id) && !id.isBlank()) {
                try {
                    param.id = Integer.parseInt(matcher.group("matchid"));
                } catch (NumberFormatException e) {
                    throw new MatchListenerException(MatchListenerException.Type.ML_MatchID_RangeError);
                }
            } else {
                throw new MatchListenerException(MatchListenerException.Type.ML_Parameter_None);
            }

            switch (op) {
                case "stop", "p", "end", "e" -> param.operate = "stop";
                case null, default -> param.operate = "start";
            }

            data.setValue(param);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void HandleMessage(MessageEvent event, ListenerParam param) throws Throwable {
        var from = event.getSubject();
        Match match;

        try {
            match = osuMatchApiService.getMatchInfo(param.id, 10);
        } catch (WebClientResponseException.NotFound e) {
            throw new MatchListenerException(MatchListenerException.Type.ML_MatchID_NotFound);
        }

        if (Objects.equals(match.getEvents().getLast().getDetail().type(), "match-disbanded") || match.isMatchEnd()) {
            throw new MatchListenerException(MatchListenerException.Type.MR_Match_End);
        }

        MatchListener listener = null;

        if (Objects.equals(param.operate, "start")) {
            from.sendMessage("开始监听比赛" + param.id);
            listener = new MatchListener(match, osuMatchApiService);
            listener.startListener();
        }

        if (Objects.equals(param.operate, "stop") && Objects.isNull(listener)) {
            throw new MatchListenerException(MatchListenerException.Type.MR_Match_NotListen);
        }

        if (Objects.nonNull(listener)) {
            listener.addEventListener((eventList, newMatch) -> {
                // 发送新比赛
                if (!eventList.isEmpty() && Objects.equals(eventList.getFirst().getDetail().type(), "other") && Objects.nonNull(eventList.getFirst().getRound())) {
                    //刚开始比赛，没分
                    if (Objects.isNull(eventList.getFirst().getRound().getScoreInfoList())) {
                        var b = eventList.getFirst().getRound().getBeatmap();
                        var s = b.getBeatMapSet();

                        String mapInfo = s.getArtist() + '-' + s.getTitle() + " (" + s.getMapperName() + ") [" + b.getVersion() + "]";
                        from.sendMessage("比赛" + param.id + "已开始！谱面：\n" + mapInfo);
                    } else {
                        //比赛结束，发送成绩
                        try {
                            var img = getDataImage(eventList.getFirst().getRound(), newMatch.getMatchStat(), imageService);
                            QQMsgUtil.sendImage(from, img);
                        } catch (MatchRoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                // 比赛结束
                if ((!eventList.isEmpty() && Objects.equals(eventList.getFirst().getDetail().type(), "match-disbanded"))
                        || newMatch.isMatchEnd()) {
                    from.sendMessage("停止监听" + param.id + "：比赛结束");
                }
            });
        }

        var threadLock = ASyncMessageUtil.getLock(event);
        MessageEvent unlockEvent;

        while (true) {
            unlockEvent = threadLock.get();
            // 线程锁只能用一次, 记得重复取锁
            threadLock = ASyncMessageUtil.getLock(unlockEvent, 6 * 3600 * 1000);
            // 如果是收到 stop <match id> 就停止
            if (Objects.equals(param.operate, "stop") && Objects.nonNull(listener)) {
                from.sendMessage("停止监听" + param.id);
                listener.stopListener();
                break;
            }
        }
    }

    public byte[] getDataImage(MatchRound round, MatchStat stat, ImageService imageService) throws MatchRoundException {
        byte[] img;
        try {
            img = imageService.getPanelF2(stat, round, 0);
        } catch (Exception e) {
            log.error("MR 图片渲染失败：", e);
            throw new MatchRoundException(MatchRoundException.Type.MR_Fetch_Error);
        }
        return img;
    }

}

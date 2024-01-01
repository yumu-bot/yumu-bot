package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.Permission;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.multiplayer.*;
import com.now.nowbot.qq.event.GroupMessageEvent;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import com.now.nowbot.throwable.ServiceException.MatchListenerException;
import com.now.nowbot.throwable.ServiceException.MatchRoundException;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.throwable.TipsRuntimeException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Service("MATCHLISTENER")
public class MatchListenerService implements MessageService<MatchListenerService.ListenerParam> {
    static final Logger log = LoggerFactory.getLogger(MatchListenerService.class);

    @Resource
    OsuMatchApiService osuMatchApiService;
    @Resource
    ImageService imageService;
    @Resource
    MatchMapService matchMapService;

    public static void stopAllListener() {
        ListenerCheck.listenerMap.values().forEach(l -> l.stopListener(MatchListener.StopType.SERVICE_STOP));
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<ListenerParam> data) throws Throwable {
        var matcher = Instructions.LISTENER.matcher(event.getRawMessage().trim());
        var param = new MatchListenerService.ListenerParam();

        if (! matcher.find()) return false;

        var id = matcher.group("matchid");
        var op = matcher.group("operate");

        if (StringUtils.hasText(id)) {
            param.id = Integer.parseInt(matcher.group("matchid"));
        } else {
            throw new MatchListenerException(MatchListenerException.Type.ML_Parameter_None);
        }

        param.operate = switch (op) {
            case "stop", "p", "end", "e", "off", "f" -> "stop";
            case "s", "start" -> "start";
            case null -> "start";
            default -> throw new MatchListenerException(MatchListenerException.Type.ML_Parameter_None);
        };

        data.setValue(param);
        return true;
    }

    @SuppressWarnings("all")
    private static MatchRound insertUser(MatchEvent matchEvent, Match match) {
        var round = matchEvent.getRound();
        //要自己加MicroUser
        for (MatchScore s : round.getScoreInfoList()) {
            for (MicroUser p : match.getPlayers()) {
                if (Objects.equals(p.getId(), s.getUserId()) && s.getUser() == null) {
                    s.setUser(p);
                    s.setUserName(p.getUserName());
                    break;
                }
            }
        }
        return round;
    }

    public static class ListenerParam {
        Integer id      = null;
        String  operate = null;
    }

    public byte[] getDataImage(MatchRound round, MatchStat stat, int index, ImageService imageService) throws MatchRoundException {

        byte[] img;
        try {
            img = imageService.getPanelF2(stat, round, index);
        } catch (Exception e) {
            log.error("MR 图片渲染失败：", e);
            throw new MatchRoundException(MatchRoundException.Type.MR_Fetch_Error);
        }
        return img;
    }

    @Override
    public void HandleMessage(MessageEvent event, ListenerParam param) throws Throwable {
        if (Objects.equals(param.operate, "stop")) {
            if (event instanceof GroupMessageEvent groupEvent) {
                ListenerCheck.cancel(
                        groupEvent.getSender().getId(),
                        groupEvent.getGroup().getId(),
                        Permission.isSuper(event.getSender().getId()),
                        param.id);
            } else {
                throw new TipsException(MatchListenerException.Type.ML_Send_NotGroup.message);
            }
            return;
        }
        var from = event.getSubject();
        Match match;

        try {
            match = osuMatchApiService.getMatchInfo(param.id, 10);
        } catch (WebClientResponseException.NotFound e) {
            throw new MatchListenerException(MatchListenerException.Type.ML_MatchID_NotFound);
        }

        if (match.isMatchEnd()) {
            throw new MatchListenerException(MatchListenerException.Type.ML_Match_End);
        }

        if (! (event instanceof GroupMessageEvent groupEvent)) {
            throw new TipsException(MatchListenerException.Type.ML_Send_NotGroup.message);
        }

        var senderId = groupEvent.getSender().getId();

        from.sendMessage(
                String.format(MatchListenerException.Type.ML_Listen_Start.message, param.id)
        );

        // 监听房间结束
        BiConsumer<Match, MatchListener.StopType> handleStop = (m, type) -> from.sendMessage(
                String.format(
                        MatchListenerException.Type.ML_Listen_Stop.message,
                        m.getMatchStat().getId(),
                        type.getTips()
                )
        );

        BiConsumer<List<MatchEvent>, Match> handleEvent = (eventList, newMatch) -> {

            Optional<MatchEvent> matchEventOpt = eventList.stream()
                    .filter(s -> Objects.nonNull(s.getRound()) && Objects.nonNull(s.getRound().getId()))
                    .max(Comparator.naturalOrder()); // 这里是取最后一个包含 round 的

            if (matchEventOpt.isEmpty()) {
                // 当前变动没有 game
                return;
            }

            var matchEvent = matchEventOpt.get();

            @SuppressWarnings("all")
            var scores = matchEvent.getRound().getScoreInfoList();

            //刚开始比赛，没分
            //以后可以做个提示或者面板之类的，也可能和现在的面板互换
            if (CollectionUtils.isEmpty(scores)) {
                var r = matchEvent.getRound();
                var b = r.getBeatmap();
                var s = b.getBeatMapSet();

                /*
                var p = new MapStatisticsService.MapParam(
                        b.getId(), OsuMode.getMode(r.getMode()), 1d, 1d, 0, Mod.getModsStr(r.getModInt())

                 */
                var m = newMatch.clone(); //这个 newMatch 貌似无法更改？

                var d = new MatchData(m, 0, 0, false, true); //看来这里的 failed 只能算 false，否则有问题

                var p = new MatchMapService.MatchMapParam(
                        b.getId(), OsuMode.getMode(r.getMode()), d, Mod.getModsStr(r.getModInt())
                );

                try {
                    matchMapService.HandleMessage(event, p);
                    /*
                    mapStatisticsService.HandleMessage(event, p);
                    Integer c = Objects.nonNull(b.getMaxCombo()) ? b.getMaxCombo() : 0;

                    var e = new MapStatisticsService.Expected(
                            OsuMode.getMode(r.getMode()), 1d, c, 0, Mod.getModsStrList(r.getModInt()));

                    var i = imageService.getPanelE2(Optional.empty(), b, e);
                    QQMsgUtil.sendImage(from, i);
                     */

                } catch (Throwable e) {
                    String info = STR. "(\{ b.getId() }) \{ s.getArtistUTF() } - (\{ s.getTitleUTF() }) [\{ b.getVersion() }]" ;
                    var i = imageService.getMarkdownImage(String.format(MatchListenerException.Type.ML_Match_Start.message, param.id, info));
                    QQMsgUtil.sendImage(from, i);
                }
                return;
            }
            //比赛结束，发送成绩
            try {
                var round = insertUser(matchEvent, newMatch);
                int indexP1 = newMatch.getEvents().stream().filter(s -> s.getRound() != null).filter(s -> s.getRound().getScoreInfoList() != null).toList().size();

                var img = getDataImage(round, newMatch.getMatchStat(), indexP1 - 1, imageService);

                QQMsgUtil.sendImage(from, img);
            } catch (TipsException tipsException) {
                // 注意 在监听器里为多线程环境, 无法通过向上 throw 来抛出错误
                // 手动处理提示
                from.sendMessage(tipsException.getMessage());
            } catch (Exception e) {
                log.error("图片发送失败", e);
            }

        };

        var key = ListenerCheck.add(
                senderId,
                groupEvent.getGroup().getId(),
                param.id,
                Permission.isSuper(senderId),
                (e) -> {
                },
                handleEvent,
                handleStop,
                match,
                osuMatchApiService
        );

    }

    record QQ_GroupRecord(long qq, long group, long mid) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (! (o instanceof QQ_GroupRecord that)) return false;

            if (qq != that.qq) return false;
            if (group != that.group) return false;
            return mid == that.mid;
        }

        @Override
        public int hashCode() {
            int result = (int) (qq ^ (qq >>> 32));
            result = 31 * result + (int) (group ^ (group >>> 32));
            result = 31 * result + (int) (mid ^ (mid >>> 32));
            return result;
        }
    }

    record Handlers(Consumer<Match> start,
                    BiConsumer<List<MatchEvent>, Match> event,
                    BiConsumer<Match, MatchListener.StopType> stop) {
    }

    private static class ListenerCheck {
        private final static int                           USER_MAX    = 3;
        private final static int                           GROUP_MAX   = 3;
        private final static Map<QQ_GroupRecord, Handlers> listeners   = new ConcurrentHashMap<>();
        private final static Map<Long, MatchListener>      listenerMap = new ConcurrentHashMap<>();

        static QQ_GroupRecord add(
                long qq,
                long group,
                long mid,
                boolean isSuper,
                Consumer<Match> start,
                BiConsumer<List<MatchEvent>, Match> event,
                BiConsumer<Match, MatchListener.StopType> stop,
                Match match,
                OsuMatchApiService matchApiService
        ) throws MatchListenerException {
            boolean notSuper = ! isSuper;
            var key = new QQ_GroupRecord(qq, group, mid);

            if (listeners.containsKey(key)) {
                throw new MatchListenerException(MatchListenerException.Type.ML_Listen_AlreadyInListening, mid);
            }

            AtomicInteger qqSum = new AtomicInteger();
            AtomicInteger groupSum = new AtomicInteger();
            listeners.keySet().forEach(k -> {
                if (k.mid == mid) {
                    if (k.qq == qq) qqSum.getAndIncrement();
                    if (k.group == group) groupSum.getAndIncrement();
                }
            });

            if (qqSum.get() >= USER_MAX && notSuper)
                throw new TipsRuntimeException(MatchListenerException.Type.ML_Listen_MaxInstance.message);
            if (groupSum.get() >= GROUP_MAX && notSuper)
                throw new TipsRuntimeException(MatchListenerException.Type.ML_Listen_MaxInstanceGroup.message);

            var listener = listenerMap.computeIfAbsent(mid, (k) -> new MatchListener(match, matchApiService));
            BiConsumer<Match, MatchListener.StopType> nStop = (m, type) -> {
                listeners.remove(key);
                // 用户停止可能会多次调用, 不直接删除
                if (! MatchListener.StopType.USER_STOP.equals(type)) {
                    listenerMap.remove(mid);
                }
            };
            var handlers = new Handlers(start, event, nStop.andThen(stop));
            listeners.put(key, handlers);
            listener.addStartListener(handlers.start());
            listener.addEventListener(handlers.event());
            listener.addStopListener(handlers.stop());

            listener.startListener();
            return key;
        }

        static void cancel(long qq, long group, boolean isSuper, long matchID) {
            var key = new QQ_GroupRecord(qq, group, matchID);
            var l = listeners.get(key);
            var listener = listenerMap.get(matchID);
            if (Objects.isNull(listener)) return;
            if (Objects.nonNull(l)) {
                listener.removeListener(l.start());
                listener.removeListener(l.event());
                listener.removeListener(l.stop(), MatchListener.StopType.USER_STOP);
                listeners.remove(key);

                // 如果没有其他群在监听则停止监听
                int lCount = 0;
                for (var nk : listeners.keySet()) {
                    if (nk.mid == matchID) {
                        lCount++;
                    }
                }
                if (lCount == 0) {
                    listener.stopListener(MatchListener.StopType.USER_STOP);
                    listenerMap.remove(matchID);
                }

            } else if (isSuper) {
                listener.stopListener(MatchListener.StopType.SUPER_STOP);
            }
        }
    }
}

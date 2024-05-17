package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.Permission;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.MicroUser;
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
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Service("MATCH_LISTENER")
public class MatchListenerService implements MessageService<MatchListenerService.ListenerParam> {
    static final Logger log = LoggerFactory.getLogger(MatchListenerService.class);

    @Resource
    OsuMatchApiService osuMatchApiService;
    @Resource
    ImageService    imageService;

    public static void stopAllListener() {
        ListenerCheck.listenerMap.values().forEach(l -> l.stopListener(MatchListener.StopType.SERVICE_STOP));
    }

    public enum Status {
        INFO, START, WAITING, RESULT, STOP, END
    }

    public static class ListenerParam {
        Integer id = null;
        Status operate = Status.END;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<ListenerParam> data) throws Throwable {
        var matcher = Instructions.MATCH_LISTENER.matcher(messageText);
        if (! matcher.find()) return false;

        var param = new ListenerParam();
        var from = event.getSubject();

        param.operate = switch (matcher.group("operate")) {
            case "stop", "p", "end", "e", "off", "f" -> Status.STOP;
            case "start", "s", "on", "o" -> Status.START;
            case "list", "l", "info", "i" -> Status.INFO;
            case null -> Status.START;
            default -> throw new MatchListenerException(MatchListenerException.Type.ML_Instructions);
        };

        if (StringUtils.hasText(matcher.group("matchid"))) {
            param.id = Integer.parseInt(matcher.group("matchid"));
        } else if (param.operate != Status.INFO) {

            try {
                var md = DataUtil.getMarkdownFile("Help/listen.md");
                var image = imageService.getPanelA6(md, "help");
                from.sendImage(image);
                return false;
            } catch (Exception e) {
                throw new MatchListenerException(MatchListenerException.Type.ML_Instructions);
            }

        }

        data.setValue(param);
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, ListenerParam param) throws Throwable {
        var from = event.getSubject();
        Match match = null;
        AtomicReference<Status> status = new AtomicReference<>(Status.START);

        // 需要在群里使用
        if (! (event instanceof GroupMessageEvent groupEvent)) {
            throw new TipsException(MatchListenerException.Type.ML_Send_NotGroup.message);
        }

        var senderId = groupEvent.getSender().getId();

        switch (param.operate) {
            case INFO -> {
                var listenerList = getGroupListenerList(groupEvent.getGroup().getId());
                if (listenerList.isEmpty()) {
                    throw new MatchListenerException(MatchListenerException.Type.ML_Info_NoListener);
                } else {
                    var sb = new StringBuilder();
                    listenerList.forEach(matchID -> sb.append('\n').append(matchID));
                    throw new MatchListenerException(MatchListenerException.Type.ML_Info_List, sb.toString());
                }
            }
            case START -> {
                try {
                    match = osuMatchApiService.getMatchInfo(param.id, 10);
                } catch (WebClientResponseException.NotFound e) {
                    throw new MatchListenerException(MatchListenerException.Type.ML_MatchID_NotFound);
                }

                if (match.isMatchEnd()) {
                    throw new MatchListenerException(MatchListenerException.Type.ML_Match_End);
                }

                from.sendMessage(
                        String.format(MatchListenerException.Type.ML_Listen_Start.message, param.id)
                );
            }
            case STOP -> {
                ListenerCheck.cancel(
                        groupEvent.getSender().getId(),
                        groupEvent.getGroup().getId(),
                        Permission.isSuperAdmin(event.getSender().getId()),
                        param.id);
                return;
            }
        }

        // 监听房间结束
        BiConsumer<Match, MatchListener.StopType> handleStop = (m, type) -> from.sendMessage(
                String.format(
                        MatchListenerException.Type.ML_Listen_Stop.message,
                        m.getMatchStat().getId(),
                        type.getTips()
                )
        );

        @SuppressWarnings("all")
        BiConsumer<List<MatchEvent>, Match> handleEvent = (eventList, newMatch) -> {

            Optional<MatchEvent> opt = eventList.stream()
                    .filter(s -> Objects.nonNull(s.getRound()) && Objects.nonNull(s.getRound().getId()))
                    .max(Comparator.naturalOrder()); // 这里是取最后一个包含 round 的

            // 当前没有 game 变动
            if (opt.isEmpty()) return;

            MatchEvent matchEvent = opt.get();
            List<MatchScore> scores = Objects.requireNonNull(matchEvent.getRound()).getScoreInfoList(); //基本上不可能为 null，除非 ppy 作妖

            //有点脱裤子放屁的感觉，但是先这么写着，至少思路清晰一点
            if (! CollectionUtils.isEmpty(scores))  {
                status.set(Status.RESULT);
            } else {
                status.set(Status.WAITING);
            }

            try {
                byte[] image = switch (status.get()) {
                    case WAITING -> getRoundStartImage(matchEvent.getRound(), newMatch.clone()); // 这个 newMatch 在算分的时候，貌似无法更改？
                    case RESULT -> getRoundResultsImage(matchEvent, newMatch);
                    case null, default -> throw new RuntimeException("状态机状态异常！");
                };


                try {
                    from.sendImage(image);
                } catch (Exception e) {
                    throw new RuntimeException(MatchListenerException.Type.ML_Send_Error.message);
                }
            } catch (RuntimeException e) {
                log.error("比赛监听：", e);
                from.sendMessage(e.getMessage());
            }
        };

        var key = ListenerCheck.add(
                senderId,
                groupEvent.getGroup().getId(),
                param.id,
                Permission.isSuperAdmin(senderId),
                (e) -> {
                },
                handleEvent,
                handleStop,
                match,
                osuMatchApiService
        );

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

    public byte[] getDataImage(MatchRound round, MatchStat stat, int index, ImageService imageService) throws MatchRoundException {

        byte[] img;
        try {
            img = imageService.getPanelF2(stat, round, index);
        } catch (Exception e) {
            log.error("对局信息图片渲染失败：", e);
            throw new MatchRoundException(MatchRoundException.Type.MR_Fetch_Error);
        }
        return img;
    }

    private byte[] getRoundResultsImage(MatchEvent matchEvent, Match match) {
        //比赛结束，发送成绩
        try {
            var round = insertUser(matchEvent, match);

            //剔除 5k 分以下
            round.setScoreInfoList(round.getScoreInfoList().stream()
                    .filter(s -> s.getScore() >= 5000).toList());

            int indexP1 = match.getEvents().stream().filter(s -> s.getRound() != null).filter(s -> s.getRound().getScoreInfoList() != null).toList().size();

            return getDataImage(round, match.getMatchStat(), indexP1 - 1, imageService);
        } catch (Exception e) {
            log.error("获取对局结果图片失败：", e);
            throw new RuntimeException("获取对局结果图片失败！");
        }
    }

    private byte[] getRoundStartImage(@NonNull MatchRound round, Match match) {
        var b = Objects.requireNonNullElse(round.getBeatMap(), new BeatMap()); //按道理说这里也不会是 null

        //看来这里的 failed 只能算 false，否则有问题
        var d = new MatchData(match, 0, 0, null, 1d, false, true);

        var x = new MapStatisticsService.Expected(OsuMode.getMode(round.getMode()), 1d, 0, 0, round.getMods());

        try {
            return imageService.getPanelE3(d, b, x);
        } catch (WebClientResponseException ignored) {
            String moreInfo;
            if (Objects.nonNull(b.getBeatMapSet())) {
                moreInfo = STR."(\{b.getId()}) \{b.getBeatMapSet().getArtistUnicode()} - (\{b.getBeatMapSet().getTitleUnicode()}) [\{b.getDifficultyName()}]";
            } else {
                moreInfo = STR."(\{b.getId()}) [\{b.getDifficultyName()}]";
            }
            try {
                return imageService.getPanelA6(
                        String.format(MatchListenerException.Type.ML_Match_Start.message, match.getMatchStat().getId(), moreInfo));
            } catch (WebClientResponseException e) {
                log.error("获取对局开始信息失败：", e);
                throw new RuntimeException("获取对局开始信息失败！");
            }

        }
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

    static List<Long> getGroupListenerList(long group) {
        return ListenerCheck.listeners.keySet().stream()
                .filter(handlers -> handlers.group == group)
                .map(handlers -> handlers.mid)
                .toList();
    }
}

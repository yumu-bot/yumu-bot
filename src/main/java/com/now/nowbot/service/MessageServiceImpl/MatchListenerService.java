package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.Permission;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.Match;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.multiplayer.*;
import com.now.nowbot.qq.event.GroupMessageEvent;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import com.now.nowbot.throwable.ServiceException.MatchListenerException;
import com.now.nowbot.throwable.ServiceException.MatchRoundException;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.throwable.TipsRuntimeException;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instruction;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("MATCH_LISTENER")
public class MatchListenerService implements MessageService<MatchListenerService.ListenerParam> {
    static final Logger log = LoggerFactory.getLogger(MatchListenerService.class);
    static final int    BREAK_ROUND = 15;

    @Resource
    OsuMatchApiService   matchApiService;
    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    ImageService         imageService;

    public static void stopAllListener() {
        ListenerCheck.listenerMap.values().forEach(l -> l.stopListener(MatchListener.StopType.SERVICE_STOP));
    }

    public enum Status {
        INFO, START, WAITING, RESULT, STOP, END
    }

    public static class ListenerParam {
        Integer id      = null;
        Status  operate = Status.END;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<ListenerParam> data) throws Throwable {
        var matcher = Instruction.MATCH_LISTENER.matcher(messageText);
        if (!matcher.find()) return false;

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
        if (!(event instanceof GroupMessageEvent groupEvent)) {
            throw new TipsException(MatchListenerException.Type.ML_Send_NotGroup.message);
        }

        var senderId = groupEvent.getSender().getId();

        switch (param.operate) {
            case INFO -> {
                var listenerList = getGroupListenerList(groupEvent.getGroup().getId());
                if (listenerList.isEmpty()) {
                    from.sendMessage(MatchListenerException.Type.ML_Info_NoListener.message);
                } else {
                    var sb = new StringBuilder();
                    listenerList.forEach(matchID -> sb.append('\n').append(matchID));
                    from.sendMessage(String.format(MatchListenerException.Type.ML_Info_List.message, sb));
                }
                return;
            }
            case START -> {
                try {
                    match = matchApiService.getMatchInfo(param.id, 10);
                } catch (WebClientResponseException.NotFound e) {
                    throw new MatchListenerException(MatchListenerException.Type.ML_MatchID_NotFound);
                }

                if (match.getMatchStat().getEndTime() != null) {
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
                        m.getMatchStat().getMatchID(),
                        type.getTips()
                )
        );

        Function<Integer, Boolean> handleOverTime = (time) -> {
            from.sendMessage(String.format("""
                    比赛(%d)已监听%d轮, 如果要继续监听请30秒内任意一人回复
                    "%d"(不带引号)
                    """, param.id, time, param.id));
            var lock = ASyncMessageUtil.getLock(
                    event.getSubject().getId(),
                    null, 30*1000,
                    (e)-> e.getRawMessage().equals(param.id.toString())
            );
            var newMessage = lock.get();
            if (newMessage == null) {
                ListenerCheck.cancel(senderId, groupEvent.getGroup().getId(), true, param.id);
                from.sendMessage("监听已取消");
                return false;
            }
            return true;
        };

        @SuppressWarnings("all")
        ThreeArgConsumer<List<Match.MatchEvent>, Match, QQ_GroupRecord> handleEvent = (eventList, newMatch, key) -> {
            Optional<Match.MatchEvent> opt = eventList.stream()
                    .filter(s -> Objects.nonNull(s.getRound()) && Objects.nonNull(s.getRound().getRoundID()))
                    .max(Comparator.naturalOrder()); // 这里是取最后一个包含 round 的

            // 当前没有 game 变动
            if (opt.isEmpty()) return;

            Match.MatchEvent matchEvent = opt.get();
            List<Match.MatchScore> scores = Objects.requireNonNull(matchEvent.getRound()).getScores(); //基本上不可能为 null，除非 ppy 作妖

            //有点脱裤子放屁的感觉，但是先这么写着，至少思路清晰一点
            if (!CollectionUtils.isEmpty(scores)) {
                status.set(Status.RESULT);
            } else {
                status.set(Status.WAITING);

                // 检查是否超过最大监听次数
                key.countAdd();
                if (key.check() && !handleOverTime.apply(key.count)) {
                    return;
                }
            }

            try {
                byte[] image = switch (status.get()) {
                    case WAITING ->
                            getRoundStartImage(matchEvent.getRound(), newMatch.clone()); // 这个 newMatch 在算分的时候，貌似无法更改？
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
                matchApiService
        );

    }

    @SuppressWarnings("all")
    private static Match.MatchRound insertUser(Match.MatchEvent matchEvent, Match match) {
        var round = matchEvent.getRound();
        //要自己加MicroUser
        for (Match.MatchScore s : round.getScores()) {
            for (MicroUser p : match.getPlayers()) {
                if (Objects.equals(p.getUserID(), s.getUserID()) && s.getUser() == null) {
                    s.setUser(p);
                    break;
                }
            }
        }
        return round;
    }

    public byte[] getDataImage(Match.MatchRound round, Match.MatchStat stat, int index, ImageService imageService) throws MatchRoundException {

        byte[] img;
        try {
            img = imageService.getPanelF2(stat, round, index);
        } catch (Exception e) {
            log.error("对局信息图片渲染失败：", e);
            throw new MatchRoundException(MatchRoundException.Type.MR_Fetch_Error);
        }
        return img;
    }

    private byte[] getRoundResultsImage(Match.MatchEvent matchEvent, Match match) {
        //比赛结束，发送成绩
        try {
            var round = insertUser(matchEvent, match);

            //剔除 5k 分以下
            round.setScores(round.getScores().stream()
                    .filter(s -> s.getScore() >= 5000).toList());

            int index = Math.toIntExact(
                    match.getEvents().stream().filter(s -> s.getRound() != null).filter(s -> s.getRound().getScores() != null).count()
            );

            // apply changes
            beatmapApiService.applyBeatMapExtend(round);
            beatmapApiService.applySRAndPP(round.getBeatMap(), OsuMode.getMode(round.getMode()), round.getModInt());

            return getDataImage(round, match.getMatchStat(), index, imageService);
        } catch (Exception e) {
            log.error("获取对局结果图片失败：", e);
            throw new RuntimeException("获取对局结果图片失败！");
        }
    }

    private byte[] getRoundStartImage(@NonNull Match.MatchRound round, Match match) {
        // apply changes
        beatmapApiService.applyBeatMapExtend(round);
        beatmapApiService.applySRAndPP(round.getBeatMap(), OsuMode.getMode(round.getMode()), round.getModInt());

        var d = new MatchCalculate(match,
                new MatchCalculate.CalculateParam(0, 0, null, 1d, true, true),
                beatmapApiService);

        var b = Objects.requireNonNullElse(round.getBeatMap(), new BeatMap(round.getBeatMapID()));

        var x = new MapStatisticsService.Expected(OsuMode.getMode(round.getMode()), 1d, b.getMaxCombo(), 0, round.getMods());

        try {
            return imageService.getPanelE3(d, b, x);
        } catch (WebClientResponseException ignored) {
            String moreInfo;
            if (Objects.nonNull(b.getBeatMapSet())) {
                moreInfo = STR."(\{b.getBeatMapID()}) \{b.getBeatMapSet().getArtistUnicode()} - (\{b.getBeatMapSet().getTitleUnicode()}) [\{b.getDifficultyName()}]";
            } else {
                moreInfo = STR."(\{b.getBeatMapID()}) [\{b.getDifficultyName()}]";
            }
            try {
                return imageService.getPanelA6(
                        String.format(MatchListenerException.Type.ML_Match_Start.message, match.getMatchStat().getMatchID(), moreInfo));
            } catch (WebClientResponseException e) {
                log.error("获取对局开始信息失败：", e);
                throw new RuntimeException("获取对局开始信息失败！");
            }

        }
    }

    static class QQ_GroupRecord {
        final long qq;
        final long group;
        final long messageID;

        public QQ_GroupRecord(long qq, long group, long messageID) {
            this.qq = qq;
            this.group = group;
            this.messageID = messageID;
        }

        int count = 0;

        public void countAdd() {
            count++;
        }

        public boolean check() {
            return count % BREAK_ROUND == 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof QQ_GroupRecord that)) return false;

            if (qq != that.qq) return false;
            if (group != that.group) return false;
            return messageID == that.messageID;
        }

        @Override
        public int hashCode() {
            int result = Long.hashCode(qq);
            result = 31 * result + Long.hashCode(group);
            result = 31 * result + Long.hashCode(messageID);
            return result;
        }
    }

    record Handlers(Consumer<Match> start,
                    BiConsumer<List<Match.MatchEvent>, Match> event,
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
                ThreeArgConsumer<List<Match.MatchEvent>, Match, QQ_GroupRecord> event,
                BiConsumer<Match, MatchListener.StopType> stop,
                Match match,
                OsuMatchApiService matchApiService
        ) throws MatchListenerException {
            boolean notSuper = !isSuper;
            var key = new QQ_GroupRecord(qq, group, mid);

            if (listeners.containsKey(key)) {
                throw new MatchListenerException(MatchListenerException.Type.ML_Listen_AlreadyInListening, mid);
            }

            AtomicInteger qqSum = new AtomicInteger();
            AtomicInteger groupSum = new AtomicInteger();
            listeners.keySet().forEach(k -> {
                if (k.messageID == mid) {
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
                if (!MatchListener.StopType.USER_STOP.equals(type)) {
                    listenerMap.remove(mid);
                }
            };

            var handlers = new Handlers(start, (e, m) -> event.accept(e, m, key), nStop.andThen(stop));
            listeners.put(key, handlers);
            listener.addStartListener(handlers.start());
            listener.addEventListener(handlers.event());
            listener.addStopListener(handlers.stop());

            listener.startListener();
            return key;
        }

        static void cancel(long qq, long group, boolean isSuper, long matchID) {
            // var key = new QQ_GroupRecord(qq, group, matchID);
            // 疑似有bug, 目前先改成只要同群的用户都能停
            QQ_GroupRecord key = null;
            Handlers handle = null;
            for (var x : listeners.keySet()) {
                if (x.group == group && x.messageID == matchID) {
                    key = x;
                    handle = listeners.get(x);
                }
            }

            var listener = listenerMap.get(matchID);
            if (Objects.isNull(listener)) return;
            if (Objects.nonNull(handle)) {
                listener.removeListener(handle.start());
                listener.removeListener(handle.event());
                listener.removeListener(handle.stop(), MatchListener.StopType.USER_STOP);
                listeners.remove(key);

                // 如果没有其他群在监听则停止监听
                int lCount = 0;
                for (var nk : listeners.keySet()) {
                    if (nk.messageID == matchID) {
                        lCount++;
                    }
                }
                if (lCount == 0) {
                    listener.stopListener(MatchListener.StopType.USER_STOP);
                    listenerMap.remove(matchID);
                }

            } else if (isSuper) {
                var groupsListener = listeners.entrySet()
                        .stream()
                        .filter((e) -> e.getKey().messageID == matchID)
                        .collect(Collectors.toSet());
                // 当正在监听的群多于一个且是在这个群里的话, 只删除对应的这一个
                if (groupsListener.size() > 1) {
                    Handlers handler = null;
                    for(var e : groupsListener) {
                        if (e.getKey().group == group) {
                            handler = e.getValue();
                            listeners.remove(e.getKey());
                            break;
                        }
                    }
                    if (handler != null) {
                        listener.removeListener(handler.start());
                        listener.removeListener(handler.event());
                        listener.removeListener(handler.stop(), MatchListener.StopType.SUPER_STOP);
                    }
                }

                // 不在任何监听的群里, 停止所有群的监听
                listener.stopListener(MatchListener.StopType.SUPER_STOP);
            }
        }
    }

    static List<Long> getGroupListenerList(long group) {
        return ListenerCheck.listeners.keySet().stream()
                .filter(handlers -> handlers.group == group)
                .map(handlers -> handlers.messageID)
                .toList();
    }
}
@FunctionalInterface
interface ThreeArgConsumer<T, U, V> {
    void accept(T t, U u, V v);
}
package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.Permission;
import com.now.nowbot.model.JsonData.MicroUser;
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
import java.util.function.BiFunction;
import java.util.stream.Stream;

@Service("MATCHLISTENER")
public class MatchListenerService implements MessageService<MatchListenerService.ListenerParam> {
    static final Logger log = LoggerFactory.getLogger(MatchListenerService.class);

    @Resource
    OsuMatchApiService osuMatchApiService;
    @Resource
    ImageService imageService;


    @Override
    public boolean isHandle(MessageEvent event, DataValue<ListenerParam> data) throws Throwable {
        var matcher = Instructions.LISTENER.matcher(event.getRawMessage().trim());
        var param = new MatchListenerService.ListenerParam();

        if (! matcher.find()) return false;

        var id = matcher.group("matchid");
        var op = matcher.group("operate");

        if (StringUtils.hasText(id)) {
            param.id = Integer.parseInt(matcher.group("matchid"));
                /* 如果你正则捕获组只有 \\d, 且把空字符串的可能排除掉了, 除了数据溢出 不可能会出现 NumberFormatException (另外感觉match id考虑用long了)
                try {
                    param.id = Integer.parseInt(matcher.group("matchid"));
                } catch (NumberFormatException e) {
                    throw new MatchListenerException(MatchListenerException.Type.ML_MatchID_RangeError);
                }
                 */
        } else {
            throw new MatchListenerException(MatchListenerException.Type.ML_Parameter_None);
        }

        switch (op) {
            case "stop", "p", "end", "e", "off", "f" -> param.operate = "stop";
            case null, default -> param.operate = "start";
        }

        data.setValue(param);
        return true;
    }

    public static void stopAllListener() {
        ListenerCheck.listeners.values().forEach(l -> l.stopListener(MatchListener.StopType.SERVICE_STOP));
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
                event.getSubject().sendMessage(
                        String.format(MatchListenerException.Type.ML_Listen_StopRequest.message, param.id.toString())
                );
                ListenerCheck.cancel(groupEvent.getGroup().getId(),
                        groupEvent.getSender().getId(),
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

        MatchListener listener = new MatchListener(match, osuMatchApiService);
        if (event instanceof GroupMessageEvent groupEvent) {
            var sender = groupEvent.getSender().getId();
            // 检查有没有重复, 重复直接抛错终止
            ListenerCheck.add(groupEvent.getGroup().getId(), sender, Permission.isSuper(sender), listener);
        } else {
            throw new TipsException(MatchListenerException.Type.ML_Send_NotGroup.message);
        }
        from.sendMessage(
                String.format(MatchListenerException.Type.ML_Listen_Start.message, param.id)
        );

        // 监听房间结束
        listener.addStopListener((m, type) -> {
            from.sendMessage(
                    String.format(
                            MatchListenerException.Type.ML_Listen_Stop.message,
                            param.id,
                            type.getTips()
                    ));
        });

        listener.addEventListener((eventList, newMatch) -> {

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
                var b = matchEvent.getRound().getBeatmap();
                var s = b.getBeatMapSet();

                String mapInfo = "(" + b.getId() + ") " + s.getArtistUTF() + " - " + s.getTitleUTF() + " (" + s.getMapperName() + ") [" + b.getVersion() + "]";
                from.sendMessage(
                        String.format(MatchListenerException.Type.ML_Match_Start.message, param.id, mapInfo)
                );
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
                //throw new MatchListenerException(MatchListenerException.Type.ML_Send_Error);
            }

        });

        listener.startListener();
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

    static BiFunction<Long, List<QQ_GroupRecord>, List<QQ_GroupRecord>> getDeleteFunc(QQ_GroupRecord key) {
        return (id, list) -> {
            if (list == null || ! list.contains(key)) {
                throw new TipsRuntimeException(MatchListenerException.Type.ML_Listen_NotListen.message);
            }
            list.remove(key);
            return CollectionUtils.isEmpty(list) ? null : list;
        };
    }

    private static class ListenerCheck {
        private final static int                                USER_MAX       = 3;
        private final static int                                GROUP_MAX      = 3;
        private final static Map<QQ_GroupRecord, MatchListener> listeners      = new ConcurrentHashMap<>();
        private final static Map<Long, List<QQ_GroupRecord>>    userListeners  = new ConcurrentHashMap<>();
        private final static Map<Long, List<QQ_GroupRecord>>    groupListeners = new ConcurrentHashMap<>();

        static void add(long qq, long group, boolean isSuper, MatchListener listener) {
            long mid = listener.getMatchID();
            boolean notSuper = ! isSuper;
            var key = new QQ_GroupRecord(qq, group, mid);

            userListeners.compute(qq, (q, uList) -> {
                if (uList == null) uList = new ArrayList<>(USER_MAX);
                if (uList.size() > USER_MAX && notSuper) {
                    throw new TipsRuntimeException(MatchListenerException.Type.ML_Listen_MaxInstance.message);
                }
                if (uList.contains(key) && notSuper) {
                    throw new TipsRuntimeException(
                            String.format(MatchListenerException.Type.ML_Listen_AlreadyInListening.message, mid)
                    );
                }
                for (var k : uList) {
                    if (k.mid == mid) {
                        throw new TipsRuntimeException(
                                String.format(MatchListenerException.Type.ML_Listen_AlreadyInListeningOthers.message, mid)
                        );
                    }
                }
                uList.add(key);
                return uList;
            });

            groupListeners.compute(group, (g, gList) -> {
                if (gList == null) gList = new ArrayList<>(GROUP_MAX);
                if (gList.size() > GROUP_MAX && notSuper) {
                    throw new TipsRuntimeException(MatchListenerException.Type.ML_Listen_MaxInstanceGroup.message);
                }
                if (gList.contains(key) && notSuper) {
                    throw new TipsRuntimeException(
                            String.format(MatchListenerException.Type.ML_Listen_AlreadyInListeningGroup.message, mid)
                    );
                }
                for (var k : gList) {
                    if (k.mid == mid) {
                        throw new TipsRuntimeException(
                                String.format(MatchListenerException.Type.ML_Listen_AlreadyInListeningOthers.message, mid)
                        );
                    }
                }
                gList.add(key);
                return gList;
            });

            listeners.put(key, listener);

            listener.addStopListener((ignore1, ignore2) -> {
                if (listeners.remove(key) != null) {
                    removeKey(key);
                }
            });
        }

        static void cancel(long qq, long group, boolean isSupper, long mid) {
            var key = new QQ_GroupRecord(qq, group, mid);

            var func = getDeleteFunc(key);

            try {
                userListeners.compute(qq, func);
                groupListeners.compute(group, func);

                listeners.compute(key, (m, v) -> {
                    if (v == null) {
                        throw new TipsRuntimeException(MatchListenerException.Type.ML_Listen_NotListen.message);
                    }
                    v.stopListener(MatchListener.StopType.USER_STOP);
                    return null;
                });
            } catch (TipsRuntimeException e) {
                if (! isSupper) throw e;
                var keySet = new HashSet<QQ_GroupRecord>();
                Stream.concat(
                        userListeners.values().stream().flatMap(Collection::stream),
                        groupListeners.values().stream().flatMap(Collection::stream)
                ).forEach(k -> {
                    if (k.mid == mid) keySet.add(k);
                });
                keySet.forEach(nKey -> {
                    removeKey(nKey);
                    listeners.remove(nKey).stopListener(MatchListener.StopType.SUPER_STOP);
                });
            }
        }

        static void removeKey(QQ_GroupRecord key) {
            var nFunc = getDeleteFunc(key);
            userListeners.compute(key.qq, nFunc);
            groupListeners.compute(key.group, nFunc);
        }
    }

}

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
import com.now.nowbot.util.JacksonUtil;
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

    @Override
    public void HandleMessage(MessageEvent event, ListenerParam param) throws Throwable {
        if (Objects.equals(param.operate, "stop")) {
            if (event instanceof GroupMessageEvent g) {
                ListenerCheck.cancel(g.getGroup().getId(), g.getSender().getId(), Permission.isSuper(event.getSender().getId()), param.id);
            } else {
                throw new TipsException("不支持的方式");
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
        if (event instanceof GroupMessageEvent g) {
            var sender = g.getSender().getId();
            // 检查有没有重复, 重复直接抛错终止
            ListenerCheck.add(g.getGroup().getId(), sender, Permission.isSuper(sender), listener);
        } else {
            throw new TipsException("不支持的方式");
        }
        from.sendMessage("开始监听比赛 " + param.id);

        // 监听房间结束
        listener.addStopListener((m) -> from.sendMessage("停止监听 " + param.id + "：比赛结束"));

        listener.addEventListener((eventList, newMatch) -> {

            Optional<MatchEvent> matchEventOpt = eventList.stream()
//                                                      这里为啥要判断getRound().getId(), 应该都有吧
                    .filter(s -> Objects.nonNull(s.getRound()) && Objects.nonNull(s.getRound().getId()))
                    .max(Comparator.naturalOrder()); // 这里是取最后一个包含 round 的

            if (matchEventOpt.isEmpty()) {
                // 当前变动没有 game
                return;
            }

            var matchEvent = matchEventOpt.get();
            // 比赛结束 已经有结束的事件监听了
//            if (newMatch.isMatchEnd()) {
//                from.sendMessage("停止监听 " + param.id + "：比赛结束");
//            }

            @SuppressWarnings("all")
            var scores = matchEvent.getRound().getScoreInfoList();

            //刚开始比赛，没分
            if (CollectionUtils.isEmpty(scores)) {
                var b = matchEvent.getRound().getBeatmap();
                var s = b.getBeatMapSet();

                String mapInfo = "(" + b.getId() + ") " + s.getArtistUTF() + " - " + s.getTitleUTF() + " (" + s.getMapperName() + ") [" + b.getVersion() + "]";
                from.sendMessage("比赛 " + param.id + " 已开始！谱面：\n" + mapInfo);
                return;
            }
            //比赛结束，发送成绩
            try {
                var b = matchEvent.getRound().getBeatmap();
                var s1 = b.getBeatMapSet();
                String mapInfo = "(" + b.getId() + ") " + s1.getArtistUTF() + " - " + s1.getTitleUTF() + " (" + s1.getMapperName() + ") [" + b.getVersion() + "]";
                from.sendMessage("比赛 " + param.id + " 结束！谱面：\n" + mapInfo);

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

    private static class ListenerCheck {
        private final static int                            USER_MAX       = 3;
        private final static int                            GROUP_MAX      = 3;
        private final static Map<QQ_GroupRecord, MatchListener> listeners  = new ConcurrentHashMap<>();
        private final static Map<Long, List<QQ_GroupRecord>> userListeners = new ConcurrentHashMap<>();
        private final static Map<Long, List<QQ_GroupRecord>> groupListeners = new ConcurrentHashMap<>();

        static void add(long qq, long group, boolean isSuper, MatchListener listener) {
            long mid = listener.getMatchID();
            boolean notSuper = ! isSuper;
            var key = new QQ_GroupRecord(qq, group, mid);

            userListeners.compute(qq, (q, uList) -> {
                if (uList == null) uList = new ArrayList<>(USER_MAX);
                if (uList.size() > USER_MAX && notSuper) throw new TipsRuntimeException("你已经监听已达最大个数");
                if (uList.contains(key) && notSuper) throw new TipsRuntimeException("你已经监听过了");
                uList.add(key);
                return uList;
            });

            groupListeners.compute(group, (g, gList) -> {
                if (gList == null) gList = new ArrayList<>(GROUP_MAX);
                if (gList.size() > GROUP_MAX && notSuper) throw new TipsRuntimeException("这个群监听已达最大个数");
                if (gList.contains(key) && notSuper) throw new TipsRuntimeException("此群已经监听过了");
                gList.add(key);
                return gList;
            });

            listeners.put(key, listener);
        }

        static void cancel(long qq, long group, boolean isSupper, long mid) {
            var key = new QQ_GroupRecord(qq, group, mid);
            if (isSupper) {
//                return;
            }

            MatchListenerService.log.info(JacksonUtil.objectToJsonPretty(userListeners));
            MatchListenerService.log.info(JacksonUtil.objectToJsonPretty(groupListeners));

            BiFunction<Long, List<QQ_GroupRecord>, List<QQ_GroupRecord>> func = (id, list) -> {
                if (list == null || ! list.contains(key)) throw new TipsRuntimeException("没听过");
                list.remove(key);
                return CollectionUtils.isEmpty(list) ? null : list;
            };

            userListeners.compute(qq, func);
            groupListeners.compute(group, func);

            listeners.compute(key, (m, v) -> {
                if (v == null) throw new TipsRuntimeException("没监听过");
                return null;
            });

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
    }
}

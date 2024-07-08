package com.now.nowbot.model.multiplayer;

import com.now.nowbot.model.JsonData.Match;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import com.now.nowbot.throwable.TipsRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MatchListener {
    private static final Logger log = LoggerFactory.getLogger(MatchListener.class);
    private static final ScheduledExecutorService executorService;

    static {
        var threadFactory = Thread.ofVirtual().name("v-MatchListener", 50).factory();
        executorService = Executors.newScheduledThreadPool(Integer.MAX_VALUE, threadFactory);
    }

    Match match;
    OsuMatchApiService matchApiService;

    public void addStopListener(BiConsumer<Match, StopType> listener) {
        endListener.add(listener);
    }

    List<BiConsumer<List<Match.MatchEvent>, Match>> consumerList = new ArrayList<>();
    List<Consumer<Match>> startListener = new ArrayList<>();
    List<BiConsumer<Match, StopType>> endListener = new ArrayList<>();
    long                              matchID;
    volatile long eventID;
    private ScheduledFuture<?> future;
    private ScheduledFuture<?> kill;

    public MatchListener(Match match, OsuMatchApiService service) {
        this.match = match;
        this.matchApiService = service;
        matchID = match.getMatchStat().getMatchID();
    }

    public void addEventListener(BiConsumer<List<Match.MatchEvent>, Match> doListener) {
        consumerList.add(doListener);
        if (isStart() && Objects.nonNull(match.getCurrentGameID())) {
            doListener.accept(getLastRound(match.getEvents()), match);
        }
    }

    private List<Match.MatchEvent> getLastRound(List<Match.MatchEvent> events) {
        Match.MatchEvent e = null;
        var iter = events.listIterator(events.size());
        while (iter.hasPrevious()) {
            var event = iter.previous();
            if (Objects.nonNull(event.getRound())) {
                e = event;
                break;
            }
        }
        if (Objects.isNull(e)) {
            throw new TipsRuntimeException("查询状态异常");
        }
        if (eventID == e.getEventID()) {
            eventID = e.getEventID() - 1;
        }
        return List.of(e);
    }

    public void addStartListener(Consumer<Match> listener) {
        startListener.add(listener);
        if (isStart()) {
            listener.accept(match);
        }
    }

    private void listen() {
        try {
            if (match.isMatchEnd()) {
                this.stopListener(StopType.MATCH_END);
            }
            var newMatch = matchApiService.getMatchInfoAfter(matchID, eventID);

            if (newMatch.getLatestEventID() == eventID) return;

            if (Objects.nonNull(newMatch.getCurrentGameID())) {
                // 如果正在进行, newMatch.getEvents().getFirst() 一定是当前开始的对局
                var m = newMatch.getEvents().getFirst();
                if (m.getEventID() - 1 != eventID) {
                    eventID = m.getEventID() - 1;
                    onEvents(newMatch.getEvents(), match);
                }
                return;
            } else {
                eventID = newMatch.getLatestEventID();
            }

            match.parseNextData(newMatch);
            onEvents(newMatch.getEvents(), match);
        } catch (Exception e) {
            log.error("监听比赛期间出现错误: ", e);
        }
    }

    public synchronized void startListener() {
        if (isStart()) {
            return;
        }

        if (match.isMatchEnd()) {
            onStart();
            onStop(StopType.MATCH_END);
            return;
        }

        eventID = match.getLatestEventID();

        if (Objects.nonNull(match.getCurrentGameID())) {
            List<Match.MatchEvent> gameOpt = getLastRound(match.getEvents());
            onEvents(gameOpt, match);
        }

        onStart();

        future = executorService.scheduleAtFixedRate(this::listen, 0, 10, TimeUnit.SECONDS);
        kill = executorService.schedule(() -> {
            if (this.isStart()) {
                this.stopListener(StopType.TIME_OUT);
            }
        }, 6, TimeUnit.HOURS);
    }

    private void onStart() {
        startListener.forEach(c -> Thread.startVirtualThread(() -> c.accept(this.match)));
    }

    private void onStop(StopType type) {
        endListener.forEach(c -> Thread.startVirtualThread(() -> c.accept(this.match, type)));
    }

    public boolean isStart() {
        return Objects.nonNull(future) && !future.isDone();
    }

    private void onEvents(List<Match.MatchEvent> events, Match match) {
        consumerList.forEach(c -> Thread.startVirtualThread(() -> c.accept(events, match)));
    }

    public void stopListener(StopType type) {
        if (isStart()) {
            if (!Objects.equals(type, StopType.TIME_OUT)) {
                kill.cancel(true);
            }
            future.cancel(true);
            onStop(type);
        }
    }

    public void removeListener(BiConsumer<List<Match.MatchEvent>, Match> consumer) {
        consumerList.remove(consumer);
    }

    public void removeListener(Consumer<Match> start) {

        startListener.remove(start);
    }

    public void removeListener(BiConsumer<Match, StopType> consumer, StopType type) {
        consumer.accept(match, type);
        endListener.remove(consumer);
    }

    public enum StopType {
        MATCH_END("比赛正常结束"),
        USER_STOP("调用者关闭"),
        SUPER_STOP("超级管理员关闭"),
        SERVICE_STOP("服务器重启"),
        TIME_OUT("超时了"),
        ;
        final String tips;

        StopType(String t) {
            tips = t;
        }

        public String getTips() {
            return tips;
        }
    }

    public long getMatchID() {
        return matchID;
    }
}

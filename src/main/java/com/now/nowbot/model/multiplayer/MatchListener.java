package com.now.nowbot.model.multiplayer;

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
    private static final Logger                   log = LoggerFactory.getLogger(MatchListener.class);
    private static final ScheduledExecutorService executorService;

    static {
        var threadFactory = Thread.ofVirtual().name("v-MatchListener", 50).factory();
        executorService = Executors.newScheduledThreadPool(Integer.MAX_VALUE, threadFactory);
    }

    Match                                     match;
    OsuMatchApiService                        matchApiService;

    public void addStopListener(BiConsumer<Match, StopType> listener) {
        endListner.add(listener);
    }

    List<BiConsumer<List<MatchEvent>, Match>> consumerList = new ArrayList<>();
    List<Consumer<Match>>             startListner = new ArrayList<>();
    List<BiConsumer<Match, StopType>> endListner   = new ArrayList<>();
    long                              matchID;
    volatile long recordID;
    private ScheduledFuture<?> future;
    private ScheduledFuture<?> kill;

    public MatchListener(Match match, OsuMatchApiService service) {
        this.match = match;
        this.matchApiService = service;
        matchID = match.getMatchStat().getId();
    }

    public void addEventListener(BiConsumer<List<MatchEvent>, Match> doListener) {
        consumerList.add(doListener);
        if (isStart() && Objects.nonNull(match.getCurrentGameId())) {
            doListener.accept(getLastRound(match.getEvents()), match);
        }
    }

    private List<MatchEvent> getLastRound(List<MatchEvent> events) {
        MatchEvent e = null;
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
        if (recordID == e.getId()) {
            recordID = e.getId() - 1;
        }
        return List.of(e);
    }

    public void addStartListener(Consumer<Match> listener) {
        startListner.add(listener);
        if (isStart()) {
            listener.accept(match);
        }
    }

    private void listen() {
        try {
            if (match.isMatchEnd()) {
                this.stopListener(StopType.MATCH_END);
            }
            var newMatch = matchApiService.getMatchInfoAfter(matchID, recordID);

            if (newMatch.getLatestEventId() == recordID) return;

            if (Objects.nonNull(newMatch.getCurrentGameId())) {
                // 如果正在进行, newMatch.getEvents().getFirst() 一定是当前开始的对局
                var m = newMatch.getEvents().getFirst();
                if (m.getId() - 1 != recordID) {
                    recordID = m.getId() - 1;
                    onEvents(newMatch.getEvents(), match);
                }
                return;
            } else {
                recordID = newMatch.getLatestEventId();
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

        recordID = match.getLatestEventId();

        if (Objects.nonNull(match.getCurrentGameId())) {
            List<MatchEvent> gameOpt = getLastRound(match.getEvents());
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
        startListner.forEach(c -> Thread.startVirtualThread(() -> c.accept(this.match)));
    }

    private void onStop(StopType type) {
        endListner.forEach(c -> Thread.startVirtualThread(() -> c.accept(this.match, type)));
    }

    public boolean isStart() {
        return Objects.nonNull(future) && !future.isDone();
    }

    private void onEvents(List<MatchEvent> events, Match match) {
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

    public void removeListener(BiConsumer<List<MatchEvent>, Match> consumer) {
        consumerList.remove(consumer);
    }

    public void removeListener(Consumer<Match> start) {

        startListner.remove(start);
    }

    public void removeListener(BiConsumer<Match, StopType> consumer, StopType type) {
        consumer.accept(match, type);
        endListner.remove(consumer);
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

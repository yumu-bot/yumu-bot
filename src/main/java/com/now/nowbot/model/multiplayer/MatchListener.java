package com.now.nowbot.model.multiplayer;

import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MatchListener {
    List<BiConsumer<Match, StopType>>         endListner   = new ArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(MatchListener.class);
    private static final ScheduledExecutorService executorService;

    static {
        var threadFactory = Thread.ofVirtual().name("v-MatchListener", 50).factory();
        executorService = Executors.newScheduledThreadPool(Integer.MAX_VALUE, threadFactory);
    }

    Match                                     match;
    OsuMatchApiService                        matchApiService;
    List<BiConsumer<List<MatchEvent>, Match>> consumerList = new ArrayList<>();

    public void addStopListener(BiConsumer<Match, StopType> listener) {
        endListner.add(listener);
    }
    List<Consumer<Match>>                     startListner = new ArrayList<>();
    long                                      matchID;
    long                                      recordID;
    private ScheduledFuture<?> future;
    private ScheduledFuture<?> kill;

    public MatchListener(Match match, OsuMatchApiService service) {
        this.match = match;
        this.matchApiService = service;
        matchID = match.getMatchStat().getId();
    }

    public void addEventListener(BiConsumer<List<MatchEvent>, Match> doListener) {
        consumerList.add(doListener);
    }

    /**
     * 如果在 .startListener() 之后添加, 则无效
     */
    public void addStartListener(Consumer<Match> listener) {
        startListner.add(listener);
    }

    public synchronized void startListener() {
        if (isStart()) {
            return;
        }

        if (match.isMatchEnd()) {
            startListner.forEach(c -> c.accept(match));
            endListner.forEach(c -> c.accept(match, StopType.MATCH_END));
            return;
        }

        recordID = match.getLatestEventId();

        if (Objects.nonNull(match.getCurrentGameId())) {
            Optional<MatchEvent> gameOpt = getLastRound(match.getEvents());
            var game = gameOpt.orElseThrow();
            recordID = game.getId() - 1;
            onEvents(List.of(game), match);
        }

        startListner.forEach(c -> c.accept(match));

        future = executorService.scheduleAtFixedRate(this::listen, 0, 10, TimeUnit.SECONDS);
        kill = executorService.schedule(()-> {
            if (this.isStart()) {
                this.stopListener(StopType.TIME_OUT);
            }
        }, 6, TimeUnit.HOURS);
    }

    private void onEvents(List<MatchEvent> events, Match match) {
        consumerList.forEach(c -> c.accept(events, match));
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

    public boolean isStart() {
        return Objects.nonNull(future) && ! future.isDone();
    }

    private Optional<MatchEvent> getLastRound(List<MatchEvent> events) {
        MatchEvent e = null;
        var iter = events.listIterator(events.size());
        while (iter.hasPrevious()) {
            var event = iter.previous();
            if (Objects.nonNull(event.getRound())) {
                e = event;
                break;
            }
        }
        return Optional.ofNullable(e);
    }

    public void stopListener(StopType type) {
        if (isStart()) {
            if (!Objects.equals(type, StopType.TIME_OUT)) {
                kill.cancel(true);
            }
            future.cancel(true);
            endListner.forEach(c -> c.accept(match, type));
        }
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

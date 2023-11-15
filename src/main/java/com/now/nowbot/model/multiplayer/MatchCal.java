package com.now.nowbot.model.multiplayer;

import com.now.nowbot.model.JsonData.MicroUser;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MatchCal {
    Match match;
    Map<Long, MicroUser> users;
    // 这个流已经确认过滤为只包含对局
    // 原始 event 用 Match.getgetEvents()
    Stream<MatchRound> gameEvents;

    public MatchCal(Match match) {
        this.match = match;
        users = match.getPlayers().stream().collect(Collectors.toMap(MicroUser::getId, u -> u, (u1, u2) -> u2));
        gameEvents = match.getEvents().stream().map(MatchEvent::getRound).filter(Objects::nonNull);
    }

    public MicroUser getUser(long id) {
        return users.get(id);
    }

    public List<MatchRound> getAllRound() {
        return gameEvents.collect(Collectors.toList());
    }

    /**
     * @param rematch 是否包含重赛, true 为包含; false 为去重, 去重操作为保留最后一个
     * @param remove 是否删除低于 1w 的成绩
     * @return 对局 Round
     */
    public List<MatchRound> getGameRoundWidthScore(boolean rematch, boolean remove) {
        var result = gameEvents
                .filter(matchRound -> !CollectionUtils.isEmpty(matchRound.getScoreInfoList()));
        if (remove) {
            result = result.peek(matchRound -> matchRound.getScoreInfoList().removeIf(s -> s.getScore() <= 10000));
        }
        if (rematch) {
            return result.collect(Collectors.toList());
        } else {
            return new ArrayList<>(result.collect(Collectors.toMap(MatchRound::getBid, e -> e, (o, n) -> n, LinkedHashMap::new)).values());
        }
    }
}

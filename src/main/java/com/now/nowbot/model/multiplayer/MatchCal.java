package com.now.nowbot.model.multiplayer;

import com.now.nowbot.model.JsonData.MicroUser;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MatchCal {
    Match match;
    Map<Long, MicroUser> users;
    Stream<MatchRound> gameEvents;

    public MatchCal(Match match) {
        // 写了常用的, 别的有需要, 跟我说一声
        this.match = match;
        users = match.getPlayers().stream().collect(Collectors.toMap(MicroUser::getId, u -> u, (u1, u2) -> u2));
        gameEvents = match.getEvents().stream().map(MatchEvent::getRound).filter(Objects::nonNull);
    }

    public MicroUser getUser(long id) {
        return users.get(id);
    }

    public List<MatchRound> getAllGameRound() {
        return gameEvents.collect(Collectors.toList());
    }

    /**
     * @param rematch 是否包含重赛, true 为包含; false 为去重, 去重操作为保留最后一个
     * @return 对局 Round
     */
    public List<MatchRound> getGameRoundWidthScore(boolean rematch) {
        var result = gameEvents.filter(matchRound -> !CollectionUtils.isEmpty(matchRound.getScoreInfoList()));
        if (rematch) {
            return result.collect(Collectors.toList());
        } else {
            return new ArrayList<>(result.collect(Collectors.toMap(MatchRound::getBid, e -> e, (o, n) -> n, LinkedHashMap::new)).values());
        }
    }
}

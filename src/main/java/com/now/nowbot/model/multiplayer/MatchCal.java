package com.now.nowbot.model.multiplayer;

import com.now.nowbot.model.JsonData.MicroUser;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class MatchCal {
    Match match;
    Map<Long, MicroUser> users;
    List<MicroUser> players;
    // 这个流已经确认过滤为只包含对局
    // 原始 event 用 Match.getgetEvents()
    List<MatchRound> gameRounds;
    List<MatchScore> cache;

    /**
     * @param rematch 是否包含重赛, true 为包含; false 为去重, 去重操作为保留最后一个
     * @param remove 是否删除低于 1w 的成绩
     */
    public MatchCal(Match match, boolean rematch, boolean remove) {
        this.match = match;
        users = match.getPlayers().stream().collect(Collectors.toMap(MicroUser::getId, u -> u, (u1, u2) -> u2));
        var roundsStream = match.getEvents().stream()
                .map(MatchEvent::getRound)
                .filter(Objects::nonNull)
                .filter(matchRound -> !CollectionUtils.isEmpty(matchRound.getScoreInfoList()))
                .filter(matchRound -> matchRound.getEndTime() != null);
        if (remove) {
            roundsStream = roundsStream.peek(matchRound -> matchRound.getScoreInfoList().removeIf(s -> s.getScore() <= 10000));
        }

        if (rematch) {
            gameRounds = roundsStream.collect(Collectors.toList());
        } else {
            gameRounds = new ArrayList<>(roundsStream.
                    collect(Collectors.toMap(MatchRound::getBid, e -> e, (o, n) -> n, LinkedHashMap::new))
                    .values());
        }
        cache = gameRounds.stream()
                .flatMap(r -> r.scoreInfoList.stream())
                .filter(s -> s.getScore() > 1000)
                .collect(Collectors.toList());
        Set<Long> playerUid = cache.stream()
                .map(MatchScore::getUserId).collect(Collectors.toCollection(LinkedHashSet::new));
//        等OsuUserApiService接口实现写好了用注释的这个, 或者另外想办法搞个兜底的
//        players = playerUid.stream().map(uid -> users.computeIfAbsent(uid, _uid -> userApiService.getPlayerInfo(_uid))).toList();
        players = playerUid.stream().map(uid -> users.get(uid)).toList();
    }

    public MicroUser getUser(long id) {
        return users.get(id);
    }

    public List<MatchRound> getGameRounds() {
        return gameRounds;
    }

    public void sikp(int start, int end) {
        int oldGameSize = gameRounds.size();
        if (start < 0 || start >= oldGameSize || end < 0 || end >= oldGameSize || start + end >= oldGameSize) return;
        gameRounds = getGameRounds().stream()
                .limit(oldGameSize - end)
                .skip(start)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<MicroUser> getPlayers(String teamType) {
        return cache
                .stream()
                .filter(s -> s.getMatchPlayerStat().getTeam().equals(teamType))
                .map(s -> users.get(s.getUserId()))
                .toList();
    }

    public List<MicroUser> getRedPlayers() {
        return getPlayers("red");
    }


    public List<MicroUser> getBluePlayers() {
        return getPlayers("blue");
    }


    public List<MicroUser> getNonTeamPlayers() {
        return getPlayers("none");
    }

    /**
     * 取对局选图的平均星级, 不精确, 对丢失铺面信息, mod 未作处理
     *
     * @return 平均星级
     */
    public float getAverageStar() {
        return (float) gameRounds.stream()
                .filter(gameRounds -> gameRounds.getBeatmap() != null)
                .mapToDouble(gameRounds -> gameRounds.getBeatmap().getDifficultyRating())
                .average()
                .orElse(0d);
    }

    /**
     * 获取第一个sid 当作背景图? (大概率选到热手图上去了
     *
     * @return sid
     */
    public long getBgSid() {
        for (var r : gameRounds) {
            if (r.getBeatmap() != null) {
                return r.getBid();
            }
        }
        return 0;
    }
}

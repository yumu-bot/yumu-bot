package com.now.nowbot.model.multiplayer;

import com.now.nowbot.model.JsonData.MicroUser;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class MatchCal {
    static final String NAME = "MC";
    Match match;
    Map<Long, MicroUser> users;
    List<MicroUser> players;
    // gameRounds 为只包含对局，其他 events 走 Match 那边取
    List<MatchRound> gameRounds;
    List<MatchScore> cache;

    /**
     * @param remove 是否删除低于 1w 的成绩，true 为删除，false 为保留
     * @param rematch 是否包含重赛, true 为包含; false 为去重, 去重操作为保留最后一个
     */
    public MatchCal(Match match, int skip, int skipEnd, boolean remove, boolean rematch) {
        this.match = match;
        users = match.getPlayers().stream().collect(Collectors.toMap(MicroUser::getId, u -> u, (u1, u2) -> u2));
        var roundsStream = match.getEvents().stream()
                .map(MatchEvent::getRound)
                .filter(Objects::nonNull)
                .filter(matchRound -> !CollectionUtils.isEmpty(matchRound.getScoreInfoList()))
                .filter(matchRound -> matchRound.getEndTime() != null);

        skip(skip, skipEnd);

        if (remove) {
            roundsStream = roundsStream.peek(matchRound -> matchRound.getScoreInfoList().removeIf(s -> s.getScore() <= 10000));
        }

        if (rematch) {
            gameRounds = new ArrayList<>(roundsStream.
                    collect(Collectors.toMap(MatchRound::getBid, e -> e, (o, n) -> n, LinkedHashMap::new))
                    .values());
            // 剔除成绩不足 1w 的玩家
            cache = gameRounds.stream()
                    .flatMap(r -> r.scoreInfoList.stream())
                    .filter(s -> s.getScore() > 10000)
                    .collect(Collectors.toList());
        } else {
            gameRounds = roundsStream.collect(Collectors.toList());
            cache = gameRounds.stream()
                    .flatMap(r -> r.scoreInfoList.stream())
                    .collect(Collectors.toList());
        }
        skip(skip, skipEnd);

        Set<Long> playerUid = cache.stream()
                .map(MatchScore::getUserId).collect(Collectors.toCollection(LinkedHashSet::new));
        // 不需要get啊...他不是给你默认的microUser了吗？那个是现成的不用走 API，重复获取也太占用 API 了
//        用来兜底的, 比如说ppy给的就不够(好像不太可能) 以及被删号的? 不需要也没事, 删了就行
//        computeIfAbsent() 是如果 map 里不存在这个 key 或者 value 为 null 才执行查询函数, 并且将这个结果再插入到 map 里
        // 背景什么的我再想办法
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

    public void skip(int skip, int skipEnd) {
        int size = gameRounds.size();
        int limit = size - skipEnd;

        if (skip < 0 || skip > size || limit < 0 || limit > size || skip + skipEnd > size) return;
        gameRounds = getGameRounds().stream()
                .limit(limit)
                .skip(skip)
                .collect(Collectors.toList());
    }

    public List<MicroUser> getPlayers() {
        return players;
    }

    /**
     * @param teamType blue, red, none
     */
    public List<MicroUser> getPlayers(String teamType) {
        return cache
                .stream()
                .filter(s -> s.getMatchPlayerStat().getTeam().equals(teamType))
                .map(s -> users.get(s.getUserId()))
                .toList();
    }

    public void setPlayers(List<MicroUser> players) {
        this.players = players;
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
    public long getFirstMapSID() {
        for (var r : gameRounds) {
            if (r.getBeatmap() != null) {
                return r.getBeatmap().getBeatmapsetId();
            }
        }
        return 0;
    }
}

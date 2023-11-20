package com.now.nowbot.model.multiplayer;

import com.now.nowbot.model.JsonData.MicroUser;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MatchCal {
    Match match;
    Map<Long, MicroUser> playerMap;
    List<MicroUser> players;
    // gameRounds 只包含对局，其他 events 走 Match 那边取
    List<MatchRound> roundList;
    List<MatchScore> scoreList;

    /**
     * @param remove 是否删除低于 1w 的成绩，true 为删除，false 为保留
     * @param rematch 是否包含重赛, true 为包含; false 为去重, 去重操作为保留最后一个
     */
    public MatchCal(Match match, int skip, int skipEnd, boolean remove, boolean rematch) {
        this.match = match;

        //包含所有玩家的映射表
        playerMap = match.getPlayers().stream().collect(Collectors.toMap(MicroUser::getId, p -> p, (p1, p2) -> p2));
        var roundsStream = match.getEvents().stream()
                .map(MatchEvent::getRound)
                .filter(Objects::nonNull)
                .filter(matchRound -> !CollectionUtils.isEmpty(matchRound.getScoreInfoList()))
                .filter(matchRound -> matchRound.getEndTime() != null);

        if (remove) {
            roundsStream = roundsStream.peek(matchRound -> matchRound.getScoreInfoList().removeIf(s -> s.getScore() <= 10000));
        }

        if (rematch) {
            roundList = roundsStream.collect(Collectors.toList());
        } else {
            roundList = new ArrayList<>(roundsStream.
                    collect(Collectors.toMap(MatchRound::getBid, e -> e, (o, n) -> n, LinkedHashMap::new))
                    .values());
        }

        skip(skip, skipEnd);

        constructScoreList();

        Set<Long> playerUIDSet = scoreList.stream().map(MatchScore::getUserId).collect(Collectors.toCollection(LinkedHashSet::new));

//        等OsuUserApiService接口实现写好了用注释的这个, 或者另外想办法搞个兜底的
//        players = playerUid.stream().map(uid -> users.computeIfAbsent(uid, _uid -> userApiService.getPlayerInfo(_uid))).toList();

        players = playerUIDSet.stream().map(playerMap::get).toList();
        playerMap = players.stream().collect(Collectors.toMap(MicroUser::getId, m -> m));

        addPlayerName4MatchScore();
    }

    private void constructScoreList() {
        scoreList = roundList.stream()
                .flatMap(r -> r.scoreInfoList.stream())
                .filter(s -> s.getScore() > 10000)
                .toList();
    }

    //默认跳过
    private void skip(int skip, int skipEnd) {
        int size = roundList.size();
        int limit = size - skipEnd;

        if (skip < 0 || skip > size || limit < 0 || limit > size || skip + skipEnd > size) return;
        roundList = getRoundList().stream()
                .limit(limit)
                .skip(skip)
                .collect(Collectors.toList());
    }

    //默认设置
    private void addPlayerName4MatchScore() {
        for (MicroUser p: players) {
            for (MatchScore s: scoreList) {
                if (Objects.equals(p.getId(), s.getUserId())) {
                    s.setUserName(p.getUserName());
                }
            }
        }
    }

    public void addMicroUser4MatchScore() {
        for (MicroUser p: players) {
            for (MatchScore s: scoreList) {
                if (Objects.equals(p.getId(), s.getUserId()) && s.getUser() == null) {
                    s.setUser(p);
                }
            }
        }
    }

    //不知道为什么，这里总是会筛出 10000 分以下的
    public void addRanking4MatchScore() {
        for (MatchRound r: roundList) {
            AtomicInteger i = new AtomicInteger(1);

            var scores = r.getScoreInfoList();
            r.setScoreInfoList(scores.stream()
                    .filter(s -> s.getScore() > 10000)
                    .sorted(Comparator.comparing(MatchScore::getScore).reversed())
                    .peek(s -> s.setRanking(i.getAndIncrement()))
                    .toList());
        }
        //重置分数列表
        constructScoreList();
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public String getPlayerName(long id) {
        return playerMap.get(id).getUserName();
    }

    public Map<Long, String> getPlayerNameMap() {
        return playerMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getUserName()));
    }

    public Map<Long, MicroUser> getPlayerMap() {
        return playerMap;
    }

    public void setPlayerMap(Map<Long, MicroUser> playerMap) {
        this.playerMap = playerMap;
    }

    public List<MatchRound> getRoundList() {
        return roundList;
    }

    public void setRoundList(List<MatchRound> roundList) {
        this.roundList = roundList;
    }

    public List<MatchScore> getScoreList() {
        return scoreList;
    }

    public void setScoreList(List<MatchScore> scoreList) {
        this.scoreList = scoreList;
    }

    public List<MicroUser> getPlayers() {
        return players;
    }

    /**
     * @param teamType blue, red, none
     */
    public List<MicroUser> getPlayers(String teamType) {
        return scoreList
                .stream()
                .filter(s -> s.getTeam().equals(teamType))
                .map(s -> playerMap.get(s.getUserId()))
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
        return (float) roundList.stream()
                .filter(round -> round.getBeatmap() != null)
                .mapToDouble(round -> round.getBeatmap().getDifficultyRating())
                .average()
                .orElse(0d);
    }

    /**
     * 获取第一个sid 当作背景图? (大概率选到热手图上去了
     *
     * @return sid
     */
    public long getFirstMapSID() {
        for (var r : roundList) {
            if (r.getBeatmap() != null) {
                return r.getBeatmap().getBeatmapsetId();
            }
        }
        return 0;
    }
}

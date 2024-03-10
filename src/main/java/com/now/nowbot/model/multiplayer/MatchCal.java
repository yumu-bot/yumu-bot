package com.now.nowbot.model.multiplayer;

import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.enums.Mod;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MatchCal {
    Match match;
    Map<Long, MicroUser> playerMap;
    List<MicroUser> playerList;
    // gameRounds 只包含对局，其他 events 走 Match 那边取
    List<MatchRound> roundList;
    List<MatchScore> scoreList;

    // easy Mod 倍率
    double easyMultiplier;

    /**
     * @param delete 是否保留低于 1w 的成绩，true 为删除，false 为保留
     * @param rematch 是否去重赛, true 为包含; false 为去重, 去重操作为保留最后一个
     */
    public MatchCal(Match match, int skip, int ignore, List<Integer> remove, double easy, boolean delete, boolean rematch) {
        this.match = match;
        this.easyMultiplier = easy;

        //包含所有玩家的映射表
        playerMap = match.getPlayers().stream().collect(Collectors.toMap(MicroUser::getId, p -> p, (p1, p2) -> p2));
        var roundsStream = match.getEvents().stream()
                .map(MatchEvent::getRound)
                .filter(Objects::nonNull)
                .filter(round -> (Objects.nonNull(round.getScoreInfoList()) && !round.getScoreInfoList().isEmpty()))
                .filter(round -> round.getEndTime() != null);

        if (delete) {
            roundsStream = roundsStream.peek(round -> {
                try {
                    round.getScoreInfoList().removeIf(s -> s.getScore() <= 10000);
                } catch (UnsupportedOperationException ignored) {
                    //我不清楚为什么不能移除，难道是数组内最后一个元素？
                }
            });
        }

        if (rematch) {
            roundList = roundsStream.collect(Collectors.toList());
        } else {
            roundList = new ArrayList<>(roundsStream.
                    collect(Collectors.toMap(MatchRound::getBid, e -> e, (o, n) -> n, LinkedHashMap::new))
                    .values());
        }

        skip(skip, ignore, remove);

        applyEasyMultiplier();

        constructScoreList();

        Set<Long> playerUIDSet = scoreList.stream().map(MatchScore::getUserId).collect(Collectors.toCollection(LinkedHashSet::new));

//        等OsuUserApiService接口实现写好了用注释的这个, 或者另外想办法搞个兜底的
//        players = playerUid.stream().map(uid -> users.computeIfAbsent(uid, _uid -> userApiService.getPlayerInfo(_uid))).toList();

        playerList = playerUIDSet.stream().map(playerMap::get).toList();
        playerMap = playerList.stream().collect(Collectors.toMap(MicroUser::getId, m -> m));

        addPlayerName4MatchScore();
        addMicroUser4MatchScore();
        addRanking4MatchScore();
    }

    private void applyEasyMultiplier() {
        // easy 处理
        if (easyMultiplier != 1d) {
            roundList = roundList.stream().peek(round -> round.setScoreInfoList(
                    round.getScoreInfoList().stream().peek(
                            s -> {
                                if (Mod.hasEz(Mod.getModsValue(s.getMods()))) {
                                    s.setScore((int) (s.getScore() * easyMultiplier));
                                }
                            }).toList())
            ).toList();
        }
    }

    private void constructScoreList() {
        scoreList = roundList.stream()
                .flatMap(r -> r.scoreInfoList.stream())
                .filter(s -> s.getScore() > 10000)
                .toList();
    }

    //默认跳过
    private void skip(int skip, int ignore, List<Integer> remove) {
        int size = roundList.size();
        int limit = size - ignore;

        if (skip < 0 || skip > size || limit < 0 || limit > size || limit - skip < 0) return;

        roundList = getRoundList().stream()
                .limit(limit)
                .skip(skip)
                .collect(Collectors.toList());

        if (Objects.nonNull(remove) && ! remove.isEmpty()) {
            remove = remove.stream().map(i -> i - skip).filter(i -> i < limit - skip && i > 0).map(i -> i - 1).toList();

            for (int i = 0; i < roundList.size(); i++) {
                for (var m : remove) {
                    if (i == m) {
                        roundList.set(i, null);
                        break;
                    }
                }
            }

            roundList = roundList.stream().filter(Objects::nonNull).toList();
        }
    }

    //默认设置
    private void addPlayerName4MatchScore() {
        for (MatchScore s: scoreList) {
            for (MicroUser p: playerList) {
                if (Objects.equals(p.getId(), s.getUserId())) {
                    s.setUserName(p.getUserName());
                    break;
                }
            }
        }
    }

    //默认设置
    public void addMicroUser4MatchScore() {
        for (MatchScore s: scoreList) {
            for (MicroUser p: playerList) {
                if (Objects.equals(p.getId(), s.getUserId()) && s.getUser() == null) {
                    s.setUser(p);
                    break;
                }
            }
        }
    }

    //默认设置
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

    public List<MicroUser> getPlayerList() {
        return playerList;
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

    public void setPlayerList(List<MicroUser> playerList) {
        this.playerList = playerList;
    }

    /**
     * 取对局选图的平均星级, 不精确, 对丢失铺面信息, mod 未作处理
     *
     * @return 平均星级
     */
    public float getAverageStar() {
        return (float) roundList.stream()
                .filter(round -> round.getBeatmap() != null)
                .mapToDouble(round -> round.getBeatmap().getStarRating())
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
                return r.getBeatmap().getSID();
            }
        }
        return 0;
    }
}

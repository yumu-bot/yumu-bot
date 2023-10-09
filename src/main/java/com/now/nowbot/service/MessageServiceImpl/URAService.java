package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.match.*;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.MRAException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service("URA")
public class URAService implements MessageService<Matcher> {
    @Autowired
    RestTemplate template;

    @Autowired
    OsuGetService osuGetService;

    public static record RatingData(boolean isTeamVs, int red, int blue, String type, List<UserMatchData> allUsers) {
    }

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(u{1,2})(rating|ra(?![a-zA-Z_]))+\\s*(?<matchid>\\d+)(\\s*(?<skipedrounds>\\d+))?(\\s*(?<deletendrounds>\\d+))?(\\s*(?<excludingrematch>[Rr]))?(\\s*(?<excludingfail>[Ff]))?");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        int matchId = Integer.parseInt(matcher.group("matchid"));
        int skipedRounds = matcher.group("skipedrounds") == null ? 0 : Integer.parseInt(matcher.group("skipedrounds"));
        int deletEndRounds = matcher.group("deletendrounds") == null ? 0 : Integer.parseInt(matcher.group("deletendrounds"));
        boolean includingRematch = matcher.group("excludingrematch") == null || !matcher.group("excludingrematch").equalsIgnoreCase("r");
        boolean includingFail = matcher.group("excludingfail") == null || !matcher.group("excludingfail").equalsIgnoreCase("f");
        var from = event.getSubject();

        Match match = osuGetService.getMatchInfo(matchId);
        while (!match.getFirstEventId().equals(match.getEvents().get(0).getID())) {
            var events = osuGetService.getMatchInfo(matchId, match.getEvents().get(0).getID()).getEvents();
            match.getEvents().addAll(0, events);
        }

        var data = calculate(match, skipedRounds, deletEndRounds, includingFail, includingRematch, osuGetService);
        List<UserMatchData> finalUsers = data.allUsers;

        //结果数据
        StringBuilder sb = new StringBuilder();
        sb.append(match.getMatchInfo().getName()).append("\n")
                .append(data.red).append(" : ")
                .append(data.blue).append("\n")
                .append("mp").append(matchId).append(" ").append(data.type).append("\n");

        for (int i = 0; i < finalUsers.size(); i++) {
            var user = finalUsers.get(i);
            sb.append(String.format("#%d [%.2f] %s (%s)", i + 1, user.getMRA(), user.getUsername(), user.getTeam().toUpperCase()))
                    .append("\n")
                    .append(String.format("%dW-%dL %d%% (%.2fM) [%s]", user.getWins(), user.getLost(),
                            Math.round((double) user.getWins() * 100 / (user.getWins() + user.getLost())), user.getTotalScore(), user.getPlayerLabelV2()))
                    .append("\n\n");

        }

        try {
            from.sendMessage(sb.toString());
        } catch (Exception e) {
            NowbotApplication.log.error("URA 数据请求失败", e);
            throw new MRAException(MRAException.Type.RATING_URA_Error);
            //from.sendMessage("URA 输出失败，请重试。\n或尝试最新版渲染 !ra <mpid>。");
        }
    }

    public static RatingData calculate(Match match, int skipFirstRounds, int deleteLastRounds, boolean includingFail, boolean includingRematch, OsuGetService osuGetService) {
        //存储计算信息
        MatchStatistics matchStatistics = new MatchStatistics();

        List<GameInfo> games = new ArrayList<>();
        var JUsers = match.getUsers();
        Map<Integer, UserMatchData> users = new HashMap<>();
        matchStatistics.setUsers(users);

        //获取所有user
        for (var jUser : JUsers) {
            try {
                users.put(jUser.getId().intValue(), new UserMatchData(osuGetService.getPlayerInfo(jUser.getId())));
            } catch (Exception e) {
                users.put(jUser.getId().intValue(), new UserMatchData(jUser.getId().intValue(), "UID:" + jUser.getId().intValue()));
            }
        }

        //获取所有轮的游戏
        for (var matchEvent : match.getEvents()) {
            if (matchEvent.getGame() != null)
                games.add(matchEvent.getGame());
        }

        //跳过前几轮

        int s = games.size();

        {
            var streamTemp = games.stream()
                    .limit(s - deleteLastRounds)
                    .skip(skipFirstRounds)
                    .filter(gameInfo -> gameInfo.getEndTime() != null);
            if (includingRematch) {
                games = streamTemp.toList();
            } else {
                games = streamTemp.collect(
                        Collectors.toMap(
                                e -> e.getBeatmap().getId(),
                                v -> v,
                                (e, c) -> e.getStartTime().isBefore(c.getStartTime()) ? c : e
                        )
                ).values().stream().toList();
            }
        }

        int scoreNum = 0;
        //每一局单独计算
        for (var game : games) {
            var scoreInfos = game.getScoreInfos();

            GameRound round = new GameRound();
            matchStatistics.getGameRounds().add(round);
            //算总分
            for (int i = 0; i < scoreInfos.size(); i++) {
                var scoreInfo = scoreInfos.get(i);
                //剔除未passed成绩
                if (!includingFail && !scoreInfo.getPassed()) {
                    scoreInfos.remove(i);
                    i--;
                }
                //剔除低于10000分的成绩。
                else if (scoreInfo.getScore() < 10000) {
                    scoreInfos.remove(i);
                    i--;
                } else {
                    String team = scoreInfos.get(i).getMatch().get("team").asText();
                    if (team.equals("none") && matchStatistics.isTeamVs()) {
                        matchStatistics.setTeamVs(false);
                    }
                    //填充用户队伍信息和总分信息
                    var user = users.get(scoreInfo.getUserID());
                    if (user == null) {
                        user = new UserMatchData(osuGetService.getPlayerOsuInfo(scoreInfo.getUserID().longValue()));
                        users.put(scoreInfo.getUserID(), user);
                    }
                    user.setTeam(team);
                    user.getScores().add(scoreInfo.getScore());
                    round.getUserScores().put(user.getId(), scoreInfo.getScore());
                    //队伍总分
                    round.getTeamScores().put(team, round.getTeamScores().getOrDefault(team, 0L) + scoreInfo.getScore());
                }
            }

            //算RRA,算法score/average(score);
            for (var scoreEntry : round.getUserScores().entrySet()) {
                var user = users.get(scoreEntry.getKey());
                user.getRRAs().add((((double) scoreEntry.getValue() / round.getTotalScore()) * scoreInfos.size()));

                // YMRA v3.4 添加 BWS
                if (Objects.equals(round.getWinningTeam(), user.getTeam())) {
                    user.getRWSs().add((((double) scoreEntry.getValue() / round.getWinningTeamScore())));
                }
            }

            scoreNum += scoreInfos.size();
        }
        matchStatistics.setScoreNum(scoreNum);

        //剔除没参赛的用户
        users.values().removeIf(user -> user.getRRAs().isEmpty());

        //计算步骤封装
        matchStatistics.calculate();

        //从大到小排序
        List<UserMatchData> finalUsers = new ArrayList<>(users.values());
//        sortedUsers.sort((o1, o2) -> (int) ((o2.getMRA() - o1.getMRA()) * 10000)); //排序采用stream
        AtomicInteger tp1 = new AtomicInteger(1);
        AtomicInteger tp2 = new AtomicInteger(1);
        AtomicInteger tp3 = new AtomicInteger(1);
        AtomicInteger tpIndex = new AtomicInteger(1);
        final int alluserssize = finalUsers.size();
        finalUsers = finalUsers.stream()
                .sorted(Comparator.comparing(UserMatchData::getERA).reversed())
                .peek(r -> r.setERA_index(1.0 * tp1.getAndIncrement() / alluserssize))
                .sorted(Comparator.comparing(UserMatchData::getDRA).reversed())
                .peek(r -> r.setDRA_index(1.0 * tp2.getAndIncrement() / alluserssize))
                .sorted(Comparator.comparing(UserMatchData::getRWS).reversed())
                .peek(r -> r.setRWS_index(1.0 * tp3.getAndIncrement() / alluserssize))
                .sorted(Comparator.comparing(UserMatchData::getMRA).reversed())
                .peek(r -> r.setIndex(tpIndex.getAndIncrement())).collect(Collectors.toList())
        ;

        var teamPoint = matchStatistics.getTeamPoint();


        return new RatingData(matchStatistics.isTeamVs(), teamPoint.getOrDefault("red", 0), teamPoint.getOrDefault("blue", 0), games.get(0).getTeamType(), finalUsers);
    }
}

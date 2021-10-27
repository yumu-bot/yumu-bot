package com.now.nowbot.service.MessageService;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.match.*;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.JacksonUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;

@Service("rating")
public class RatingService implements MessageService {
    private static final Logger log = LoggerFactory.getLogger(RatingService.class);

    @Autowired
    OsuGetService osuGetService;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        int matchId = Integer.parseInt(matcher.group("matchid"));
        int skipedRounds = matcher.group("skipedrounds") == null ? 0 : Integer.parseInt(matcher.group("skipedrounds"));
        boolean includingFail = matcher.group("includingfail") == null || !matcher.group("includingfail").equals("0");

        var data = osuGetService.getMatchInfo(matchId);
        Match match = JacksonUtil.toObj(data.toString(), Match.class);
        while (!match.getFirstEventId().equals(match.getEvents().get(0).getId())) {
            data = osuGetService.getMatchInfo(matchId, match.getEvents().get(0).getId());
            var events = JacksonUtil.toObj(data.toString(), Match.class).getEvents();
            match.getEvents().addAll(0, events);
        }

        //存储计算信息
        MatchStatistics matchStatistics = new MatchStatistics();

        List<GameInfo> games = new ArrayList<>();
        JsonNode JUsers = match.getUsers();
        Map<Integer, UserMatchData> users = new HashMap<>();
        matchStatistics.setUsers(users);

        //获取所有user
        for (var jUser : JUsers) {
            Integer id = Integer.parseInt(jUser.get("id").toString());
            String username = jUser.get("username").asText();
            users.put(id, new UserMatchData(id, username));
        }

        //获取所有轮的游戏
        for (var matchEvent : match.getEvents()) {
            if (matchEvent.getGame() != null)
                games.add(matchEvent.getGame());
        }

        //跳过前几轮
        games = games.subList(skipedRounds, games.size());

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

                    //填充用户队伍信息和总分信息
                    var user = users.get(scoreInfo.getUserId());
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
            }

            scoreNum += scoreInfos.size();
        }
        matchStatistics.setScoreNum(scoreNum);

        //剔除没参赛的用户
        Iterator<Map.Entry<Integer, UserMatchData>> it = users.entrySet().iterator();
        while (it.hasNext()) {
            var user = it.next().getValue();
            if (user.getRRAs().size() == 0)
                it.remove();
        }

        //计算步骤封装
        matchStatistics.calculate();

        //从大到小排序
        List<UserMatchData> sortedUsers = new ArrayList<>(users.values());
        sortedUsers.sort((o1, o2) -> (int) ((o2.getMRA() - o1.getMRA()) * 10000));

        var teamPoint = matchStatistics.getTeamPoint();

        //结果数据
        StringBuilder sb = new StringBuilder();
        sb.append(match.getMatchInfo().getName()).append("\n")
                .append(teamPoint.getOrDefault("red", 0)).append(" : ")
                .append(teamPoint.getOrDefault("blue", 0)).append("\n")
                .append("mp").append(matchId).append(" ").append(games.get(0).getTeamType()).append("\n");

        for (int i = 0; i < sortedUsers.size(); i++) {
            var user = sortedUsers.get(i);
            sb.append(String.format("#%d [%.2f] %s %s", i + 1, user.getMRA(), user.getUsername(), user.getTeam().toUpperCase()))
                    .append("\n")
                    .append(String.format("DMG %.2f (%.2fM)", user.getMRA(), user.getTotalScore()))
                    .append("\n")
                    .append(String.format("%dW-%dL (%d%%)", user.getWins(), user.getLost(),
                            Math.round((double) user.getWins() * 100 / (user.getWins() + user.getLost()))))
                    .append("\n\n");
        }

//        //输出完整用户数据
//        for(var user:sortedUsers){
//            sb.append("\n").append(user.getUsername()).append(" ").append(user.getTeam()).append("\n")
//                    .append("TMG: ").append(user.getTMG()).append("\n")
//                    .append("AMG: ").append(user.getAMG()).append("\n")
//                    .append("MRA: ").append(user.getMRA()).append("\n")
//                    .append("MDRA: ").append(user.getMDRA()).append("\n");
//            //输出完整的分数信息
////                    .append("Scores And RRAs: ").append("\n");
////            for(int i=0;i<user.getScores().size();i++){
////                sb.append(user.getScores().get(i)).append(" ")
////                        .append(user.getRRAs().get(i)).append("\n");
////            }
//        }
        event.getSubject().sendMessage(sb.toString());
    }


}


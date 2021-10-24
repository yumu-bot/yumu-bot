package com.now.nowbot.service.MessageService;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.match.GameInfo;
import com.now.nowbot.model.match.Match;
import com.now.nowbot.model.match.UserMatchData;
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
        int skipedRounds = matcher.group("skipedrounds")==null?0:Integer.parseInt(matcher.group("skipedrounds"));
        boolean includingFail = matcher.group("includingfail") == null || !matcher.group("includingfail").equals("0");

        var data = osuGetService.getMatchInfo(matchId);
        Match match = JacksonUtil.toObj(data.toString(), Match.class);
        while (!match.getFirstEventId().equals(match.getEvents().get(0).getId())) {
            data = osuGetService.getMatchInfo(matchId, match.getEvents().get(0).getId());
            var events = JacksonUtil.toObj(data.toString(), Match.class).getEvents();
            match.getEvents().addAll(0, events);
        }

        List<GameInfo> games = new ArrayList<>();
        JsonNode JUsers = match.getUsers();
        Map<Integer, UserMatchData> users = new HashMap<>();

        for (var jUser : JUsers) {
            Integer id = Integer.parseInt(jUser.get("id").toString());
            String username = jUser.get("username").asText();
            users.put(id, new UserMatchData(id, username));
        }

        for (var matchEvent : match.getEvents()) {
            if (matchEvent.getGame() != null)
                games.add(matchEvent.getGame());
        }

        //跳过前几轮
        games = games.subList(skipedRounds, games.size());

        //队伍总分
        Map<String, List<Long>> teamScores = new HashMap<>();

        int scoreNum = 0;
        //每一局单独计算
        for (var game : games) {
            var scoreInfos = game.getScoreInfos();
            long totalScores = 0L;
            //算总分
            for (int i=0;i<scoreInfos.size();i++) {
                //剔除未passed成绩
                if(includingFail&&!scoreInfos.get(i).getPassed()){
                    scoreInfos.remove(i);
                    i--;
                }
                //剔除低于10000分的成绩。
                else if(scoreInfos.get(i).getScore()<10000){
                    scoreInfos.remove(i);
                    i--;
                }
                else {
                    totalScores += scoreInfos.get(i).getScore();
                }
            }

            //算RRA,算法score/average(score);
            for (var scoreInfo : scoreInfos) {
                var user = users.get(scoreInfo.getUserId());
                user.setTeam(scoreInfo.getMatch().get("team").asText());

                //算队伍总分
                teamScores.putIfAbsent(user.getTeam(), new ArrayList<>());
                teamScores.get(user.getTeam()).add(scoreInfo.getScore().longValue());

                user.getRRAs().add(((double) scoreInfo.getScore()*scoreInfos.size() / totalScores));
            }
            scoreNum += scoreInfos.size();
        }

        //剔除没参赛的用户
        Iterator<Map.Entry<Integer, UserMatchData>> it = users.entrySet().iterator();
        while(it.hasNext()){
            var user = it.next().getValue();
            if(user.getRRAs().size()==0)
                it.remove();
        }

        //挨个用户计算AMG,并记录总AMG
        double totalAMG = 0;
        for (var user : users.values()) {
            user.calculateAMG();
            totalAMG += user.getAMG();
        }

        //挨个计算MQ,并记录最小的MQ
        double minMQ = 100;
        for (var user : users.values()) {
            user.calculateMQ(totalAMG / users.size());
            if (user.getMQ() < minMQ)
                minMQ = user.getMQ();
        }

        //挨个计算MRA和MDRA
        for (var user : users.values()) {
            user.calculateMRA(minMQ);
            user.calculateMDRA(users.size(), scoreNum);
        }

        //如果人数<=2或者没换过人, 则MRA==MQ
        if(users.size()<=2||users.size()* games.size()==scoreNum){
            for(var user:users.values()){
                user.setMRA(user.getMQ());
            }
        }

        //从大到小排序
        List<UserMatchData> sortedUsers = new ArrayList<>(users.values());
        sortedUsers.sort((o1, o2) -> (int) ((o2.getMRA() - o1.getMRA())*10000));

        StringBuilder sb = new StringBuilder();
        sb.append(match.getMatchInfo().getName()).append(" ");
        for(var user:sortedUsers){
            sb.append("\n").append(user.getUsername()).append(" ").append(user.getTeam()).append("\n")
                    .append("TMG: ").append(user.getTMG()).append("\n")
                    .append("AMG: ").append(user.getAMG()).append("\n")
                    .append("MRA: ").append(user.getMRA()).append("\n")
                    .append("MDRA: ").append(user.getMDRA()).append("\n");
        }
        event.getSubject().sendMessage(sb.toString());
    }


}


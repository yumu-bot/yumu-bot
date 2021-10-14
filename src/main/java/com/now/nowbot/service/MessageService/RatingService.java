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
            String username = jUser.get("username").toString();
            users.put(id, new UserMatchData(id, username));
        }
        for (var matchEvent : match.getEvents()) {
            if (matchEvent.getGame() != null)
                games.add(matchEvent.getGame());
        }

        int scoreNum = 0;
        for (var game : games) {
            var scoreInfos = game.getScoreInfos();
            long totalScores = 0L;
            for (var scoreInfo : scoreInfos) {
                totalScores += scoreInfo.getScore();
            }
            for (var scoreInfo : scoreInfos) {
                var user = users.get(scoreInfo.getUserId());
                user.getRRAs().add(((double) scoreInfo.getScore() / totalScores));
            }
            scoreNum += scoreInfos.size();
        }

        double totalAMG = 0;
        for (var user : users.values()) {
            user.calculateAMG();
            totalAMG += user.getAMG();
        }

        double minMQ = 100;
        for (var user : users.values()) {
            user.calculateMQ(totalAMG / users.size());
            if (user.getMQ() < minMQ)
                minMQ = user.getMQ();
        }

        for (var user : users.values()) {
            user.calculateMRA(minMQ);
            user.calculateMDRA(users.size(), scoreNum);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(match.getMatchInfo().getName()).append(" ");
        sb.append("\n").append("username").append(" ")
                .append("TMG").append(" ")
                .append("AMG").append(" ")
                .append("MRA").append(" ")
                .append("MDRA");
        for(var user:users.values()){
            sb.append("\n").append(user.getUsername())
                    .append(user.getTMG()).append(" ")
                    .append(user.getAMG()).append(" ")
                    .append(user.getMRA()).append(" ")
                    .append(user.getMDRA()).append(" ");
        }
        event.getSubject().sendMessage(sb.toString());
    }


}


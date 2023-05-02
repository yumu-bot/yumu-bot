package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.match.*;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.Panel.MatchRatingPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Service("MRA")
public class MRAService implements MessageService {
    private static final Logger log = LoggerFactory.getLogger(MRAService.class);
    @Autowired
    RestTemplate template;

    @Autowired
    OsuGetService osuGetService;
    @Autowired
    BindDao bindDao;

    public static record RatingData(boolean isTeamVs, int red, int blue, String type, List<UserMatchData> allUsers) {
    }


    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        int matchId = Integer.parseInt(matcher.group("matchid"));
        int skipedRounds = matcher.group("skipedrounds") == null ? 0 : Integer.parseInt(matcher.group("skipedrounds"));
        int deletEndRounds = matcher.group("deletendrounds") == null ? 0 : Integer.parseInt(matcher.group("deletendrounds"));
        boolean includingFail = matcher.group("includingfail") == null || !matcher.group("includingfail").equals("0");
        var from = event.getSubject();

        Match match = osuGetService.getMatchInfo(matchId);
        while (!match.getFirstEventId().equals(match.getEvents().get(0).getId())) {
            var events = osuGetService.getMatchInfo(matchId, match.getEvents().get(0).getId()).getEvents();
            match.getEvents().addAll(0, events);
        }

        var data = calculate(match, skipedRounds, deletEndRounds, includingFail, osuGetService);
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


//        //色彩debug
//        var s = Surface.makeRasterN32Premul(50,50*finalUsers.size());
//        var c = s.getCanvas();
//        finalUsers.forEach(
//                userMatchData -> {
//                    c.drawRect(Rect.makeWH(50,50), new Paint().setColor(userMatchData.getRating().color));
//                    c.translate(0,50);
//                }
//        );
//        try {
//            Files.write(Path.of("D:/ra.png"), s.makeImageSnapshot().encodeToData().getBytes());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        //输出完整用户数据
//        for(var user:sortedUsers){
//            sb.append("\n").append(user.getUsername()).append(" ").append(user.getTeam()).append("\n")
//                    .append("TMG: ").append(user.getTMG()).append("\n")
//                    .append("AMG: ").append(user.getAMG()).append("\n")
//                    .append("ERA: ").append(user.getERA()).append("\n")
//                    .append("DRA: ").append(user.getDRA()).append("\n");
//            //输出完整的分数信息
////                    .append("Scores And RRAs: ").append("\n");
////            for(int i=0;i<user.getScores().size();i++){
////                sb.append(user.getScores().get(i)).append(" ")
////                        .append(user.getRRAs().get(i)).append("\n");
////            }
//        }
//        event.getSubject().sendMessage(sb.toString());

        //第二版图像渲染

        if (data.isTeamVs()) {
            var bl = finalUsers.stream().filter(userMatchData -> userMatchData.getTeam().equalsIgnoreCase("blue")).collect(Collectors.toList());
            var rl = finalUsers.stream().filter(userMatchData -> userMatchData.getTeam().equalsIgnoreCase("red")).collect(Collectors.toList());
            var img = new MatchRatingPanelBuilder(bl.size(), rl.size()).drawBanner(PanelUtil.getBanner(null)).drawUser(rl, bl).build();
            QQMsgUtil.sendImage(event.getSubject(), img);
        } else {
            var img = new MatchRatingPanelBuilder(finalUsers.size()).drawBanner(PanelUtil.getBanner(null)).drawUser(finalUsers).build();
            QQMsgUtil.sendImage(event.getSubject(), img);
        }
    }

/*
        //第三版图像渲染

        try {
            var img = postImage(finalUsers, match.getMatchInfo());
            QQMsgUtil.sendImage(from, img);
        } catch (Exception e) {
            log.error("MRA 数据请求失败", e);
            from.sendMessage("MRA 渲染图片超时，请重试。");
        }
    }

    public byte[] postImage(List<UserMatchData> userMatchDataList, MatchInfo matchInfo) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        var body = Map.of(
                "userMatchDataList", userMatchDataList,
                "matchInfo", matchInfo
        );

        HttpEntity httpEntity = new HttpEntity(body, headers);
        ResponseEntity<byte[]> s = template.exchange(URI.create("http://127.0.0.1:1611/panel_C"), HttpMethod.POST, httpEntity, byte[].class);
        return s.getBody();
    }
*/
    //主计算方法
    public static RatingData calculate(Match match, int skipFirstRounds, int deleteLastRounds, boolean includingFail, OsuGetService osuGetService) {
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
        games = games.stream().limit(s - deleteLastRounds).skip(skipFirstRounds).filter(gameInfo -> gameInfo.getEndTime() != null).toList();

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
                    var user = users.get(scoreInfo.getUserId());
                    if (user == null) {
                        user = new UserMatchData(osuGetService.getPlayerOsuInfo(scoreInfo.getUserId().longValue()));
                        users.put(scoreInfo.getUserId(), user);
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
//        Iterator<Map.Entry<Integer, UserMatchData>> it = users.entrySet().iterator();
//        while (it.hasNext()) {
//            var user = it.next().getValue();
//            if (user.getRRAs().size() == 0)
//                it.remove();
//        } //22-04-15 尝试缩短代码
        users.values().removeIf(user -> user.getRRAs().size() == 0);

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
                .peek(r -> r.setIndx(tpIndex.getAndIncrement())).collect(Collectors.toList())
;

        var teamPoint = matchStatistics.getTeamPoint();


        return new RatingData(matchStatistics.isTeamVs(), teamPoint.getOrDefault("red", 0), teamPoint.getOrDefault("blue", 0), games.get(0).getTeamType(), finalUsers);
    }
}


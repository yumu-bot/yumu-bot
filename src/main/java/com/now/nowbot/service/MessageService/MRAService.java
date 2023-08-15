package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.Cover;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.match.*;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.JacksonUtil;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    ImageService imageService;

    public static record RatingData(boolean isTeamVs, int red, int blue, String type, List<UserMatchData> allUsers) {
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        int matchId = Integer.parseInt(matcher.group("matchid"));
        int skipedRounds = matcher.group("skipedrounds") == null ? 0 : Integer.parseInt(matcher.group("skipedrounds"));
        int deletEndRounds = matcher.group("deletendrounds") == null ? 0 : Integer.parseInt(matcher.group("deletendrounds"));
        boolean includingRematch = matcher.group("excludingrematch") == null || !matcher.group("excludingrematch").equalsIgnoreCase("r");
        boolean includingFail = matcher.group("excludingfail") == null || !matcher.group("excludingfail").equalsIgnoreCase("f");
        var from = event.getSubject();
        try {
            var img = getDataImage(matchId, skipedRounds, deletEndRounds, includingFail, includingRematch);
            QQMsgUtil.sendImage(from, img);
        } catch (Exception e) {
            log.error("MRA 数据请求失败", e);
            from.sendMessage("MRA 渲染图片超时，请重试。\n或尝试旧版渲染 !rl <mpid>。");
        }
    }

    public byte[] getDataImage (int matchId, int skipRounds, int deleteEnd, boolean includeFailed, boolean includingRepeat) {
        Match match = osuGetService.getMatchInfo(matchId);

        while (!match.getFirstEventId().equals(match.getEvents().get(0).getId())) {
            var events = osuGetService.getMatchInfo(matchId, match.getEvents().get(0).getId()).getEvents();
            match.getEvents().addAll(0, events);
        }

        var data = calculate(match, skipRounds, deleteEnd, includeFailed, includingRepeat, osuGetService);

        List<UserMatchData> finalUsers = data.allUsers;
        var blueList = finalUsers.stream().filter(userMatchData -> userMatchData.getTeam().equalsIgnoreCase("blue")).toList();
        var redList = finalUsers.stream().filter(userMatchData -> userMatchData.getTeam().equalsIgnoreCase("red")).toList();
        var noneList = finalUsers.stream().filter(userMatchData -> userMatchData.getTeam().equalsIgnoreCase("none")).toList();

        //平均星数和第一个sid
        int sid = 0;
        int rounds = 0;
        double averageStar = 0f;

        //过滤未完成的对局和其他活动
        var events = match.getEvents().stream().filter(
                e -> (e.getGame() != null && e.getGame().getEndTime() != null)
        ).toList();

        for (int i = 0; i < events.size(); i++) {
            var event = match.getEvents().get(i);

            if (event.getGame() != null && event.getGame().getEndTime() != null) {
                if (i > (skipRounds - 1) && i < (events.size() - deleteEnd)) {
                    averageStar += event.getGame().getBeatmap().getDifficultyRating();
                    rounds ++;
                }

                if (sid == 0) {
                    sid = event.getGame().getBeatmap().getBeatmapsetId();
                }
            }
        }

        if (rounds <= 0) {
            averageStar = 0f;
        } else {
            averageStar /= rounds;
        }

        return imageService.getPanelC(redList, blueList, noneList, match.getMatchInfo(), sid, averageStar, rounds, data.red, data.blue, data.isTeamVs);
    }

    /*
    public byte[] postImage(List<UserMatchData> red, List<UserMatchData> blue, List<UserMatchData> none, MatchInfo matchInfo, int sid, int redwins, int bluewins, boolean isTeamVs) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        var body = Map.of(
                "redUsers", red,
                "blueUsers", blue,
                "noneUsers", none,
                "matchInfo", matchInfo,
                "sid", sid,
                "redWins", redwins,
                "blueWins", bluewins,
                "isTeamVs", isTeamVs
        );

        HttpEntity httpEntity = new HttpEntity(body, headers);
        ResponseEntity<byte[]> s = template.exchange(URI.create("http://127.0.0.1:1611/panel_C"), HttpMethod.POST, httpEntity, byte[].class);
        return s.getBody();
    }

     */

    //主计算方法
    public static RatingData calculate(Match match, int skipFirstRounds, int deleteLastRounds, boolean includingFail, boolean includingRematch, OsuGetService osuGetService) {
        //存储计算信息
        MatchStatistics matchStatistics = new MatchStatistics();

        List<GameInfo> games = new ArrayList<>();
        var JUsers = match.getUsers();
        Map<Integer, UserMatchData> users = new HashMap<>();
        matchStatistics.setUsers(users);
        var uid4cover = new HashMap<Long, Cover>();
        int indexOfUser = 0;
        while (true) {
            var l = JUsers.stream().skip(indexOfUser* 50L).limit(50).map(MicroUser::getId).toList();
            indexOfUser++;
            if (l.isEmpty()) break;
            var us = osuGetService.getUsers(l).get("users");
            for(var node: us) {
                uid4cover.put(node.get("id").asLong(0), JacksonUtil.parseObject(node.get("cover"), Cover.class));
            }
        }
        //获取所有user
        for (var jUser : JUsers) {
            var u = new OsuUser();
            u.setId(jUser.getId());
            u.setUsername(jUser.getUserName());
            u.setCover(uid4cover.get(jUser.getId()));
            u.setAvatarUrl(jUser.getAvatarUrl());
            try {
                users.put(jUser.getId().intValue(), new UserMatchData(u));
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
        /*
        Iterator<Map.Entry<Integer, UserMatchData>> it = users.entrySet().iterator();
        while (it.hasNext()) {
            var user = it.next().getValue();
           if (user.getRRAs().size() == 0)
                it.remove();
       }
         */
        //22-04-15 尝试缩短代码
        users.values().removeIf(user -> user.getRRAs().size() == 0);

        //计算步骤封装
        matchStatistics.calculate();

        //从大到小排序
        List<UserMatchData> finalUsers = new ArrayList<>(users.values());
        //sortedUsers.sort((o1, o2) -> (int) ((o2.getMRA() - o1.getMRA()) * 10000)); //排序采用stream
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
                .peek(r -> r.setIndex(tpIndex.getAndIncrement())).collect(Collectors.toList());

        var teamPoint = matchStatistics.getTeamPoint();

        return new RatingData(matchStatistics.isTeamVs(), teamPoint.getOrDefault("red", 0), teamPoint.getOrDefault("blue", 0), games.get(0).getTeamType(), finalUsers);
    }
}


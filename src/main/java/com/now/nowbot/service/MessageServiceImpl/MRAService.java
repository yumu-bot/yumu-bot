package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.JsonData.Cover;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.match.*;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.MRAException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service("MRA")
public class MRAService implements MessageService<Matcher> {

    @Autowired
    OsuGetService osuGetService;
    @Autowired
    ImageService imageService;

    public record RatingData(boolean isTeamVs, int red, int blue, String type, List<UserMatchData> allUsers) {
    }
    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)((ym)?rating|(ym)?ra(?![a-zA-Z_])|mra(?![a-zA-Z_]))+\\s*(?<matchid>\\d+)?(\\s*(?<skip>-?\\d+))?(\\s*(?<skipend>-?\\d+))?(\\s*(?<excludingrematch>[Rr]))?(\\s*(?<excludingfail>[Ff]))?");

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
        int matchID;
        var matchIDStr = matcher.group("matchid");

        if (matchIDStr == null || matchIDStr.isBlank()) {
            throw new MRAException(MRAException.Type.RATING_Parameter_None);
        }

        try {
            matchID = Integer.parseInt(matchIDStr);
        } catch (NumberFormatException e) {
            throw new MRAException(MRAException.Type.RATING_Parameter_Error);
        }

        int skip = matcher.group("skip") == null ? 0 : Integer.parseInt(matcher.group("skip"));
        int skipEnd = matcher.group("skipend") == null ? 0 : Integer.parseInt(matcher.group("skipend"));
        boolean includingRematch = matcher.group("excludingrematch") == null || !matcher.group("excludingrematch").equalsIgnoreCase("r");
        boolean includingFail = matcher.group("excludingfail") == null || !matcher.group("excludingfail").equalsIgnoreCase("f");

        var from = event.getSubject();
        var img = getDataImage(matchID, skip, skipEnd, includingFail, includingRematch);
        try {
            QQMsgUtil.sendImage(from, img);
        } catch (Exception e) {
            NowbotApplication.log.error("MRA 数据请求失败", e);
            throw new MRAException(MRAException.Type.RATING_MRA_Error);
        }
    }

    public byte[] getDataImage (int matchID, int skip, int skipEnd, boolean includeFailed, boolean includingRepeat) throws MRAException {

        if (skip < 0) throw new MRAException(MRAException.Type.RATING_Parameter_SkipError);
        if (skipEnd < 0) throw new MRAException(MRAException.Type.RATING_Parameter_SkipEndError);

        Match match;
        try {
            match = osuGetService.getMatchInfo(matchID);
        } catch (Exception e) {
            throw new MRAException(MRAException.Type.RATING_Match_NotFound);
        }

        while (!match.getFirstEventId().equals(match.getEvents().get(0).getID())) {
            var events = osuGetService.getMatchInfo(matchID, match.getEvents().get(0).getID()).getEvents();
            if (events.isEmpty()) throw new MRAException(MRAException.Type.RATING_Round_Empty);
            match.getEvents().addAll(0, events);
        }

        //获取所有轮的游戏
        List<GameInfo> games = match.getEvents().stream().map(MatchEvent::getGame).filter(Objects::nonNull).toList();

        //跳过前几轮
        int s = games.size();

        {
            games = games.stream()
                    .limit(s - skipEnd)
                    .skip(skip)
                    .filter(gameInfo -> gameInfo.getEndTime() != null).collect(Collectors.toList());
            if (!includingRepeat) {
                Collections.reverse(games);
                var mapSet = new HashSet<Long>();
                games.removeIf(e -> !mapSet.add(e.getBID()));
            }
        }

        var data = calculate(match.getUsers(), games, includeFailed, osuGetService);

        List<UserMatchData> finalUsers = data.allUsers;
        var blueList = finalUsers.stream().filter(userMatchData -> userMatchData.getTeam().equalsIgnoreCase("blue")).toList();
        var redList = finalUsers.stream().filter(userMatchData -> userMatchData.getTeam().equalsIgnoreCase("red")).toList();
        var noneList = finalUsers.stream().filter(userMatchData -> userMatchData.getTeam().equalsIgnoreCase("none")).toList();

        //平均星数和获取第一个sid
        int sid = 0;
        float averageStar = 0f;
        int rounds = games.size();
        int noMapRounds = 0;
        for (var g : games) {
            if (sid == 0 && g.getBeatmap() != null) {
                sid = g.getBeatmap().getSID();
            }

            if (g.getBeatmap() != null) {
                averageStar += g.getBeatmap().getStarRating();
            } else {
                noMapRounds ++;
            }
        }

        averageStar /= (rounds - noMapRounds);

        byte[] img;
        try {
            img = imageService.getPanelC(redList, blueList, noneList, match.getMatchInfo(), sid, averageStar, rounds, data.red, data.blue, data.isTeamVs);
        } catch (Exception e) {
            throw new MRAException(MRAException.Type.RATING_MRA_Error);
        }

        return img;
    }

    //主计算方法
    public static RatingData calculate(List<MicroUser> userAll, List<GameInfo> games, boolean includingFail, OsuGetService osuGetService) {
        //存储计算信息
        MatchStatistics matchStatistics = new MatchStatistics();


        Map<Integer, UserMatchData> users = new HashMap<>();
        matchStatistics.setUsers(users);
        var uid4cover = new HashMap<Long, Cover>();
        int indexOfUser = 0;
        while (true) {
            var l = userAll.stream().skip(indexOfUser* 50L).limit(50).map(MicroUser::getUID).toList();
            indexOfUser++;
            if (l.isEmpty()) break;
            var us = osuGetService.getUsers(l);
            for(var node: us) {
                uid4cover.put(node.getUID(), node.getCover());
            }
        }
        //获取所有user
        for (var jUser : userAll) {
            var u = new OsuUser();
            u.setUID(jUser.getUID());
            u.setUsername(jUser.getUserName());
            u.setCover(uid4cover.get(jUser.getUID()));
            u.setAvatarUrl(jUser.getAvatarUrl());
            try {
                users.put(jUser.getUID().intValue(), new UserMatchData(u));
            } catch (Exception e) {
                users.put(jUser.getUID().intValue(), new UserMatchData(jUser.getUID().intValue(), "UID:" + jUser.getUID().intValue()));
            }
        }



        int scoreNum = 0;
        //每一局单独计算
        for (var game : games) {
            var scoreInfos = game.getScoreInfoList();

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
                    round.getUserScores().put(user.getUID(), scoreInfo.getScore());
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


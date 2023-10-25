package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.*;
import com.now.nowbot.model.match.*;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.MatchRoundException;
import com.now.nowbot.util.QQMsgUtil;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service("MR")
public class MatchRoundService implements MessageService<Matcher> {
    Logger log = LoggerFactory.getLogger(MatchRoundService.class);

    @Autowired
    OsuGetService osuGetService;
    @Autowired
    ImageService imageService;

    public record RatingData(boolean isTeamVs, int red, int blue, String type, List<UserMatchData> allUsers) {
    }
    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)((ym)?matchround(s)?|(ym)?round(s)?(?![a-zA-Z_])|mr(?![a-zA-Z_])|ro(?![a-zA-Z_]))+\\s*(?<matchid>\\d+)?\\s*(?<round>\\d+)?(\\s*(?<keyword>[\\w\\s\\d-_ %*()/|]+))?");

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
            throw new MatchRoundException(MatchRoundException.Type.MR_Parameter_None);
        }

        try {
            matchID = Integer.parseInt(matchIDStr);
        } catch (NumberFormatException e) {
            throw new MatchRoundException(MatchRoundException.Type.MR_MatchID_RangeError);
        }

        var keyword = matcher.group("keyword");
        boolean hasNoKeyword = (keyword == null || keyword.isEmpty() || keyword.isBlank());

        int round;
        var roundStr = matcher.group("round");

        if (roundStr == null || roundStr.isBlank()) {
            if (hasNoKeyword) throw new MatchRoundException(MatchRoundException.Type.MR_Parameter_None);
            else roundStr = "-1";
        }

        try {
            round = Integer.parseInt(roundStr);
        } catch (NumberFormatException e) {
            if (hasNoKeyword) throw new MatchRoundException(MatchRoundException.Type.MR_MatchID_RangeError);
            else round = -1;
        }

        var from = event.getSubject();
        var img = getDataImage(matchID, round, keyword);
        try {
            QQMsgUtil.sendImage(from, img);
        } catch (Exception e) {
            log.error("MR 数据请求失败", e);
            throw new MatchRoundException(MatchRoundException.Type.MR_Send_Error);
        }
    }

    public byte[] getDataImage (int matchID, int index, @Nullable String keyword) throws MatchRoundException {
        boolean hasNoKeyword = (keyword == null || keyword.isEmpty() || keyword.isBlank());

        Match match;
        try {
            match = osuGetService.getMatchInfo(matchID);
        } catch (Exception e) {
            throw new MatchRoundException(MatchRoundException.Type.MR_MatchID_NotFound);
        }

        while (!match.getFirstEventId().equals(match.getEvents().get(0).getID())) {
            var events = osuGetService.getMatchInfo(matchID, match.getEvents().get(0).getID()).getEvents();
            if (events.isEmpty()) throw new MatchRoundException(MatchRoundException.Type.MR_Round_Empty);
            match.getEvents().addAll(0, events);
        }

        //获取所有轮的游戏
        List<GameInfo> games = match.getEvents().stream()
                .map(MatchEvent::getGame)
                .filter(Objects::nonNull)
                .filter(gameInfo -> gameInfo.getEndTime() != null)
                .collect(Collectors.toList());

        if (index < 0 || index > match.getEvents().size()) {
            if (hasNoKeyword){
                try {
                    index = getRoundIndexFromKeyWord(games, String.valueOf(index));
                } catch (NumberFormatException e) {
                    throw new MatchRoundException(MatchRoundException.Type.MR_Round_NotFound);
                }
            } else {
                index = getRoundIndexFromKeyWord(games, keyword);
            }
        }

        if (index == -1){
            throw new MatchRoundException(MatchRoundException.Type.MR_KeyWord_NotFound);
        }

        var matchBeatmap = games.get(index).getBeatmap();

        var data = fetch(match.getUsers(), games, index, osuGetService);

        List<UserMatchData> finalUsers = data.allUsers;
        var blueList = finalUsers.stream().filter(
                userMatchData -> userMatchData.getTeam().equalsIgnoreCase("blue")).toList();
        var redList = finalUsers.stream().filter(
                userMatchData -> userMatchData.getTeam().equalsIgnoreCase("red")).toList();
        var noneList = finalUsers.stream().filter(
                userMatchData -> userMatchData.getTeam().equalsIgnoreCase("none")).toList();

        //平均星数
        float averageStar = 0f;
        int rounds = games.size();
        int noMapRounds = 0;
        for (var g : games) {
            if (g.getBeatmap() != null) {
                averageStar += g.getBeatmap().getDifficultyRating();
            } else {
                noMapRounds ++;
            }
        }

        averageStar /= (rounds - noMapRounds);

        byte[] img;
        try {
            img = imageService.getPanelF2(redList, blueList, noneList, match.getMatchInfo(), matchBeatmap, averageStar, rounds, data.red, data.blue, data.isTeamVs);
        } catch (Exception e) {
            log.error("MR 图片渲染失败：", e);
            throw new MatchRoundException(MatchRoundException.Type.MR_Fetch_Error);
        }

        return img;
    }

    //主获取方法
    public static RatingData fetch(List<MicroUser> userAll, List<GameInfo> games, int index, OsuGetService osuGetService) {
        //存储计算信息
        MatchStatistics matchStatistics = new MatchStatistics();

        //生成玩家哈希表
        Map<Integer, UserMatchData> userDataMap = new HashMap<>();
        matchStatistics.setUsers(userDataMap);
        var UID2Cover = new HashMap<Long, Cover>();
        int indexOfUser = 0;
        while (true) {
            var UIDList = userAll.stream().skip(indexOfUser * 50L).limit(50).map(MicroUser::getId).toList();
            indexOfUser++;
            if (UIDList.isEmpty()) break;
            var microUserList = osuGetService.getUsers(UIDList);
            for (var m : microUserList) {
                UID2Cover.put(m.getId(), m.getCover());
            }
        }

        //获取所有玩家信息
        for (var microUser : userAll) {

            var osuUser = new OsuUser();
            osuUser.setUID(microUser.getId());
            osuUser.setUsername(microUser.getUserName());
            osuUser.setCover(UID2Cover.get(microUser.getId()));
            osuUser.setAvatarUrl(microUser.getAvatarUrl());

            try {
                userDataMap.put(microUser.getId().intValue(), new UserMatchData(osuUser));
            } catch (Exception e) {
                userDataMap.put(microUser.getId().intValue(), new UserMatchData(microUser.getId().intValue(),
                        "UID:" + microUser.getId().intValue()));
            }
        }


        // 获取某一对局的信息
        {
            var roundInfo = games.get(index);
            var scoreInfoList = roundInfo.getScoreInfoList();

            GameRound round = new GameRound();
            matchStatistics.getGameRounds().add(round);

            //算对局内的总分
            for (int i = 0; i < scoreInfoList.size(); i++) {
                var scoreInfo = scoreInfoList.get(i);

                //剔除低于10000分的成绩
                if (scoreInfo.getScore() < 10000) {
                    scoreInfoList.remove(i);
                    i--;
                } else {
                    String team = scoreInfo.getMatch().get("team").asText();
                    if (team.equals("none") && matchStatistics.isTeamVs()) {
                        matchStatistics.setTeamVs(false);
                    }

                    //填充用户队伍信息和总分信息
                    var userRoundData = userDataMap.get(scoreInfo.getUserID());
                    if (userRoundData == null) {
                        userRoundData = new UserMatchData(osuGetService.getPlayerOsuInfo(scoreInfo.getUserID().longValue()));
                        userDataMap.put(scoreInfo.getUserID(), userRoundData);
                    }
                    userRoundData.setTeam(team);
                    userRoundData.getScores().add(scoreInfo.getScore());
                    round.getUserScores().put(userRoundData.getUID(), scoreInfo.getScore());

                    //队伍总分
                    round.getTeamScores().put(team, round.getTeamScores().getOrDefault(team, 0L) + scoreInfo.getScore());
                }
            }

            //算RRA,算法score/average(score);
            for (var scoreEntry : round.getUserScores().entrySet()) {
                var user = userDataMap.get(scoreEntry.getKey());
                user.getRRAs().add((((double) scoreEntry.getValue() / round.getTotalScore()) * scoreInfoList.size()));
            }

            matchStatistics.setScoreNum(games.isEmpty() ? 0 : games.size());

            //剔除没参赛的用户
            userDataMap.values().removeIf(user -> user.getRRAs().isEmpty());
        }

        //计算步骤封装
        matchStatistics.calculate();

        //不需要排序
        List<UserMatchData> finalUsers = new ArrayList<>(userDataMap.values());
        var teamPoint = matchStatistics.getTeamPoint();

        return new RatingData(matchStatistics.isTeamVs(), teamPoint.getOrDefault("red", 0), teamPoint.getOrDefault("blue", 0), games.get(0).getTeamType(), finalUsers);
    }

    private static int getRoundIndexFromKeyWord (List<GameInfo> infos, @Nullable String keyword) {
        int size = infos.size();

        for (int i = 0; i < size; i++) {
            BeatMap b;

            try {
                b = infos.get(i).getBeatmap();
            } catch (NullPointerException ignored) {
                continue;
            }

            if (keyword != null &&
                    (b.getBeatMapSet().getTitle().toLowerCase().contains(keyword.trim())
                    || b.getBeatMapSet().getArtist().contains(keyword.trim())
                    || b.getVersion().contains(keyword.trim()))) {
                return i;
            }
        }

        return -1;
    }
}


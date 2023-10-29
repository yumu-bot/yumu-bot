package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.*;
import com.now.nowbot.model.match.*;
import com.now.nowbot.model.score.MPScore;
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

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?(matchround(s)?|round(s)?(?![a-zA-Z_])|mr(?![a-zA-Z_])|ro(?![a-zA-Z_]))+\\s*(?<matchid>\\d+)?\\s*(?<round>\\d+)?(\\s*(?<keyword>[\\w\\s\\d-_ %*()/|]+))?");

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
            List<MatchEvent> events = osuGetService.getMatchInfo(matchID, match.getEvents().get(0).getID()).getEvents();
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

        var scoreList = fetch(match.getUsers(), games, index, osuGetService);
        var blueList = scoreList.stream().filter(
                s -> s.getMatch().get("team").asText().equalsIgnoreCase("blue")).toList();
        var redList = scoreList.stream().filter(
                s -> s.getMatch().get("team").asText().equalsIgnoreCase("red")).toList();
        var noneList = scoreList.stream().filter(
                s -> s.getMatch().get("team").asText().equalsIgnoreCase("none")).toList();


        boolean isTeamVS;
        try {
            isTeamVS = match.getEvents().get(0).getGame().getScoringType().equalsIgnoreCase("team-vs");
        } catch (Exception e) {
            isTeamVS = false;
        }

        //平均星数
        float averageStar = 0f;
        int rounds = games.size();
        int noMapRounds = 0;

        int redWin = 0;
        int blueWin = 0;

        for (var g : games) {
            if (g.getBeatmap() != null) {
                averageStar += g.getBeatmap().getDifficultyRating();
            } else {
                noMapRounds ++;
            }

            //算红蓝总分
            if (isTeamVS) {
                int redTeamScore = 0;
                int blueTeamScore = 0;

                var scores = g.getScoreInfoList();
                for (MPScore s : scores) {
                    switch (s.getMatch().get("team").asText()) {
                        case "red": redTeamScore += s.getScore(); break;
                        case "blue": blueTeamScore += s.getScore(); break;
                    }
                }

                if (redTeamScore > blueTeamScore) redWin++;
                else if (redTeamScore < blueTeamScore) blueWin++;
            }
        }

        averageStar /= (rounds - noMapRounds);

        var beatMap = games.get(index).getBeatmap();

        byte[] img;
        try {
            img = imageService.getPanelF2(redList, blueList, noneList, match.getMatchInfo(), beatMap, averageStar, rounds, redWin, blueWin, isTeamVS);
        } catch (Exception e) {
            log.error("MR 图片渲染失败：", e);
            throw new MatchRoundException(MatchRoundException.Type.MR_Fetch_Error);
        }

        return img;
    }

    //主获取方法
    public static List<MPScore> fetch(List<MicroUser> userAll, List<GameInfo> games, int index, OsuGetService osuGetService) {
        //存储计算信息
        List<MPScore> scoreData = new ArrayList<>();

        //生成玩家哈希表
        Map<Integer, OsuUser> osuUserMap = new HashMap<>();

        //获取玩家背景图
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
            try {
                osuUser.setUID(microUser.getId());
                osuUser.setUsername(microUser.getUserName());
                osuUser.setCover(UID2Cover.get(microUser.getId()));
                osuUser.setAvatarUrl(microUser.getAvatarUrl());
                osuUser.setCountry(microUser.getCountry());
            } catch (Exception e) {
                osuUser.setUID(microUser.getId());
                osuUser.setUsername("UID:" + microUser.getId());
                osuUser.setCover(null);
                osuUser.setAvatarUrl(null);
                osuUser.setCountry(null);
            }

            osuUserMap.put(microUser.getId().intValue(), osuUser);
        }


        // 获取某一对局的信息
        var roundInfo = games.get(index);
        var scoreInfoList = roundInfo.getScoreInfoList();

        //剔除没参赛和小于1w的玩家
        scoreInfoList.removeIf(score -> score.getScore() <= 10000);

        // 给分数加头像
        for (MPScore s: scoreInfoList) {
            s.setOsuUser(osuUserMap.get(s.getUID()));
            scoreData.add(s);
        }

        //按分数排序 也可以不排序
        /*
        scoreData = scoreData.stream()
                .sorted(Comparator.comparing(MPScore::getScore).reversed())
                .collect(Collectors.toList());
         */

        return scoreData;
    }

    private static int getRoundIndexFromKeyWord (List<GameInfo> infoList, @Nullable String keyword) {
        int size = infoList.size();

        for (int i = 0; i < size; i++) {
            BeatMap beatMap;
            String word = "";
            if (keyword != null) {
                word = keyword.trim().toLowerCase();
            }

            try {
                beatMap = infoList.get(i).getBeatmap();
            } catch (NullPointerException ignored) {
                continue;
            }

            try {
                if (beatMap.getBeatMapSet().getTitle().toLowerCase().contains(word) ||
                        beatMap.getBeatMapSet().getArtist().toLowerCase().contains(word) ||
                        beatMap.getBeatMapSet().getMapperName().toLowerCase().contains(word) ||
                        beatMap.getVersion().toLowerCase().contains(word)) {
                    return i;
                }
            } catch (Exception ignored) {}
        }

        return -1;
    }
}


package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.multiplayer.Match;
import com.now.nowbot.model.multiplayer.MatchCal;
import com.now.nowbot.model.multiplayer.MatchEvent;
import com.now.nowbot.model.multiplayer.MatchRound;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import com.now.nowbot.throwable.ServiceException.MatchRoundException;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

@Service("MATCH_ROUND")
public class MatchRoundService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(MatchRoundService.class);

    @Resource
    OsuMatchApiService osuMatchApiService;
    @Resource
    ImageService imageService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instructions.MATCH_ROUND.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        int matchID;
        var matchIDStr = matcher.group("matchid");

        if (matchIDStr == null || matchIDStr.isBlank()) {

            try {
                var md = DataUtil.getMarkdownFile("Help/round.md");
                var image = imageService.getPanelA6(md, "help");
                from.sendImage(image);
                return;
            } catch (Exception e) {
                throw new MatchRoundException(MatchRoundException.Type.MR_Instructions);
            }
        }

        try {
            matchID = Integer.parseInt(matchIDStr);
        } catch (NumberFormatException e) {
            throw new MatchRoundException(MatchRoundException.Type.MR_MatchID_RangeError);
        }

        var keyword = matcher.group("keyword");
        boolean hasKeyword = (keyword != null && !keyword.isEmpty() && !keyword.isBlank());

        int round;
        var roundStr = matcher.group("round");
        boolean hasRound = (roundStr != null && !roundStr.isEmpty() && !roundStr.isBlank());

        if (hasRound) {
            if (hasKeyword) {
            //这里是把诸如 21st 类的东西全部匹配到 keyword 里
            keyword = roundStr + keyword;
            roundStr = "-1";
            }
        } else {
            if (hasKeyword) {
                roundStr = "-1";
            } else {
                try {
                    var md = DataUtil.getMarkdownFile("Help/round.md");
                    var image = imageService.getPanelA6(md, "help");
                    from.sendImage(image);
                    return;
                } catch (Exception e) {
                    throw new MatchRoundException(MatchRoundException.Type.MR_Instructions);
                }
            }
        }

        try {
            round = Integer.parseInt(roundStr) - 1;
        } catch (NumberFormatException e) {
            if (hasKeyword) {
                round = -1;
            } else {
                throw new MatchRoundException(MatchRoundException.Type.MR_Round_RangeError);
            }
        }

        var image = getDataImage(matchID, round, keyword);

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("对局信息数据请求失败", e);
            throw new MatchRoundException(MatchRoundException.Type.MR_Send_Error);
        }
    }

    public byte[] getDataImage(int matchID, int index, @Nullable String keyword) throws MatchRoundException {
        boolean hasKeyword = (keyword != null && !keyword.isEmpty() && !keyword.isBlank());

        Match match;
        try {
            match = osuMatchApiService.getMatchInfo(matchID, 10);
        } catch (WebClientResponseException e) {
            throw new MatchRoundException(MatchRoundException.Type.MR_MatchID_NotFound);
        }

        while (!match.getFirstEventId().equals(match.getEvents().getFirst().getId())) {
            List<MatchEvent> events = osuMatchApiService.getMatchInfo(matchID, 10).getEvents();
            if (events.isEmpty()) throw new MatchRoundException(MatchRoundException.Type.MR_Round_Empty);
            match.getEvents().addAll(0, events);
        }

        //获取所有轮的游戏
        var cal = new MatchCal(match, 0, 0, null, 1d, true, true);

        List<MatchRound> rounds = cal.getRoundList();

        if (index < 0 || index > match.getEvents().size()) {
            if (hasKeyword) {
                index = getRoundIndexFromKeyWord(rounds, keyword);
            } else {
                try {
                    index = getRoundIndexFromKeyWord(rounds, String.valueOf(index));
                } catch (NumberFormatException e) {
                    throw new MatchRoundException(MatchRoundException.Type.MR_Round_NotFound);
                }
            }
        }

        if (index == -1 && hasKeyword) {
            throw new MatchRoundException(MatchRoundException.Type.MR_KeyWord_NotFound);
        }

        byte[] img;
        try {
            img = imageService.getPanelF2(match.getMatchStat(), cal.getRoundList().get(index), index);
        } catch (Exception e) {
            log.error("对局信息图片渲染失败：", e);
            throw new MatchRoundException(MatchRoundException.Type.MR_Fetch_Error);
        }

        return img;
    }

    private static int getRoundIndexFromKeyWord (List<MatchRound> infoList, @Nullable String keyword) {
        int size = infoList.size();
        String word;

        if (keyword != null && !keyword.isEmpty() && !keyword.isBlank()) {
            word = keyword.trim().toLowerCase();
        } else {
            return -1;
        }

        for (int i = 0; i < size; i++) {
            BeatMap beatMap;

            try {
                beatMap = infoList.get(i).getBeatMap();
                if (Objects.isNull(beatMap)) continue;
            } catch (NullPointerException ignored) {
                continue;
            }

            try {
                if (Objects.nonNull(beatMap.getBeatMapSet()) && (
                        beatMap.getBeatMapSet().getTitle().toLowerCase().contains(word) ||
                        beatMap.getBeatMapSet().getArtist().toLowerCase().contains(word) ||
                        beatMap.getBeatMapSet().getTitleUnicode().toLowerCase().contains(word) ||
                        beatMap.getBeatMapSet().getArtistUnicode().toLowerCase().contains(word) ||
                        beatMap.getBeatMapSet().getCreator().toLowerCase().contains(word) ||
                        beatMap.getDifficultyName().toLowerCase().contains(word)
                )) {
                    return i;
                }
            } catch (Exception ignored) {
                //continue;
            }
        }

        return -1;
    }
}


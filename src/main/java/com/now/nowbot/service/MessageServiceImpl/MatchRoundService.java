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
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.regex.Matcher;

@Service("MR")
public class MatchRoundService implements MessageService<Matcher> {
    Logger log = LoggerFactory.getLogger(MatchRoundService.class);

    @Autowired
    OsuMatchApiService osuMatchApiService;
    @Autowired
    ImageService imageService;

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = Instructions.ROUND.matcher(event.getRawMessage().trim());
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
                throw new MatchRoundException(MatchRoundException.Type.MR_Parameter_None);
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
        var cal = new MatchCal(match, 0, 0, true, true);

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
            log.error("MR 图片渲染失败：", e);
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
                beatMap = infoList.get(i).getBeatmap();
            } catch (NullPointerException ignored) {
                continue;
            }

            try {
                if (
                        beatMap.getBeatMapSet().getTitle().toLowerCase().contains(word) ||
                        beatMap.getBeatMapSet().getArtist().toLowerCase().contains(word) ||
                        beatMap.getBeatMapSet().getTitleUTF().toLowerCase().contains(word) ||
                        beatMap.getBeatMapSet().getArtistUTF().toLowerCase().contains(word) ||
                        beatMap.getBeatMapSet().getMapperName().toLowerCase().contains(word) ||
                        beatMap.getVersion().toLowerCase().contains(word)
                ) {
                    return i;
                }
            } catch (Exception ignored) {
                //continue;
            }
        }

        return -1;
    }
}


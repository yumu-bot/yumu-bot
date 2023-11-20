package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.multiplayer.Match;
import com.now.nowbot.model.multiplayer.MatchData;
import com.now.nowbot.model.multiplayer.PlayerData;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import com.now.nowbot.throwable.ServiceException.MRAException;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("MRA2")
public class MuRatingService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(MuRatingService.class);

    @Autowired
    OsuMatchApiService osuMatchApiService;
    @Autowired
    ImageService imageService;

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)((?<uu>(u{1,2})(rating|ra(?![a-zA-Z_])))|(?<main>((ym)?rating|(ym)?ra(?![a-zA-Z_])|mra(?![a-zA-Z_]))))\\s*(?<matchid>\\d+)?(\\s*(?<skip>-?\\d+))?(\\s*(?<skipend>-?\\d+))?(\\s*(?<rematch>[Rr]))?(\\s*(?<keep>[Ff]))?");

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
            throw new MRAException(MRAException.Type.RATING_Parameter_MatchNone);
        }

        try {
            matchID = Integer.parseInt(matchIDStr);
        } catch (NumberFormatException e) {
            throw new MRAException(MRAException.Type.RATING_Parameter_Error);
        }

        int skip = matcher.group("skip") == null ? 0 : Integer.parseInt(matcher.group("skip"));
        int skipEnd = matcher.group("skipend") == null ? 0 : Integer.parseInt(matcher.group("skipend"));
        boolean rematch = matcher.group("rematch") == null || !matcher.group("rematch").equalsIgnoreCase("r");
        boolean keep = matcher.group("keep") == null || !matcher.group("keep").equalsIgnoreCase("f");

        var from = event.getSubject();
        MatchData data;
        try {
            data = calculate(matchID, skip, skipEnd, keep, rematch);
        } catch (Exception e) {
            log.error("MRA 数据计算失败", e);
            throw new MRAException(MRAException.Type.RATING_Client_CalculatingFailed);
        }

        if (matcher.group("main") != null) {
            byte[] img;
            try {
                img = imageService.getPanelC(data);
                QQMsgUtil.sendImage(from, img);
            } catch (Exception e) {
                NowbotApplication.log.error("MRA 数据请求失败", e);
                throw new MRAException(MRAException.Type.RATING_MRA_Error);
            }
        } else if (matcher.group("uu") != null) {
            //结果数据
            StringBuilder sb = new StringBuilder();
            sb.append(data.getMatchStat().getName()).append("\n")
                    .append(data.getTeamPoint().get("red")).append(" : ")
                    .append(data.getTeamPoint().get("blue")).append("\n")
                    .append("mp").append(data.getMatchStat().getId()).append(" ")
                    .append(data.getRounds().get(0).getTeamType()).append("\n");

            for (PlayerData p : data.getPlayerDataList()) {
                sb.append(String.format("#%d [%.2f] %s (%s)", p.getRanking(), p.getMRA(), p.getPlayer().getUserName(), p.getTeam().toUpperCase()))
                        .append(" ")
                        .append(String.format("%dW-%dL %d%% (%.2fM) [%.2f] [%s | %s]", p.getWin(), p.getLose(),
                                Math.round((double) p.getWin() * 100 / (p.getWin() + p.getLose())), p.getTTS() / 1000000d, p.getRWS() * 100d, p.getPlayerClass().getName(), p.getPlayerClass().getNameCN()))
                        .append("\n\n");
            }
            try {
                var receipt = from.sendMessage(sb.toString());
                from.recallIn(receipt, 60000);
            } catch (Exception e) {
                NowbotApplication.log.error("URA 数据请求失败", e);
                throw new MRAException(MRAException.Type.RATING_URA_Error);
            }
        }
    }


    public MatchData calculate(int matchID, int skip, int skipEnd, boolean keep, boolean rematch) throws MRAException {

        if (skip < 0) throw new MRAException(MRAException.Type.RATING_Parameter_SkipError);
        if (skipEnd < 0) throw new MRAException(MRAException.Type.RATING_Parameter_SkipEndError);

        Match match;
        try {
            match = osuMatchApiService.getMatchInfo(matchID, 10);
        } catch (Exception e) {
            throw new MRAException(MRAException.Type.RATING_Match_NotFound);
        }

        while (!match.getFirstEventId().equals(match.getEvents().get(0).getId())) {
            var events = osuMatchApiService.getMatchInfo(matchID, 10).getEvents();
            if (events.isEmpty()) throw new MRAException(MRAException.Type.RATING_Round_Empty);
            match.getEvents().addAll(0, events);
        }

        //真正的计算封装，就两行
        MatchData matchData = new MatchData(match, skip, skipEnd, !keep, rematch); //!keep = remove
        matchData.calculate();

        return matchData;
    }
}


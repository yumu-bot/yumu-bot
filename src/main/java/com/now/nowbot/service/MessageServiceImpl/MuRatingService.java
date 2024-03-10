package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.multiplayer.Match;
import com.now.nowbot.model.multiplayer.MatchData;
import com.now.nowbot.model.multiplayer.PlayerData;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import com.now.nowbot.throwable.ServiceException.MRAException;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instructions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

@Service("MU_RATING")
public class MuRatingService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(MuRatingService.class);

    @Autowired
    OsuMatchApiService osuMatchApiService;
    @Autowired
    ImageService imageService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instructions.MU_RATING.matcher(messageText);

        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    public record MRAParam(Integer matchID, Integer skip, Integer ignore, List<Integer> remove, Double easy, Boolean failed, Boolean rematch) {}

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        var matchIDStr = matcher.group("matchid");
        if (Objects.isNull(matchIDStr) || matchIDStr.isBlank()) {
            try {
                var md = DataUtil.getMarkdownFile("Help/rating.md");
                var image = imageService.getPanelA6(md, "help");
                from.sendImage(image);
                return;
            } catch (Exception e) {
                throw new MRAException(MRAException.Type.RATING_Instructions);
            }
        }

        var param = parseParam(matcher);
        MatchData data;

        try {
            data = calculate(param);
        } catch (MRAException e) {
            throw e;
        } catch (Exception e) {
            log.error("MRA 数据计算失败", e);
            throw new MRAException(MRAException.Type.RATING_Rating_CalculatingFailed);
        }

        if (matcher.group("main") != null) {
            byte[] image;
            try {
                image = imageService.getPanelC(data);
                from.sendImage(image);
            } catch (Exception e) {
                log.error("MRA 数据请求失败", e);
                throw new MRAException(MRAException.Type.RATING_Send_MRAFailed);
            }
        } else if (matcher.group("uu") != null) {
            String str = parseCSA(data);
            try {
                from.sendMessage(str).recallIn(60000);
            } catch (Exception e) {
                log.error("URA 数据请求失败", e);
                throw new MRAException(MRAException.Type.RATING_Send_URAFailed);
            }
        }
    }

    /**
     * 提取通用方法：将消息匹配变成 MRA 参数
     * @param matcher 消息匹配
     * @return MRA 参数
     * @throws MRAException 错误
     */
    public MRAParam parseParam(Matcher matcher) throws MRAException {
        int matchID;
        try {
            var matchIDStr = matcher.group("matchid");
            matchID = Integer.parseInt(matchIDStr);
        } catch (NumberFormatException e) {
            throw new MRAException(MRAException.Type.RATING_Parameter_MatchIDError);
        }

        int skip = matcher.group("skip") == null ? 0 : Integer.parseInt(matcher.group("skip"));
        int ignore = matcher.group("ignore") == null ? 0 : Integer.parseInt(matcher.group("ignore"));
        boolean failed = matcher.group("failed") == null || !matcher.group("failed").equalsIgnoreCase("f");
        boolean rematch = matcher.group("rematch") == null || !matcher.group("rematch").equalsIgnoreCase("r");

        List<Integer> remove = getIntegers(matcher);

        var easyStr = matcher.group("easy");
        double easy = 1d;

        if (Objects.nonNull(easyStr) && ! easyStr.isBlank()) {
            try {
                easy = Double.parseDouble(easyStr);
            } catch (NullPointerException | NumberFormatException e) {
                throw new MRAException(MRAException.Type.RATING_Parameter_EasyError);
            }
        }

        if (easy > 10d) throw new MRAException(MRAException.Type.RATING_Parameter_EasyTooLarge);
        if (easy < 0d) throw new MRAException(MRAException.Type.RATING_Parameter_EasyTooSmall);

        return new MRAParam(matchID, skip, ignore, remove, easy, failed, rematch);
    }


    @NonNull
    private static List<Integer> getIntegers(Matcher matcher) {
        var removeStrArr = matcher.group("remove");
        List<Integer> remove = new ArrayList<>();

        if (Objects.nonNull(removeStrArr) && ! removeStrArr.isBlank()) {
            var split = removeStrArr.split("[\\s,，\\-|:]+");
            for (var s : split) {
                int r;
                try {
                    r = Integer.parseInt(s);
                    remove.add(r);
                } catch (NumberFormatException ignored) {

                }
            }
        }
        return remove;
    }

    private String parseCSA(MatchData data) {
        //结果数据
        StringBuilder sb = new StringBuilder();
        sb.append(data.getMatchStat().getName()).append("\n")
                .append(data.getTeamPoint().get("red")).append(" : ")
                .append(data.getTeamPoint().get("blue")).append("\n")
                .append("mp").append(data.getMatchStat().getId()).append(" ")
                .append(data.getRoundList().getFirst().getTeamType()).append("\n");

        for (PlayerData p : data.getPlayerDataList()) {
            sb.append(String.format("#%d [%.2f] %s (%s)", p.getRanking(), p.getMRA(), p.getPlayer().getUserName(), p.getTeam().toUpperCase()))
                    .append(" ")
                    .append(String.format("%dW-%dL %d%% (%.2fM) [%.2f] [%s | %s]", p.getWin(), p.getLose(),
                            Math.round((double) p.getWin() * 100 / (p.getWin() + p.getLose())), p.getTTS() / 1000000d, p.getRWS() * 100d, p.getPlayerClass().getName(), p.getPlayerClass().getNameCN()))
                    .append("\n\n");
        }
        return sb.toString();
    }

    public MatchData calculate(MRAParam data) throws MRAException {
        return calculate(data.matchID, data.skip, data.ignore, data.remove, data.easy, data.failed, data.rematch);
    }

    public MatchData calculate(int matchID, int skip, int ignore, List<Integer> remove, double easy, boolean failed, boolean rematch) throws MRAException {

        if (skip < 0) throw new MRAException(MRAException.Type.RATING_Parameter_SkipError);
        if (ignore < 0) throw new MRAException(MRAException.Type.RATING_Parameter_SkipEndError);

        Match match;
        try {
            match = osuMatchApiService.getMatchInfo(matchID, 10);
        } catch (Exception e) {
            throw new MRAException(MRAException.Type.RATING_Match_NotFound);
        }

        while (!match.getFirstEventId().equals(match.getEvents().getFirst().getId())) {
            var events = osuMatchApiService.getMatchInfo(matchID, 10).getEvents();
            if (events.isEmpty()) throw new MRAException(MRAException.Type.RATING_Round_Empty);
            match.getEvents().addAll(0, events);
        }

        //真正的计算封装，就两行
        MatchData matchData = new MatchData(match, skip, ignore, remove, easy, failed, rematch); //!keep = remove
        matchData.calculate();

        return matchData;
    }
}


package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.Match;
import com.now.nowbot.model.multiplayer.MatchCalculate;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import com.now.nowbot.throwable.ServiceException.MRAException;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

@Service("MU_RATING")
public class MuRatingService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(MuRatingService.class);

    @Resource
    OsuMatchApiService matchApiService;
    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    ImageService imageService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instruction.MU_RATING.matcher(messageText);

        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    public record MRAParam(Integer matchID, MatchCalculate.CalculateParam calParam) {}

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
        MatchCalculate mc;

        try {
            mc = calculate(param, matchApiService, beatmapApiService);
        } catch (MRAException e) {
            throw e;
        } catch (Exception e) {
            log.error("MRA 数据计算失败", e);
            throw new MRAException(MRAException.Type.RATING_Rating_CalculatingFailed);
        }

        if (matcher.group("main") != null) {
            byte[] image;
            try {
                image = imageService.getPanelC(mc);
                from.sendImage(image);
            } catch (Exception e) {
                log.error("MRA 数据请求失败", e);
                throw new MRAException(MRAException.Type.RATING_Send_MRAFailed);
            }
        } else if (matcher.group("uu") != null) {
            String str = parseCSA(mc);
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
        double easy = getEasyMultiplier(matcher);

        return new MRAParam(matchID, new MatchCalculate.CalculateParam(skip, ignore, remove, easy, failed, rematch));
    }

    @NonNull
    private static double getEasyMultiplier(Matcher matcher) throws MRAException {
        var easyStr = matcher.group("easy");
        double easy = 1d;

        if (StringUtils.hasText(easyStr)) {
            try {
                easy = Double.parseDouble(easyStr);
            } catch (NullPointerException | NumberFormatException e) {
                throw new MRAException(MRAException.Type.RATING_Parameter_EasyError);
            }
        }

        if (easy > 10d) throw new MRAException(MRAException.Type.RATING_Parameter_EasyTooLarge);
        if (easy < 0d) throw new MRAException(MRAException.Type.RATING_Parameter_EasyTooSmall);
        return easy;
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

    private String parseCSA(MatchCalculate c) {
        var data = c.getMatchData();

        //结果数据
        StringBuilder sb = new StringBuilder();
        sb.append(c.getMatch().getMatchStat().getName()).append("\n")
                .append(data.getTeamPointMap().get("red")).append(" : ")
                .append(data.getTeamPointMap().get("blue")).append("\n")
                .append("mp").append(c.getMatch().getMatchStat().getMatchID()).append(" ")
                .append(data.isTeamVs()).append("\n");

        for (MatchCalculate.PlayerData p : data.getPlayerDataMap().values().stream().toList()) {
            sb.append(String.format("#%d [%.2f] %s (%s)", p.getRanking(), p.getMRA(), p.getPlayer().getUserName(), p.getTeam().toUpperCase()))
                    .append(" ")
                    .append(String.format("%dW-%dL %d%% (%.2fM) [%.2f] [%s | %s]", p.getWin(), p.getLose(),
                            Math.round((double) p.getWin() * 100 / (p.getWin() + p.getLose())), p.getTotal() / 1000000d, p.getRWS() * 100d, p.getPlayerClass().getName(), p.getPlayerClass().getNameCN()))
                    .append("\n\n");
        }
        return sb.toString();
    }

    public static MatchCalculate calculate(int matchID, int skip, int ignore, List<Integer> remove, double easy, boolean failed, boolean rematch, OsuMatchApiService matchApiService, OsuBeatmapApiService beatmapApiService) throws MRAException {
        var param = new MRAParam(matchID, new MatchCalculate.CalculateParam(skip, ignore, remove, easy, failed, rematch));

        return calculate(param, matchApiService, beatmapApiService);
    }

    public static MatchCalculate calculate(MRAParam param, OsuMatchApiService matchApiService, OsuBeatmapApiService beatmapApiService) throws MRAException {
        if (param.calParam().skip() < 0) throw new MRAException(MRAException.Type.RATING_Parameter_SkipError);
        if (param.calParam().ignore() < 0) throw new MRAException(MRAException.Type.RATING_Parameter_SkipEndError);

        Match match;
        try {
            match = matchApiService.getMatchInfo(param.matchID(), 10);
        } catch (Exception e) {
            throw new MRAException(MRAException.Type.RATING_Match_NotFound);
        }

        while (! match.getFirstEventID().equals(match.getEvents().getFirst().getEventID())) {
            var events = matchApiService.getMatchInfo(param.matchID(), 10).getEvents();
            if (events.isEmpty()) throw new MRAException(MRAException.Type.RATING_Round_Empty);
            match.getEvents().addAll(0, events);
        }

        return new MatchCalculate(match, param.calParam(), beatmapApiService);
    }
}


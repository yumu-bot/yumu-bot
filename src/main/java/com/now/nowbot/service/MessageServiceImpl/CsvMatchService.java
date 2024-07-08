package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.Match;
import com.now.nowbot.model.multiplayer.MatchCalculate;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import com.now.nowbot.throwable.ServiceException.MRAException;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

@Service("CSV")
public class CsvMatchService implements MessageService<Matcher> {
    Logger log = LoggerFactory.getLogger(CsvMatchService.class);
    static DateTimeFormatter Date1 = DateTimeFormatter.ofPattern("yy-MM-dd");
    static DateTimeFormatter Date2 = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Resource
    OsuMatchApiService matchApiService;
    @Resource
    OsuBeatmapApiService beatmapApiService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) throws Throwable {
        var m = Instructions.CSV_MATCH.matcher(messageText);
        if (m.find()) {
            /*
            if (! Permission.isGroupAdmin(event)) {
                throw new MRAException(MRAException.Type.RATING_Permission_OnlyGroupAdmin);
            }

             */
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    @CheckPermission(isGroupAdmin = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        var isMultiple = matcher.group("x") != null;
        int id = 0;
        List<Integer> ids = null;
        StringBuilder sb = new StringBuilder();

        if (isMultiple) {
            try {
                ids = parseDataString(matcher.group("data"));
                from.sendMessage("正在处理系列赛");
                parseCRAs(sb, ids);
            } catch (MRAException e) {
                throw e;
            } catch (Exception e) {
                log.error("CSV-Series 获取失败");
                throw new MRAException(MRAException.Type.RATING_Parameter_MatchIDError);
            }
        } else {
            try {
                id = Integer.parseInt(matcher.group("data"));
                from.sendMessage(STR."正在处理\{id}");
                parseCRA(sb, id);
            } catch (NullPointerException e) {
                throw new MRAException(MRAException.Type.RATING_Match_NotFound);
            } catch (MRAException e) {
                throw e;
            } catch (Exception e) {
                log.error("CSV-Round (Rating) 获取失败", e);
                throw new MRAException(MRAException.Type.RATING_Parameter_MatchIDError);
            }
        }

        //必须群聊
        if (from instanceof Group group) {
            try {
                if (isMultiple) {
                    if (Objects.nonNull(ids)) {
                        group.sendFile(sb.toString().getBytes(StandardCharsets.UTF_8), STR."\{ids.getFirst()}s.csv");
                    }
                } else {
                    group.sendFile(sb.toString().getBytes(StandardCharsets.UTF_8), STR."\{id}.csv");
                }
            } catch (Exception e) {
                log.error("比赛评分表：发送失败", e);
                throw new MRAException(MRAException.Type.RATING_Send_CRAFailed);
            }
        } else {
            throw new MRAException(MRAException.Type.RATING_Send_NotGroup);
        }
    }

    public void parseCRA(StringBuilder sb, int matchID) throws MRAException {
        Match match;

        try {
            match = matchApiService.getMatchInfo(matchID, 10);
        } catch (Exception e) {
            throw new MRAException(MRAException.Type.RATING_Match_NotFound);
        }

        var cal = new MatchCalculate(match, new MatchCalculate.CalculateParam(0, 0, null, 1d, true, true), beatmapApiService);
        var rounds = cal.getRounds();

        for (var r : rounds) {
            var scores = r.getScores();
            appendRoundStrings(sb, r);
            for (var s : scores) {
                getScoreStrings(sb, s);
            }
        }
    }

    public void parseCRAs(StringBuilder sb, List<Integer> matchIDs) throws MRAException {
        if (Objects.isNull(matchIDs) || matchIDs.isEmpty()) throw new MRAException(MRAException.Type.RATING_Series_FetchFailed);

        for (Integer matchID: matchIDs) {
            Match match;

            try {
                match = matchApiService.getMatchInfo(matchID, 10);
            } catch (Exception e) {
                throw new MRAException(MRAException.Type.RATING_Series_NotFound, matchID.toString());
            }

            var cal = new MatchCalculate(match, new MatchCalculate.CalculateParam(0, 0, null, 1d, true, true), beatmapApiService);
            var rounds = cal.getRounds();

            //多比赛
            appendMatchStrings(sb, match);
            for (var r : rounds) {
                var scores = r.getScores();
                appendRoundStrings(sb, r);
                for (var s : scores) {
                    appendScoreStringsLite(sb, s);
                }
            }

            //多比赛分隔符
            sb.append('\n');
        }
    }

    private void appendMatchStrings(StringBuilder sb, Match match){
        try {
            sb.append(match.getMatchStat().getStartTime().format(Date1)).append(',')
                    .append(match.getMatchStat().getStartTime().format(Date2)).append(',')
                    .append(match.getMatchStat().getMatchID()).append(',')
                    .append(match.getMatchStat().getName()).append(',')
                    .append('\n');
        } catch (Exception e) {
            sb.append(e.getMessage()).append('\n');//.append("  error---->")
        }
    }


    private void appendRoundStrings(StringBuilder sb, Match.MatchRound round){
        try {
            BeatMap b;

            if (Objects.nonNull(round.getBeatMap())) {
                b = round.getBeatMap();
            } else {
                b = new BeatMap();
                b.setStarRating(0f);
                b.setTotalLength(0);
                b.setBeatMapID(-1L);
                b.setMaxCombo(0);
            }

            sb.append(round.getStartTime().format(Date1)).append(',')
                    .append(round.getStartTime().format(Date2)).append(',')
                    .append(round.getMode()).append(',')
                    .append(round.getScoringType()).append(',')
                    .append(round.getTeamType()).append(',')
                    .append(b.getStarRating()).append(',')
                    .append(b.getTotalLength()).append(',')
                    .append(round.getMods().toString().replaceAll(", ", "|")).append(',')
                    .append(b.getBeatMapID()).append(',')
                    .append(b.getMaxCombo())
                    .append('\n');
        } catch (Exception e) {
            sb.append(e.getMessage()).append('\n');//.append("  error---->")
        }
    }

    private void getScoreStrings(StringBuilder sb, Match.MatchScore score){
        try {
            sb.append(score.getUserID()).append(',')
                    .append(String.format("%4.4f", score.getAccuracy())).append(',')
                    .append('[').append(String.join("|", score.getMods())).append("],")
                    .append(score.getScore()).append(',')
                    .append(score.getMaxCombo()).append(',')
                    .append(score.getPassed()).append(',')
                    .append(score.getPerfect()).append(',')
                    .append(score.getPlayerStat().slot()).append(',')
                    .append(score.getPlayerStat().team()).append(',')
                    .append(score.getPlayerStat().pass())
                    .append("\n");
        } catch (Exception e) {
            sb.append("<----MP ABORTED---->").append(e.getMessage()).append('\n');
        }
    }

    private void appendScoreStringsLite(StringBuilder sb, Match.MatchScore score){

        try {
            sb.append(score.getPlayerStat().team()).append(',')
                    .append(score.getUserID()).append(',')
                    .append(score.getUser().getUserName()).append(',')
                    .append(score.getScore()).append(',')
                    .append('[').append(String.join("|", score.getMods())).append("],")
                    .append(score.getMaxCombo()).append(',')
                    .append(String.format("%4.4f", score.getAccuracy())).append(',')
                    .append("\n");
        } catch (Exception e) {
            sb.append("<----MP ABORTED---->").append(e.getMessage()).append('\n');
        }
    }

    private List<Integer> parseDataString(String dataStr) throws MRAException {
        String[] dataStrArray = dataStr.trim().split("[\\s,，\\-|:]+");
        if (dataStr.isBlank() || dataStrArray.length == 0) return null;

        var matches = new ArrayList<Integer>();

        for (String s: dataStrArray) {
            int id;

            try {
                id = Integer.parseInt(s);
                matches.add(id);
            } catch (NumberFormatException e) {
                throw new MRAException(MRAException.Type.RATING_Series_NotFound, s);
            }
        }

        return matches;
    }
}

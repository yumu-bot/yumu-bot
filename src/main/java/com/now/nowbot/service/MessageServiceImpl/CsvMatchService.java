package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.model.multiplayer.Match;
import com.now.nowbot.model.multiplayer.MatchCal;
import com.now.nowbot.model.multiplayer.MatchRound;
import com.now.nowbot.model.multiplayer.MatchScore;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import com.now.nowbot.throwable.ServiceException.MRAException;
import com.now.nowbot.util.Instructions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    OsuMatchApiService osuMatchApiService;

    @Autowired
    CsvMatchService(OsuMatchApiService osuMatchApiService) {
        this.osuMatchApiService = osuMatchApiService;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = Instructions.CSVMATCH.matcher(event.getRawMessage().trim());
        if (m.find()) {
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
                throw new MRAException(MRAException.Type.RATING_Parameter_Error);
            }
        } else {
            try {
                id = Integer.parseInt(matcher.group("data"));
                from.sendMessage("正在处理" + id);
                parseCRA(sb, id);
            } catch (NullPointerException e) {
                throw new MRAException(MRAException.Type.RATING_Match_NotFound);
            } catch (MRAException e) {
                throw e;
            } catch (Exception e) {
                log.error("CSV-Round (Rating) 获取失败", e);
                throw new MRAException(MRAException.Type.RATING_Parameter_Error);
            }
        }

        //必须群聊
        if (from instanceof Group group) {
            try {
                if (isMultiple) {
                    if (Objects.nonNull(ids)) {
                        group.sendFile(sb.toString().getBytes(StandardCharsets.UTF_8), ids.get(0) + "s.csv");
                    }
                } else {
                    group.sendFile(sb.toString().getBytes(StandardCharsets.UTF_8), id + ".csv");
                }
            } catch (Exception e) {
                log.error("CSV: 发送失败", e);
                throw new MRAException(MRAException.Type.RATING_Send_CRAFailed);
            }
        } else {
            throw new MRAException(MRAException.Type.RATING_Send_NotGroup);
        }
    }

    public void parseCRA(StringBuilder sb, int matchID) throws MRAException {
        Match match;

        try {
            match = osuMatchApiService.getMatchInfo(matchID, 10);
        } catch (Exception e) {
            throw new MRAException(MRAException.Type.RATING_Match_NotFound);
        }

        var cal = new MatchCal(match, 0, 0, true, true);
        cal.addMicroUser4MatchScore();
        var rounds = cal.getRoundList();

        for (var r : rounds) {
            var scores = r.getScoreInfoList();
            getRoundStrings(sb, r);
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
                match = osuMatchApiService.getMatchInfo(matchID, 10);
            } catch (Exception e) {
                throw new MRAException(MRAException.Type.RATING_Series_NotFound, matchID.toString());
            }

            var cal = new MatchCal(match, 0, 0, true, true);
            cal.addMicroUser4MatchScore();
            var rounds = cal.getRoundList();

            //多比赛
            getMatchStrings(sb, match);
            for (var r : rounds) {
                var scores = r.getScoreInfoList();
                getRoundStrings(sb, r);
                for (var s : scores) {
                    getScoreStringsLite(sb, s);
                }
            }

            //多比赛分隔符
            sb.append('\n');
        }
    }

    private void getMatchStrings(StringBuilder sb, Match match){
        try {
            sb.append(match.getMatchStat().getStartTime().format(Date1)).append(',')
                    .append(match.getMatchStat().getStartTime().format(Date2)).append(',')
                    .append(match.getMatchStat().getId()).append(',')
                    .append(match.getMatchStat().getName()).append(',')
                    .append('\n');
        } catch (Exception e) {
            sb.append(e.getMessage()).append('\n');//.append("  error---->")
        }
    }


    private void getRoundStrings(StringBuilder sb, MatchRound round){
        try {
            sb.append(round.getStartTime().format(Date1)).append(',')
                    .append(round.getStartTime().format(Date2)).append(',')
                    .append(round.getMode()).append(',')
                    .append(round.getScoringType()).append(',')
                    .append(round.getTeamType()).append(',')
                    .append((round.getBeatmap().getDifficultyRating())).append(',')
                    .append(round.getBeatmap().getTotalLength()).append(',')
                    .append(round.getMods().toString().replaceAll(", ", "|")).append(',')
                    .append(round.getBeatmap().getId()).append(',')
                    .append(round.getBeatmap().getMaxCombo())
                    .append('\n');
        } catch (Exception e) {
            sb.append(e.getMessage()).append('\n');//.append("  error---->")
        }
    }

    private void getScoreStrings(StringBuilder sb, MatchScore score){
        try {
            sb.append(score.getUserId()).append(',')
                    .append(String.format("%4.4f", score.getAccuracy())).append(',')
                    .append('[').append(String.join("|", score.getMods())).append("],")
                    .append(score.getScore()).append(',')
                    .append(score.getMaxCombo()).append(',')
                    .append(score.isPassed()).append(',')
                    .append(score.isPerfect()).append(',')
                    .append(score.getSlot()).append(',')
                    .append(score.getTeam()).append(',')
                    .append(score.isPass())
                    .append("\n");
        } catch (Exception e) {
            sb.append("<----MP ABORTED---->").append(e.getMessage()).append('\n');
        }
    }

    private void getScoreStringsLite(StringBuilder sb, MatchScore score){

        try {
            sb.append(score.getTeam()).append(',')
                    .append(score.getUserId()).append(',')
                    .append(score.getUserName()).append(',')
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

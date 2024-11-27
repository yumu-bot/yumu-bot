package com.now.nowbot.service.messageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.model.json.BeatMap;
import com.now.nowbot.model.json.MicroUser;
import com.now.nowbot.model.multiplayer.Match;
import com.now.nowbot.model.multiplayer.MatchRating;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.osuApiService.OsuCalculateApiService;
import com.now.nowbot.service.osuApiService.OsuMatchApiService;
import com.now.nowbot.throwable.serviceException.MRAException;
import com.now.nowbot.util.Instruction;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
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
    OsuBeatmapApiService   beatmapApiService;
    @Resource
    OsuCalculateApiService calculateApiService;

    @Override
    public boolean isHandle(@NotNull MessageEvent event, @NotNull String messageText, @NotNull DataValue<Matcher> data) throws Throwable {
        var m = Instruction.CSV_MATCH.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    @CheckPermission(isGroupAdmin = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var isMultiple = matcher.group("x") != null;
        int id = 0;
        List<Integer> ids = null;
        StringBuilder sb = new StringBuilder();

        if (isMultiple) {
            try {
                ids = parseDataString(matcher.group("data"));
                event.reply("正在处理系列赛");
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
                event.reply("正在处理" + id);
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
        if (event.getSubject() instanceof Group group) {
            try {
                if (isMultiple) {
                    if (Objects.nonNull(ids)) {
                        group.sendFile(sb.toString().getBytes(StandardCharsets.UTF_8), ids.getFirst() + "s.csv");
                    }
                } else {
                    group.sendFile(sb.toString().getBytes(StandardCharsets.UTF_8), id + ".csv");
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

        var mr = new MatchRating(match, new MatchRating.RatingParam(0, 0, null, 1d, true, true), beatmapApiService, calculateApiService);
        mr.calculate();
        MatchRating.insertMicroUserToScores(mr);
        var rounds = mr.getRounds();

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

            var mr = new MatchRating(match, new MatchRating.RatingParam(0, 0, null, 1d, true, true), beatmapApiService, calculateApiService);
            mr.calculate();
            MatchRating.insertMicroUserToScores(mr);

            var rounds = mr.getRounds();

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
            sb.append(match.getStatistics().getStartTime().format(Date1)).append(',')
                    .append(match.getStatistics().getStartTime().format(Date2)).append(',')
                    .append(match.getStatistics().getMatchID()).append(',')
                    .append(match.getStatistics().getName()).append(',')
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
                b.setStarRating(0);
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
                    .append(score.getPlayerStat().getSlot()).append(',')
                    .append(score.getPlayerStat().getTeam()).append(',')
                    .append(score.getPlayerStat().getPass())
                    .append("\n");
        } catch (Exception e) {
            sb.append("<----MP ABORTED---->").append(e.getMessage()).append('\n');
        }
    }

    private void appendScoreStringsLite(StringBuilder sb, Match.MatchScore score){

        try {
            sb.append(score.getPlayerStat().getTeam()).append(',')
              .append(score.getUserID()).append(',')
              .append(Objects.requireNonNullElse(score.getUser(), new MicroUser()).getUserName()).append(',')
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

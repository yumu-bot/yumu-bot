package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.multiplayer.Match;
import com.now.nowbot.model.multiplayer.PlayerData;
import com.now.nowbot.model.multiplayer.Series;
import com.now.nowbot.model.multiplayer.SeriesData;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.contact.Group;
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
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

@Service("SERIES_RATING")
public class SeriesRatingService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(SeriesRatingService.class);

    @Autowired
    OsuMatchApiService osuMatchApiService;
    @Autowired
    ImageService imageService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instructions.SERIES_RATING.matcher(messageText);

        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        var dataStr = matcher.group("data");
        var nameStr = matcher.group("name");

        if (Objects.isNull(dataStr) || dataStr.isBlank()) {
            try {
                var md = DataUtil.getMarkdownFile("Help/series.md");
                var image = imageService.getPanelA6(md, "help");
                from.sendImage(image);
                return;
            } catch (Exception e) {
                throw new MRAException(MRAException.Type.RATING_Series_Instructions);
            }
        }

        var parsed = parseDataString(dataStr);
        List<Integer> matchIDs = parsed.matchIDs;
        List<Integer> skips = parsed.skips;
        List<Integer> ignores = parsed.ignores;
        List<List<Integer>> removes = parsed.removes;

        boolean rematch = matcher.group("rematch") == null || !matcher.group("rematch").equalsIgnoreCase("r");
        boolean failed = matcher.group("failed") == null || !matcher.group("failed").equalsIgnoreCase("f");

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

        if (matcher.group("csv") != null) {
            from.sendMessage(MRAException.Type.RATING_Series_Progressing.message);
        }

        if (matchIDs.size() > 50) {
            from.sendMessage(MRAException.Type.RATING_Series_TooManyMatch.message);
        }

        SeriesData data;
        try {
            data = calculate(matchIDs, nameStr, skips, ignores, removes, failed, rematch, easy, from);
        } catch (MRAException e) {
            throw e;
        } catch (Exception e) {
            log.error("SRA 数据计算失败", e);
            throw new MRAException(MRAException.Type.RATING_Rating_CalculatingFailed);
        }

        if (matcher.group("main") != null) {
            byte[] image;
            try {
                image = imageService.getPanelC2(data);
                from.sendImage(image);
            } catch (Exception e) {
                log.error("SRA 数据请求失败", e);
                throw new MRAException(MRAException.Type.RATING_Send_SRAFailed);
            }
        } else if (matcher.group("uu") != null) {
            String str = parseUSA(data);
            try {
                from.sendMessage(str).recallIn(60000);
            } catch (Exception e) {
                log.error("USA 发送失败", e);
                throw new MRAException(MRAException.Type.RATING_Send_USAFailed);
            }
        } else if (matcher.group("csv") != null) {
            //必须群聊
            if (from instanceof Group group) {
                try {
                    String str = parseCSA(data);
                    group.sendFile(str.getBytes(StandardCharsets.UTF_8),
                            STR."\{data.getSeries().getMatches().getFirst().getFirstEventId()}-results.csv");
                } catch (Exception e) {
                    log.error("CSA:", e);
                    throw new MRAException(MRAException.Type.RATING_Send_CSAFailed);
                }
            } else {
                throw new MRAException(MRAException.Type.RATING_Send_NotGroup);
            }
        }
    }

    private String parseCSA(SeriesData data) {
        StringBuilder sb = new StringBuilder();

        sb.append("#").append(',')
                .append("UID").append(',')
                .append("UserName").append(',')
                .append("MRA").append(',')
                .append("RWS").append(',')
                .append("Win%").append(',')
                .append("Play%").append(',')
                .append('W').append(',')
                .append('L').append(',')
                .append('P').append(',')
                .append("玩家分类").append(',')
                .append("Player Classification").append(',')
                .append("Class Color")
                .append('\n');

        for (PlayerData p: data.getSeries().getPlayerDataList()) {
            sb.append(parsePlayer2CSA(p));
        }

        return sb.toString();
    }

    private String parsePlayer2CSA(PlayerData data) {
        StringBuilder sb = new StringBuilder();

        double winRate = 1d * data.getWin() / (data.getWin() + data.getLose());
        double playRate = 1d * data.getRRAs().size() / data.getARC();

        try {
            sb.append(data.getRanking()).append(',')
                    .append(data.getPlayer().getId()).append(',')
                    .append(data.getPlayer().getUserName()).append(',')
                    .append(String.format("%.2f", Math.round(data.getMRA() * 100d) / 100d)).append(',')
                    .append(String.format("%.2f", Math.round(data.getRWS() * 10000d) / 100d)).append(',')
                    .append(String.format("%.0f", Math.round(winRate * 100d) * 1d)).append('%').append(',')
                    .append(String.format("%.0f", Math.round(playRate * 100d) * 1d)).append('%').append(',')
                    .append(data.getWin()).append(',')
                    .append(data.getLose()).append(',')
                    .append(data.getWin() + data.getLose()).append(',')
                    .append(data.getClassNameCN()).append(',')
                    .append(data.getClassName()).append(',')
                    .append(data.getClassColor())
                    .append("\n");
        } catch (Exception e) {
            sb.append("<----User Nullified---->").append(e.getMessage()).append('\n');
        }
        return sb.toString();
    }


    private String parseUSA(SeriesData data) {
        //结果数据
        StringBuilder sb = new StringBuilder();

        sb.append(data.getSeries().getSeriesStat().getName()).append("\n")
                .append("M").append(data.getMatchCount()).append(" R").append(data.getRoundCount()).append(" P").append(data.getPlayerCount()).append(" S").append(data.getScoreCount()).append("\n");

        for (PlayerData p : data.getSeries().getPlayerDataList()) {
            sb.append(String.format("#%d [%.2f] %s", p.getRanking(), p.getMRA(), p.getPlayer().getUserName()))
                    .append(" ")
                    .append(String.format("%dW-%dL %d%% (%.2fM) [%.2f] [%s | %s]", p.getWin(), p.getLose(),
                            Math.round((double) p.getWin() * 100 / (p.getWin() + p.getLose())), p.getTTS() / 1000000d, p.getRWS() * 100d, p.getPlayerClass().getName(), p.getPlayerClass().getNameCN()))
                    .append("\n\n");
        }
        return sb.toString();
    }

    public enum Status {
        ID,
        SKIP,
        IGNORE,
        REMOVE_RECEIVED,
        REMOVE_FINISHED,
        OK
    }

    public record ParsedData(List<Integer> matchIDs, List<Integer> skips, List<Integer> ignores, List<List<Integer>> removes) {}

    public ParsedData parseDataString(String dataStr) throws MRAException {
        String[] dataStrArray = dataStr.trim().split("[\\s,，\\-|:]+");
        if (dataStr.isBlank() || dataStrArray.length == 0) return null;

        List<Integer> matchIDs = new ArrayList<>();
        List<Integer> skips = new ArrayList<>();
        List<Integer> ignores = new ArrayList<>();
        List<List<Integer>> removes = new ArrayList<>();

        Status status = Status.ID; //0：收取 matchID 状态，1：收取 skip 状态，2：收取 ignore 状态。3：收取 remove 状态。 4：无需收取，直接输出。
        int matchID = 0;
        int skip = 0;
        int ignore = 0;
        List<Integer> remove = new ArrayList<>();

        for (int i = 0; i < dataStrArray.length; i++) {

            int v;
            String s = dataStrArray[i];
            if (s == null || s.isBlank()) continue;

            if (s.contains("[")) {
                status = Status.REMOVE_RECEIVED;
                s = s.replaceAll("\\[", "");
            }

            if (s.contains("]") && status == Status.REMOVE_RECEIVED) {
                status = Status.REMOVE_FINISHED;
                s = s.replaceAll("]", "");
            }

            try {
                v = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new MRAException(MRAException.Type.RATING_Parse_ParameterError, s, String.valueOf(i));
            }

            switch (status) {
                case REMOVE_RECEIVED -> remove.add(v);
                case REMOVE_FINISHED -> {
                    removes.add(remove);
                    remove.clear();
                    status = Status.OK;
                }
            }


            //如果最后一个参数是场比赛，需要重复 parse（结算）
            if (i == dataStrArray.length - 1) {
                if (v < 1000) {
                    switch (status) {
                        case SKIP -> {
                            matchIDs.add(matchID);
                            skips.add(v);
                            ignores.add(0);
                            status = Status.IGNORE;
                        }
                        case IGNORE -> {
                            matchIDs.add(matchID);
                            skips.add(skip);
                            ignores.add(v);
                            status = Status.ID;
                        }
                        case REMOVE_RECEIVED ->
                                throw new MRAException(MRAException.Type.RATING_Parse_MissingRemove, String.valueOf(v), String.valueOf(i));
                        case OK -> {
                            matchIDs.add(matchID);
                            skips.add(skip);
                            ignores.add(ignore);
                            status = Status.ID;
                        }
                        default -> throw new MRAException(MRAException.Type.RATING_Parse_MissingMatch, String.valueOf(v), String.valueOf(i));
                    }
                } else {
                    switch (status) {
                        case SKIP -> {
                            matchIDs.add(matchID);
                            skips.add(0);
                            ignores.add(0);
                        }
                        case IGNORE -> {
                            matchIDs.add(matchID);
                            skips.add(skip);
                            ignores.add(0);
                        }
                        case OK -> {
                            matchIDs.add(matchID);
                            skips.add(skip);
                            ignores.add(ignore);
                        }
                    }
                    matchIDs.add(v);
                    skips.add(0);
                    ignores.add(0);

                    status = Status.ID;
                }
            } else {
                //正常 parse
                if (v < 1000) {
                    switch (status) {
                        case SKIP -> {
                            skip = v;
                            status = Status.IGNORE;
                        }
                        case IGNORE -> {
                            ignore = v;
                            status = Status.OK;
                        }
                        case ID, OK -> throw new MRAException(MRAException.Type.RATING_Parse_MissingMatch, String.valueOf(v), String.valueOf(i));
                    }
                } else {
                    switch (status) {
                        case ID -> {
                            matchID = v;
                            status = Status.SKIP;
                        }
                        case SKIP, IGNORE, OK -> {
                            matchIDs.add(matchID);
                            skips.add(skip);
                            ignores.add(ignore);

                            matchID = v;
                            skip = 0;
                            ignore = 0;
                            status = Status.SKIP;
                        }
                    }
                }
            }
        }

        return new ParsedData(matchIDs, skips, ignores, removes);
    }


    public SeriesData calculate(List<Integer> matchIDs, @Nullable String name, List<Integer> skips, List<Integer> ignores, List<List<Integer>> removes, boolean failed, boolean rematch, Double easy, @Nullable Contact from) throws MRAException {

        List<Match> matches = new ArrayList<>();
        int fetchMapFail = 0;
        for (int m: matchIDs) {
            try {
                matches.add(osuMatchApiService.getMatchInfo(m, 10));
            } catch (HttpClientErrorException.TooManyRequests | WebClientResponseException.TooManyRequests e) {

                fetchMapFail ++;
                if (fetchMapFail > 3) {
                    log.error("SRA 查询次数超限", e);
                    throw new MRAException(MRAException.Type.RATING_Series_TooManyRequest, String.valueOf(m));
                }

                if (from != null) {
                    from.sendMessage(MRAException.Type.RATING_Series_ReachThreshold.message);
                }

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e1) {
                    log.error("SRA 休眠意外中断", e1);
                    throw new MRAException(MRAException.Type.RATING_Series_SleepingInterrupted, String.valueOf(m));
                }
            } catch (HttpClientErrorException.NotFound | WebClientResponseException.NotFound e) {
                log.error("SRA 对局找不到", e);

                if (from != null) {
                    from.sendMessage(String.format(MRAException.Type.RATING_Series_NotFound.message, m));
                }

            } catch (Exception e) {
                log.error("SRA 对局获取失败", e);
                throw new MRAException(MRAException.Type.RATING_Series_FetchFailed);
            }
        }
        //真正的计算封装，就两行

        SeriesData data = new SeriesData(new Series(matches), name, skips, ignores, removes, easy, failed, rematch);
        data.calculate();

        return data;
    }
}

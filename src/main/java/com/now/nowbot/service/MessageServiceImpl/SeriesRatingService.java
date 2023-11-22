package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
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
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("SRA")
public class SeriesRatingService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(SeriesRatingService.class);

    @Autowired
    OsuMatchApiService osuMatchApiService;
    @Autowired
    ImageService imageService;



    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)((?<uu>(u{1,2})(seriesrating|series|sra(?![a-zA-Z_])|sa(?![a-zA-Z_])))|(ym)?(?<main>(seriesrating|series|sa(?![a-zA-Z_])|sra(?![a-zA-Z_])))|(ym)?(?<csv>(csvseriesrating|csvseries|csa(?![a-zA-Z_])|cs(?![a-zA-Z_]))))\\s*(#(?<name>\\p{all})#)?(?<data>[\\d\\s]+)?(\\s*(?<rematch>[Rr]))?(\\s*(?<keep>[Ff]))?");

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
        var dataStr = matcher.group("data");
        var nameStr = matcher.group("name");

        if (dataStr == null || dataStr.isBlank()) {
            throw new MRAException(MRAException.Type.RATING_Parameter_SeriesNone);
        }

        var parsed = parseDataString(dataStr);
        List<Integer> matchIDs = parsed.get("matchIDs");
        List<Integer> skips = parsed.get("skips");
        List<Integer> skipEnds = parsed.get("skipEnds");

        boolean rematch = matcher.group("rematch") == null || !matcher.group("rematch").equalsIgnoreCase("r");
        boolean keep = matcher.group("keep") == null || !matcher.group("keep").equalsIgnoreCase("f");

        var from = event.getSubject();

        if (matchIDs.size() > 50) {
            from.sendMessage("一次性输入的对局太多！计算的时候可能会遇到 API 瓶颈。");
            //from.sendMessage(String.valueOf(MRAException.Type.RATING_Series_TooManyMatches));
        }

        SeriesData data;
        try {
            data = calculate(matchIDs, nameStr, skips, skipEnds, !keep, rematch, from);
        } catch (MRAException e) {
            throw e;
        } catch (Exception e) {
            log.error("SRA 数据计算失败", e);
            throw new MRAException(MRAException.Type.RATING_Client_CalculatingFailed);
        }

        if (matcher.group("main") != null) {
            byte[] img;
            try {
                img = imageService.getPanelC2(data);
                QQMsgUtil.sendImage(from, img);
            } catch (Exception e) {
                NowbotApplication.log.error("SRA 数据请求失败", e);
                throw new MRAException(MRAException.Type.RATING_Send_SRAFailed);
            }
        } else if (matcher.group("uu") != null) {
            String str = parseUSA(data);
            try {
                var receipt = from.sendMessage(str);
                from.recallIn(receipt, 60000);
            } catch (Exception e) {
                NowbotApplication.log.error("USA 发送失败", e);
                throw new MRAException(MRAException.Type.RATING_Send_USAFailed);
            }
        } else if (matcher.group("csv") != null) {
            String str = parseCSA(data);
            //必须群聊
            if (from instanceof Group group) {
                try {
                    from.sendMessage("正在处理系列赛");
                    group.sendFile(str.getBytes(StandardCharsets.UTF_8), data.getSeries().getMatches().get(0).getFirstEventId() + "-results.csv");
                } catch (Exception e) {
                    NowbotApplication.log.error("CSA:", e);
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
                .append("Player Classification").append(',')
                .append("玩家分类").append(',')
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
                    .append(data.getMRA()).append(',')
                    .append(String.format("%.2f", data.getRWS() * 100d)).append(',')
                    .append(String.format("%.0f", winRate * 100d)).append(',')
                    .append(String.format("%.0f", playRate * 100d)).append(',')
                    .append(data.getWin()).append(',')
                    .append(data.getLose()).append(',')
                    .append(data.getWin() + data.getLose()).append(',')
                    .append(data.getClassName()).append(',')
                    .append(data.getClassNameCN()).append(',')
                    .append('#').append(
                            String.format("%6s", Integer.toHexString((data.getClassColor() << 8) >>> 8)).replace(' ', '0')

                    )
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

    public Map<String, List<Integer>> parseDataString(String dataStr) throws MRAException {
        String[] dataStrArray = dataStr.trim().split("\\s+|,+|，+|\\|+");
        if (dataStr.isBlank() || dataStrArray.length == 0) return null;

        List<Integer> matchIDs = new ArrayList<>();
        List<Integer> skips = new ArrayList<>();
        List<Integer> skipEnds = new ArrayList<>();

        int status = 0; //0：收取 matchID 状态，1：收取 skip 状态，2：收取 skipEnd 状态。3：无需收取，直接输出。
        int matchID = 0;
        int skip = 0;
        int skipEnd = 0;

        for (int i = 0; i < dataStrArray.length; i++) {

            int v;
            String s = dataStrArray[i];
            if (s == null || s.isBlank()) continue;
            try {
                v = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new MRAException(MRAException.Type.RATING_Parse_ParameterError, s, String.valueOf(i));
            }

            //如果最后一个参数是场比赛，需要重复 parse（结算）
            if (i == dataStrArray.length - 1) {
                if (v < 1000) {
                    switch (status) {
                        case 1 -> {
                            matchIDs.add(matchID);
                            skips.add(v);
                            skipEnds.add(0);
                            status = 0;
                        }
                        case 2 -> {
                            matchIDs.add(matchID);
                            skips.add(skip);
                            skipEnds.add(v);
                            status = 0;
                        }
                        case 3 -> {
                            matchIDs.add(matchID);
                            skips.add(skip);
                            skipEnds.add(skipEnd);
                            status = 0;
                        }
                        case 0 -> throw new MRAException(MRAException.Type.RATING_Parse_MissingMatch, String.valueOf(v), String.valueOf(i));
                    }
                } else {
                    switch (status) {
                        case 1 -> {
                            matchIDs.add(matchID);
                            skips.add(0);
                            skipEnds.add(0);
                        }
                        case 2 -> {
                            matchIDs.add(matchID);
                            skips.add(skip);
                            skipEnds.add(0);
                        }
                        case 3 -> {
                            matchIDs.add(matchID);
                            skips.add(skip);
                            skipEnds.add(skipEnd);
                        }
                    }
                    matchIDs.add(v);
                    skips.add(0);
                    skipEnds.add(0);

                    status = 0;
                }
            } else {
                //正常 parse
                if (v < 1000) {
                    switch (status) {
                        case 1 -> {
                            skip = v;
                            status = 2;
                        }
                        case 2 -> {
                            skipEnd = v;
                            status = 3;
                        }
                        case 0, 3 -> throw new MRAException(MRAException.Type.RATING_Parse_MissingMatch, String.valueOf(v), String.valueOf(i));
                    }
                } else {
                    switch (status) {
                        case 0 -> {
                            matchID = v;
                            status = 1;
                        }
                        case 1, 2, 3 -> {
                            matchIDs.add(matchID);
                            skips.add(skip);
                            skipEnds.add(skipEnd);

                            matchID = v;
                            skip = 0;
                            skipEnd = 0;
                            status = 1;
                        }
                    }
                }
            }
        }

        return Map.of(
                "matchIDs", matchIDs,
                "skips", skips,
                "skipEnds", skipEnds
        );
    }


    public SeriesData calculate(List<Integer> matchIDs, @Nullable String name, List<Integer> skips, List<Integer> skipEnds, boolean remove, boolean rematch, @Nullable Contact from) throws MRAException {

        List<Match> matches = new ArrayList<>();
        int fetchMapFail = 0;
        for (int m: matchIDs) {
            try {
                matches.add(osuMatchApiService.getMatchInfo(m, 10));
            } catch (HttpClientErrorException.TooManyRequests e) {

                fetchMapFail ++;
                if (fetchMapFail > 3) {
                    log.error("SRA 查询次数超限", e);
                    throw new MRAException(MRAException.Type.RATING_Series_TooManyRequest, String.valueOf(m));
                }

                if (from != null) {
                    from.sendMessage("遇到 API 瓶颈！等待 10 秒后再次尝试获取！");
                }

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e1) {
                    log.error("SRA 休眠意外中断", e1);
                    throw new MRAException(MRAException.Type.RATING_Series_SleepingInterrupted, String.valueOf(m));
                }
            } catch (HttpClientErrorException.NotFound e) {
                log.error("SRA 对局找不到", e);
                if (from != null) {
                    from.sendMessage(String.format("小沐找不到这一系列比赛中的 %s 哦！\n请检查房间号是否正确、房间记录是否过期！", m));
                }
                //throw new MRAException(MRAException.Type.RATING_Series_NotFound, String.valueOf(m));
            } catch (HttpClientErrorException e) {
                log.error("SRA 对局获取失败", e);
                throw new MRAException(MRAException.Type.RATING_Series_FetchFailed, String.valueOf(m));
            }
        }
        //真正的计算封装，就两行

        SeriesData data = new SeriesData(new Series(matches), name, skips, skipEnds, remove, rematch);
        data.calculate();

        return data;
    }
}

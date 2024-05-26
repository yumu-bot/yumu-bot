package com.now.nowbot.util;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.MessageServiceImpl.MapStatisticsService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.throwable.TipsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// 封装一些消息处理（Handle）的常用方法
public class HandleUtil {
    public static final  String             REG_START        = "[!！/](?i)(ym)?";
    public static final  String             REG_SPACE        = "\\s*";
    public static final  String             REG_SPACE_1P     = "\\s+";
    public static final  String             REG_SPACE_01     = "\\s?";
    public static final  String             REG_COLUMN       = "[:：]";
    public static final  String             REG_HASH         = "[#＃]";
    public static final  String             REG_HYPHEN       = "[\\-－]";
    public static final  String             REG_NAME         = "(\\*?(?<name>[0-9a-zA-Z\\[\\]\\-_][0-9a-zA-Z\\[\\]\\-_ ]{2,}?))";
    public static final  String             REG_QQ           = "(qq=(?<qq>\\d{5,}))";
    public static final  String             REG_UID          = "(uid=(?<uid>\\d+))";
    public static final  String             REG_MOD          = "(\\+?(?<mod>(EZ|NF|HT|HR|SD|PF|DT|NC|HD|FI|FL|SO|[1-9]K|CP|MR|RD|TD)+))";
    public static final  String             REG_MODE         = "(?<mode>osu|taiko|ctb|fruits?|mania|std|0|1|2|3|o|m|c|f|t)";
    public static final String REG_RANGE = "(?<range>(100|\\d{1,2})([\\-－]\\d{1,3})?)";
    public static final  String             REG_RANGE_DAY    = "(?<range>\\d{1,3}([\\-－]\\d{1,3})?)";
    public static final  String             REG_ID           = "(?<id>\\d+)";
    public static final  String             REG_BID          = "(?<bid>\\d+)";
    public static final  String             REG_SID          = "(?<sid>\\d+)";

    private static final Logger             log              = LoggerFactory.getLogger(HandleUtil.class);
    private static       BindDao            bindDao;
    private static       OsuUserApiService  userApiService;
    private static       OsuScoreApiService scoreApiService;
    private static       OsuBeatmapApiService beatmapApiService;

    public static void init(ApplicationContext applicationContext) {
        bindDao = applicationContext.getBean(BindDao.class);
        userApiService = applicationContext.getBean(OsuUserApiService.class);
        scoreApiService = applicationContext.getBean(OsuScoreApiService.class);
        beatmapApiService = applicationContext.getBean(OsuBeatmapApiService.class);
    }

    public static CommandPatternBuilder createPattern() {
        return new CommandPatternBuilder();
    }

    // 判定用于匹配的类型内是否含有此文本
    public static boolean messageMatchesPattern(@NonNull MessageEvent event, @NonNull String regex) {
        var m = Pattern.compile(regex).matcher(event.getRawMessage());
        return m.find();
    }

    /**
     * 退避机制，为真则需要退避
     * @param event 消息事件
     * @param keyWords 关键词，可以输入多个，忽略大小写
     * @return 为真，则需要退避
     */
    public static boolean isAvoidance(@NonNull MessageEvent event, @NonNull String... keyWords) {
        for (var key : keyWords) {
            if (event.getRawMessage().toLowerCase()
                    .contains(key.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取一个 user, 优先获取别人, 没找到就自己
     */
    public static OsuUser getUser(@NonNull MessageEvent event, @NonNull Matcher matcher, @NonNull int maximum) throws TipsException {
        OsuMode mode = getMode(matcher);

        var u = getOtherUser(event, matcher, mode, maximum);

        if (Objects.nonNull(u)) return u;

        return getMyselfUser(event, mode);
    }

    @NonNull
    public static OsuMode getMode(@NonNull Matcher matcher) {
        OsuMode mode = OsuMode.DEFAULT;
        try {
            var modeStr = matcher.group("mode");
            if (StringUtils.hasText(modeStr)) {
                mode = OsuMode.getMode(modeStr);
            }
        } catch (Exception ignore) {
            // 没有 mode
        }
        return mode;
    }

    /**
     * 处理默认模组。没有的话，获取传进来的玩家的模组
     * @param matcher 匹配
     * @param user 玩家
     * @return 游戏模式
     */
    @NonNull
    public static OsuMode getModeOrElse(@NonNull Matcher matcher, @NonNull OsuUser user) {
        return getModeOrElse(getMode(matcher), user);
    }

    /**
     * 处理默认模组。没有的话，获取传进来的玩家的模组
     * @param matcher 匹配
     * @param user 绑定玩家
     * @return 游戏模式
     */
    @NonNull
    public static OsuMode getModeOrElse(@NonNull Matcher matcher, @NonNull BinUser user) {
        return getModeOrElse(getMode(matcher), user);
    }

    /**
     * 处理默认模组。没有的话，获取传进来的玩家的模组
     * @param mode 通过以上方法匹配得到的游戏模式
     * @param user 玩家
     * @return 游戏模式
     */
    public static OsuMode getModeOrElse(@Nullable OsuMode mode, @NonNull OsuUser user) {
        if (mode == null) {
            return OsuMode.DEFAULT;
        } else if (mode.equals(OsuMode.DEFAULT) && user.getOsuMode() != null) {
            return user.getOsuMode();
        } else {
            return mode;
        }
    }

    /**
     * 处理默认模组。没有的话，获取传进来的玩家的模组
     * @param mode 通过以上方法匹配得到的游戏模式
     * @param user 绑定玩家
     * @return 游戏模式
     */
    public static OsuMode getModeOrElse(@Nullable OsuMode mode, @NonNull BinUser user) {
        if (mode == null) {
            return OsuMode.DEFAULT;
        } else if (mode.equals(OsuMode.DEFAULT)) {
            return user.getMode();
        } else {
            return mode;
        }
    }

    @Nullable
    public static OsuUser getOtherUser(MessageEvent event, Matcher matcher, @Nullable OsuMode mode) throws TipsException {
        return getOtherUser(event, matcher, mode, 100);
    }

    @Nullable
    public static OsuUser getOtherUser(MessageEvent event, Matcher matcher, @Nullable OsuMode mode, @NonNull int maximum) throws TipsException {
        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);

        long qq = 0;
        long uid = 0;

        try {
            var qqStr = matcher.group("qq");

            // at 比 qq= 等级高
            if (Objects.nonNull(at)) {
                qq = at.getTarget();
            } else if (StringUtils.hasText(qqStr)) {
                qq = Long.parseLong(qqStr);
            }
        } catch (RuntimeException ignore) {
            // 没 @ 也没 qq=
            try {
                var uidStr = matcher.group("uid");

                if (StringUtils.hasText(uidStr)) {
                    uid = Long.parseLong(uidStr);
                }
            } catch (RuntimeException ignore2) {

            }
        }

        if (qq != 0) {
            var user = bindDao.getUserFromQQ(qq);

            try {
                if (OsuMode.isDefault(mode)) mode = user.getMode();
                return userApiService.getPlayerInfo(user, mode);
            } catch (WebClientResponseException.Unauthorized e) {
                throw new GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Player);
            } catch (WebClientResponseException.NotFound e) {
                throw new GeneralTipsException(GeneralTipsException.Type.G_Null_Player, user.getOsuName());
            } catch (WebClientResponseException.Forbidden e) {
                throw new GeneralTipsException(GeneralTipsException.Type.G_Banned_Player, user.getOsuName());
            } catch (WebClientResponseException e) {
                throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI);
            } catch (Exception e) {
                log.error("HandleUtil：获取玩家信息失败！", e);
                throw new TipsException("HandleUtil：获取玩家信息失败！");
            }
        }

        if (uid != 0) {
            try {
                return userApiService.getPlayerInfo(uid, mode);
            } catch (WebClientResponseException.NotFound e) {
                throw new GeneralTipsException(GeneralTipsException.Type.G_Null_Player, uid);
            } catch (WebClientResponseException.Forbidden e) {
                throw new GeneralTipsException(GeneralTipsException.Type.G_Banned_Player, uid);
            } catch (WebClientResponseException e) {
                throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI);
            } catch (Exception e) {
                log.error("HandleUtil：获取玩家信息失败！", e);
                throw new TipsException("HandleUtil：获取玩家信息失败！");
            }
        }

        try {
            var name = matcher.group("name");
            var nameWithoutSpace = name.trim();
            // 对叫100(或者1000，取自 maximum)的人直接取消处理
            if (StringUtils.hasText(name) && name.length() > (String.valueOf(maximum).length() - 1) && ! String.valueOf(maximum).equals(nameWithoutSpace)) {
                try {
                    return userApiService.getPlayerInfo(nameWithoutSpace, mode);
                } catch (WebClientResponseException.NotFound e) {
                    throw new GeneralTipsException(GeneralTipsException.Type.G_Null_Player, nameWithoutSpace);
                } catch (WebClientResponseException.Forbidden e) {
                    throw new GeneralTipsException(GeneralTipsException.Type.G_Banned_Player, nameWithoutSpace);
                } catch (WebClientResponseException e) {
                    throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI);
                } catch (Exception e) {
                    log.error("HandleUtil：获取玩家信息失败！", e);
                    throw new TipsException("HandleUtil：获取玩家信息失败！");
                }
            }
        } catch (RuntimeException ignore) {
            // 没名字
        }

        // 没 at 没 qq= 没 uid= 也没名字 直接返回 null
        return null;
    }

    public static OsuUser getMyselfUser(@NonNull MessageEvent event, @Nullable OsuMode mode) throws TipsException {
        var qq = event.getSender().getId();
        var user = bindDao.getUserFromQQ(qq);

        try {
            if (OsuMode.isDefault(mode)) mode = user.getMode();
            return userApiService.getPlayerInfo(user, mode);

        } catch (WebClientResponseException.Unauthorized e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me);
        } catch (WebClientResponseException.NotFound e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Null_Player, user.getOsuName());
        } catch (WebClientResponseException.Forbidden e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Banned_Player, user.getOsuName());
        } catch (WebClientResponseException e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI);
        } catch (Exception e) {
            log.error("HandleUtil：获取自我信息失败！", e);
            throw new TipsException("HandleUtil：获取自我信息失败！");
        }
    }

    // MapStatisticsService 专属
    public static MapStatisticsService.Expected getExpectedScore(Matcher matcher, @NonNull BeatMap beatMap, @Nullable OsuMode mode) throws TipsException {

        double accuracy;
        int combo;
        int miss;

        try {
            accuracy = Double.parseDouble(matcher.group("accuracy"));
        } catch (RuntimeException e) {
            accuracy = 1d;
        }

        try {
            combo = Integer.parseInt(matcher.group("combo"));
        } catch (RuntimeException e) {
            combo = 0;
        }

        try {
            miss = Integer.parseInt(matcher.group("miss"));
        } catch (RuntimeException e) {
            miss = 0;
        }

        List<String> mods;

        try {
            mods = Mod.getModsAbbrList(matcher.group("mod"));
        } catch (RuntimeException e) {
            mods = new ArrayList<>();
        }

        // 标准化 acc 和 combo
        Integer maxCombo = beatMap.getMaxCombo();

        if (maxCombo != null) {
            if (combo <= 0) {
                combo = maxCombo;
            } else {
                combo = Math.min(combo, maxCombo);
            }
        }

        if (combo < 0) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Wrong_ParamCombo);
        }
        if (accuracy > 1d && accuracy <= 100d) {
            accuracy /= 100d;
        } else if (accuracy > 100d && accuracy <= 10000d) {
            accuracy /= 10000d;
        } else if (accuracy <= 0d || accuracy > 10000d) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Wrong_ParamAccuracy);
        }

        //只有转谱才能赋予游戏模式
        if (mode == null || mode.equals(OsuMode.DEFAULT) && OsuMode.getMode(beatMap.getMode()).equals(OsuMode.OSU)) {
            mode = OsuMode.getMode(beatMap.getMode());
        }

        return new MapStatisticsService.Expected(mode, accuracy, combo, miss, mods);

    }


    public static Map<Integer, Score> getTodayBPList(OsuUser user, Matcher matcher, @Nullable OsuMode mode) throws TipsException {
        var range = parseRange(matcher, null, true);

        final int later = range.offset();
        final int earlier = range.limit();

        List<Score> BPList;

        try {
            BPList = scoreApiService.getBestPerformance(user.getUID(), mode, 0, 100);
        } catch (WebClientResponseException.Forbidden e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Banned_Player, user.getUsername());
        } catch (WebClientResponseException.NotFound e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Null_BP, user.getUsername());
        } catch (WebClientResponseException e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI);
        } catch (Exception e) {
            log.error("HandleUtil：获取今日最好成绩失败！", e);
            throw new TipsException("HandleUtil：获取今日最好成绩失败！");
        }

        //筛选
        LocalDateTime laterDay = LocalDateTime.now().minusDays(later);
        LocalDateTime earlierDay = LocalDateTime.now().minusDays(earlier);

        var dataMap = new TreeMap<Integer, Score>();

        BPList.forEach(
                ContextUtil.consumerWithIndex(
                        (s, index) -> {
                            if (s.getCreateTimePretty().isBefore(laterDay) && s.getCreateTimePretty().isAfter(earlierDay)) {
                                dataMap.put(index + later, s);
                            }
                        }
                )
        );

        return dataMap;
    }

    public static Map<Integer, Score> getOsuBPMap(OsuUser user, Matcher matcher, @Nullable OsuMode mode) throws TipsException {
        return getOsuBPMap(user, matcher, mode, false);
    }

    //isMultipleDefault20是给bs默认 20 用的，其他情况下 false 就可以
    public static Map<Integer, Score> getOsuBPMap(OsuUser user, Matcher matcher, @Nullable OsuMode mode, boolean isMultipleDefault20) throws TipsException {
        var range = parseRange(matcher, isMultipleDefault20 ? 20 : null, false);

        int offset = range.offset();
        int limit = range.limit();

        return getOsuBPMap(user, mode, offset, limit);
    }

    // 重载
    public static Map<Integer, Score> getOsuBPMap(OsuUser user, @Nullable OsuMode mode, int offset, int limit) throws TipsException {
        List<Score> BPList = getOsuBPList(user, mode, offset, limit);

        var dataMap = new TreeMap<Integer, Score>();
        BPList.forEach(
                ContextUtil.consumerWithIndex(
                        (s, index) -> dataMap.put(index + offset, s)
                )
        );
        return dataMap;
    }

    // 单独的拿 bp 榜
    public static List<Score> getOsuBPList(OsuUser user, @Nullable OsuMode mode, int offset, int limit) throws TipsException {
        try {
            return scoreApiService.getBestPerformance(user.getUID(), mode, offset, limit);
        } catch (WebClientResponseException.NotFound e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Null_BP);
        } catch (WebClientResponseException e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI);
        }
    }

    // 这个没有保底，有保底的请使用 getOsuBeatMapOrElse
    @Nullable
    public static BeatMap getOsuBeatMap(@NonNull Matcher matcher) throws TipsException {
        long bid;
        try {
            bid = Long.parseLong(matcher.group("bid"));
        } catch (RuntimeException e) {
            return null;
        }

        return getOsuBeatMap(bid);
    }

    @NonNull
    public static BeatMap getOsuBeatMap(long bid) throws TipsException {
        try {
            return beatmapApiService.getBeatMapInfo(bid);
        } catch (WebClientResponseException.NotFound e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Null_Map);
        } catch (WebClientResponseException e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI);
        } catch (Exception e) {
            log.error("HandleUtil：获取谱面信息失败！", e);
            throw new TipsException("HandleUtil：获取谱面信息失败！");
        }
    }

    // 这个有保底
    public static BeatMap getOsuBeatMapOrElse(long bid) throws TipsException {
        try {
            return beatmapApiService.getBeatMapInfo(bid);
        } catch (WebClientResponseException.NotFound e) {
            try {
                return beatmapApiService.getBeatMapSetInfo(bid).getTopDiff();
            } catch (WebClientResponseException.NotFound e1) {
                throw new GeneralTipsException(GeneralTipsException.Type.G_Null_Map);
            } catch (WebClientResponseException e1) {
                throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI);
            }
        } catch (Exception e) {
            log.error("HandleUtil：获取谱面信息失败！", e);
            throw new TipsException("HandleUtil：获取谱面信息失败！");
        }
    }

    private record Range(int offset, int limit) {}

    /**
     * @param defaultLimit 第二个参数的默认值
     * @param isForceRange 没有第二个参数的情况下强制从 0 到第一个参数
     */
    private static Range parseRange(Matcher matcher, Integer defaultLimit, boolean isForceRange) {
        int n;
        int m;

        try {
            var range = matcher.group("range");
            var rangeArray = range.split("-");
            if (rangeArray.length == 2) {
                n = Integer.parseInt(rangeArray[0]) - 1;
                m = Integer.parseInt(rangeArray[1]);
            } else if (isForceRange) {
                n = 0;
                m = Integer.parseInt(rangeArray[0]);
            } else {
                // 只有一个数字就是查询 bp n
                n = Integer.parseInt(rangeArray[0]) - 1;
                m = n + 1;
            }

            // 处理 m n 的极值
            if (n > m) {
                n = n + m;
                m = n - m;
                n = n - m;
            } else if (n == m) {
                m = n + 1;
            }

            if (n < 0) n = 0;
            m = m - n;
            if (m < 1) m = 1;
        } catch (IllegalStateException | IllegalArgumentException | NullPointerException e) {
            // 没有 range 默认是 1？
            // !bs = !BP 1 - 20，默认是 1 直接给我功能干废了！
            n = 0;
            m = Objects.requireNonNullElse(defaultLimit, 1);
        }

        return new Range(n, m);
    }

    // 指令样式生成
    public static class CommandPatternBuilder {
        StringBuilder patternStr = new StringBuilder();

        public CommandPatternBuilder() {
            patternStr.append('^').append(REG_START).append(REG_SPACE);
        }

        /**
         * 一些特殊的指令开始 比如 #calc 等
         *
         */
        public CommandPatternBuilder(String start) {
            patternStr.append('^').append(start).append(REG_SPACE);
        }

        /**
         * 加命令
         */
        public CommandPatternBuilder appendCommands(Iterable<String> commands) {
            startGroup();
            for (var command : commands) {
                patternStr.append(command).append('|');
            }
            patternStr.deleteCharAt(patternStr.length() - 1);
            endGroup();
            return this;
        }

        /**
         * 加命令
         */
        public CommandPatternBuilder appendCommands(String... commands) {
            startGroup();
            for (var command : commands) {
                patternStr.append(command).append('|');
            }
            patternStr.deleteCharAt(patternStr.length() - 1);
            endGroup();
            return this;
        }

        /**
         * 避免不必要的命令重复，所带来的指令污染。比如：!ymb 和 !ymbind。如果前面并未加 (?!ind)，则后面的指令也会在前面被错误匹配（获取叫 ind 玩家的 bp）
         * @param ignores 需要忽略的其后字符。
         * @return this
         */
        public CommandPatternBuilder appendIgnores(@NonNull String... ignores) {
            patternStr.append("(?!");
            for (var ignore : ignores) {
                patternStr.append(ignore).append('|');
            }
            patternStr.deleteCharAt(patternStr.length() - 1);
            patternStr.append(")");
            return this;
        }

        // 等于忽略 A-Za-z_
        public CommandPatternBuilder appendIgnoreAlphabets() {
            return this.appendIgnoreArea("A-Z", "a-z", "_");
        }

        /**
         * 避免不必要的命令重复，所带来的指令污染。比如：!ymb 和 !ymbind。如果前面并未加 (?!ind)，则后面的指令也会在前面被错误匹配（获取叫 ind 玩家的 bp）
         * @param areas 注意，areas 可以是单个字符，也可以是包含连字符 - 的区间。
         * @return this
         */
        public CommandPatternBuilder appendIgnoreArea(@NonNull String... areas) {

            patternStr.append("(?![");
            for (var area : areas) {
                patternStr.append(area);
            }
            patternStr.deleteCharAt(patternStr.length() - 1);
            patternStr.append("])");
            return this;
        }

        /**
         * 加一段
         */
        public CommandPatternBuilder append(String str) {
            patternStr.append(str);
            return this;
        }

        // 默认就是 Any
        public CommandPatternBuilder appendSpace() {
            return this.appendSpaceAny();
        }

        // 支持 0-1
        public CommandPatternBuilder appendSpace(int num) {
            return switch (num) {
                case 0 -> this.appendSpaceOnce();
                case 1 -> this.appendSpaceLeast();
                default -> this.appendSpaceAny();
            };
        }

        // 0-∞
        private CommandPatternBuilder appendSpaceAny() {
            patternStr.append(REG_SPACE);
            return this;
        }

        // 1-∞
        private CommandPatternBuilder appendSpaceLeast() {
            patternStr.append(REG_SPACE_1P);
            return this;
        }

        // 0-1
        private CommandPatternBuilder appendSpaceOnce() {
            patternStr.append(REG_SPACE_01);
            return this;
        }

        /**
         * 前面自己带冒号
         */
        public CommandPatternBuilder appendMode(boolean nullable) {
            startGroup();
            append(REG_COLUMN);
            append(REG_MODE);
            endGroup();
            if (nullable) whatever();
            return this;
        }

        public CommandPatternBuilder startGroup() {
            patternStr.append('(');
            return this;
        }

        public CommandPatternBuilder endGroup() {
            patternStr.append(')');
            return this;
        }

        public CommandPatternBuilder appendKeyWord(String str) {
            return this.appendKeyWord(str, true);
        }

        public CommandPatternBuilder appendKeyWordWithSpace(String str) {
            return this.appendKeyWordWithSpace(str, true);
        }

        // 构建类似于 (?<s>s)? 这样匹配特定字符的序列
        public CommandPatternBuilder appendKeyWord(String str, boolean whatever) {
            patternStr.append("(?<").append(str).append('>').append(str).append(")");

            return whatever ? whatever() : more();
        }

        // 构建类似于 (?<s>s)? 这样匹配特定字符的序列，加空格
        public CommandPatternBuilder appendKeyWordWithSpace(String str, boolean include0) {
            return this.appendKeyWord(str, include0).append(REG_SPACE);
        }

        public CommandPatternBuilder appendWithSpace(String str) {
            patternStr.append(str).append(REG_SPACE);
            return this;
        }

        public CommandPatternBuilder appendQQ(boolean nullable) {
            append(REG_QQ);
            if (nullable) whatever();
            return this;
        }

        public CommandPatternBuilder appendName(boolean nullable) {
            append(REG_NAME);
            if (nullable) whatever();
            return this;
        }

        public CommandPatternBuilder appendUID(boolean nullable) {
            append(REG_UID);
            if (nullable) whatever();
            return this;
        }

        public CommandPatternBuilder appendRange(boolean nullable) {
            startGroup();
            append("\\s+");
            append(REG_HASH).whatever().appendSpace();
            append(REG_RANGE);
            endGroup();
            if (nullable) whatever();
            return this;
        }

        public CommandPatternBuilder appendRange1000(boolean nullable) {
            startGroup();
            appendSpace();
            append(REG_HASH).whatever();
            appendSpace();
            append(REG_RANGE_DAY);
            endGroup();
            if (nullable) whatever();
            return this;
        }

        public CommandPatternBuilder end() {
            append("$");
            return this;
        }

        public Pattern build() {
            return Pattern.compile(patternStr.toString());
        }


        /**
         * 0-1，?，whatever 你可以理解成无所谓，随便的意思
         * @return this
         */
        public CommandPatternBuilder whatever() {
            patternStr.append('?');
            return this;
        }

        /**
         * 0-∞，*
         * @return this
         */
        public CommandPatternBuilder any() {
            patternStr.append('*');
            return this;
        }

        /**
         * 1-∞，+
         * @return this
         */
        public CommandPatternBuilder more() {
            patternStr.append('+');
            return this;
        }
    }
}

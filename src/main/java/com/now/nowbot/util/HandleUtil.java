package com.now.nowbot.util;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.TipsException;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// 封装一些消息处理（Handle）的常用方法
public class HandleUtil {
    @Language("RegExp")
    private static final String REG_USER_AND_RANGE = "(?<ur>([0-9a-zA-Z\\[\\]\\-_][0-9a-zA-Z\\[\\]\\-_ ]+[0-9a-zA-Z\\[\\]\\-_])?([#＃]?((\\d{1,3})[\\-－ ])?(\\d{1,3}))?)?";
    @Language("RegExp")
    public static final  String REG_START          = "[!！/](?i)(ym)?";
    @Language("RegExp")
    public static final  String REG_SPACE          = "\\s*";
    @Language("RegExp")
    public static final  String REG_SPACE_1P       = "\\s+";
    @Language("RegExp")
    public static final  String REG_SPACE_01       = "\\s?";
    @Language("RegExp")
    public static final  String REG_COLUMN         = "[:：]";
    @Language("RegExp")
    public static final  String REG_HASH           = "[#＃]";
    @Language("RegExp")
    public static final  String REG_HYPHEN         = "[\\-－]";
    @Language("RegExp")
    public static final  String REG_NAME           = "(\\*?(?<name>[0-9a-zA-Z\\[\\]\\-_][0-9a-zA-Z\\[\\]\\-_ ]{2,}?))";
    @Language("RegExp")
    public static final  String REG_QQ             = "(qq=(?<qq>\\d{5,}))";
    @Language("RegExp")
    public static final  String REG_UID            = "(uid=(?<uid>\\d+))";
    @Language("RegExp")
    public static final  String REG_MOD            = "(\\+?(?<mod>(EZ|NF|HT|HR|SD|PF|DT|NC|HD|FI|FL|SO|[1-9]K|CP|MR|RD|TD)+))";
    @Language("RegExp")
    public static final  String REG_MODE           = "(?<mode>osu|taiko|ctb|fruits?|mania|std|0|1|2|3|o|m|c|f|t)";
    @Language("RegExp")
    public static final  String REG_RANGE          = "(?<range>(100|\\d{1,2})([\\-－]\\d{1,3})?)";
    @Language("RegExp")
    public static final  String REG_RANGE_DAY      = "(?<range>\\d{1,3}([\\-－]\\d{1,3})?)";
    @Language("RegExp")
    public static final  String REG_ID             = "(?<id>\\d+)";
    @Language("RegExp")
    public static final  String REG_BID            = "(?<bid>\\d+)";
    @Language("RegExp")
    public static final  String REG_SID            = "(?<sid>\\d+)";

    private static final Logger               log = LoggerFactory.getLogger(HandleUtil.class);
    private static       BindDao              bindDao;
    private static       OsuUserApiService    userApiService;
    private static       OsuScoreApiService   scoreApiService;
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

    /**
     * 退避机制，为真则需要退避
     *
     * @param text     消息
     * @param keyWords 关键词，可以输入多个，忽略大小写
     * @return 为真，则需要退避
     */
    public static boolean isAvoidance(@NonNull String text, @NonNull String... keyWords) {
        for (var key : keyWords) {
            if (text.toLowerCase()
                    .contains(key.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    public static OsuMode getMode(@NonNull Matcher matcher) {// 没有 mode
        return getMode(matcher, null);
    }

    @NonNull
    public static OsuMode getMode(@NonNull Matcher matcher, OsuMode other) {
        OsuMode mode = OsuMode.DEFAULT;
        try {
            var modeStr = matcher.group("mode");
            if (StringUtils.hasText(modeStr)) {
                mode = OsuMode.getMode(modeStr);
            }
        } catch (Exception ignore) {
            // 没有 mode
        }
        if (OsuMode.isDefaultOrNull(mode) && !OsuMode.isDefaultOrNull(other)) {
            return other;
        }
        return mode;
    }

    /**
     * 处理默认模组。没有的话，获取传进来的玩家的模组
     *
     * @param matcher 匹配
     * @param user    玩家
     * @return 游戏模式
     */
    @NonNull
    public static OsuMode getModeOrElse(@NonNull Matcher matcher, @NonNull OsuUser user) {
        return getModeOrElse(getMode(matcher), user);
    }

    /**
     * 处理默认模组。没有的话，获取传进来的玩家的模组
     *
     * @param matcher 匹配
     * @param user    绑定玩家
     * @return 游戏模式
     */
    @NonNull
    public static OsuMode getModeOrElse(@NonNull Matcher matcher, @NonNull BinUser user) {
        return getModeOrElse(getMode(matcher), user);
    }

    /**
     * 处理默认模组。没有的话，获取传进来的玩家的模组
     *
     * @param mode 通过以上方法匹配得到的游戏模式
     * @param user 玩家
     * @return 游戏模式
     */
    public static OsuMode getModeOrElse(@Nullable OsuMode mode, @Nullable OsuUser user) {
        if (OsuMode.isDefaultOrNull(mode)) {
            if (user == null || OsuMode.isDefaultOrNull(user.getCurrentOsuMode())) {
                return OsuMode.DEFAULT;
            } else {
                return user.getCurrentOsuMode();
            }
        } else {
            return mode;
        }
    }

    /**
     * 处理默认模组。没有的话，获取传进来的玩家的模组
     *
     * @param mode 通过以上方法匹配得到的游戏模式
     * @param user 绑定玩家
     * @return 游戏模式
     */
    public static OsuMode getModeOrElse(@Nullable OsuMode mode, @Nullable BinUser user) {
        if (OsuMode.isDefaultOrNull(mode)) {
            if (user == null || OsuMode.isDefaultOrNull(user.getOsuMode())) {
                return OsuMode.DEFAULT;
            } else {
                return user.getOsuMode();
            }
        } else {
            return mode;
        }
    }

    /**
     * 获取其他玩家, 支持 @ / qq= / uid= / data= , 注意不再处理 range
     *
     * @param event   必须
     * @param matcher 必须
     * @param mode    可选
     * @return user
     * @throws TipsException 提示
     */
    @Nullable
    public static OsuUser getOtherUser(MessageEvent event, Matcher matcher, @Nullable OsuMode mode) throws TipsException {
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
        }

        if (qq != 0) {
            var user = bindDao.getUserFromQQ(qq);
            if (OsuMode.isDefaultOrNull(mode)) mode = user.getOsuMode();

            return getOsuUser(user.getOsuName(), mode);
        }

        try {
            var uidStr = matcher.group("uid");
            if (StringUtils.hasText(uidStr)) {
                uid = Long.parseLong(uidStr);
            }
        } catch (RuntimeException ignore) {
            // 没 uid=
        }

        if (uid != 0) {
            return getOsuUser(uid, mode);
        }

        try {
            // 这里不再处理包含range的情况
            var name = matcher.group("name");
            if (!StringUtils.hasText(name)) return null;
            var id = userApiService.getOsuId(name);
            return getOsuUser(id, mode);
        } catch (IllegalStateException | IllegalArgumentException ignore) {
            // 没名字，就别管了
        }

        // 没 at 没 qq= 没 uid= 也没名字 直接返回 null
        return null;
    }

    private static OsuUser getUserWithSelfOrOtherAndRange(MessageEvent event, Matcher matcher, UserAndRange range, @Nullable OsuMode mode, AtomicBoolean isMyself) throws TipsException {
        isMyself.set(false);
        OsuUser user = getOtherUser(event, matcher, mode);
        if (user == null && range.user == null) {
            isMyself.set(true);
        } else if (range.user != null) {
            user = range.user;
        }

        if (user == null) {
            user = getMyselfUser(event.getSender().getId(), mode);
        }
        return user;
    }

    public static OsuUser getMyselfUser(Long qq, @Nullable OsuMode mode) throws BindException {
        var user = bindDao.getUserFromQQ(qq);

        if (OsuMode.isDefaultOrNull(mode)) mode = user.getOsuMode();

        return userApiService.getPlayerInfo(user, mode);
    }


    /**
     *
     * @param defaultLimit 默认20
     * @return 如果是 null 则说明要退避
     * @throws TipsException
     */
    public static Map<Integer, Score> getBPList(MessageEvent event, Matcher matcher,String text, @Nullable OsuMode mode, int defaultLimit) throws TipsException {
        var data = getUserAndRange(matcher, mode);
        var isMySelf = new AtomicBoolean(false);
        OsuUser user;
        try {
            user = getUserWithSelfOrOtherAndRange(event, matcher, data, mode, isMySelf);
            mode = user.getCurrentOsuMode();
        } catch (BindException e) {
            if (isMySelf.get() && HandleUtil.isAvoidance(text, "bp")) {
                log.info(String.format("指令退避：BP 退避成功，被退避的玩家：%s", event.getSender().getName()));
                return null;
            } else {
                throw e;
            }
        }
        var range = data.range;
        int offset = 0;
        int limit = defaultLimit;

        if (range.range2 > 0) {
            offset = range.range1 - 1;
            limit = range.range2 - offset;
        } else if (range.range1 > 0) {
            limit = range.range1;
        }

        return getBPList(user, mode, offset, limit);
    }

    // 重载
    public static Map<Integer, Score> getBPList(OsuUser user, @Nullable OsuMode mode, int offset, int limit) throws TipsException {
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
            return scoreApiService.getBestPerformance(user.getUserID(), mode, offset, limit);
        } catch (WebClientResponseException.NotFound e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Null_BP);
        } catch (WebClientResponseException e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI);
        }
    }

    // 单独拿成绩
    public static List<Score> getOsuScoreList(OsuUser user, @Nullable OsuMode mode, int offset, int limit, boolean isPass) throws TipsException {
        try {
            return scoreApiService.getRecent(user.getUserID(), mode, offset, limit, isPass);
        } catch (WebClientResponseException.NotFound e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Null_BP);
        } catch (WebClientResponseException e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI);
        }
    }

    public static List<Score> getOsuScoreList(OsuUser user, @Nullable OsuMode mode, Range range, boolean isPass) throws TipsException {
        return getOsuScoreList(user, mode, 0, 0, isPass);
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

    public record Range(int range1, int range2) {
    }

    /**
     * 用 getUserAndRange
     *
     * @param defaultLimit         第二个参数的默认值
     * @param parseLimitWhen1Param 只有一个参数时，匹配 1-此位置
     */
    @Deprecated
    public static Range parseRange(Matcher matcher, Integer defaultLimit, boolean parseLimitWhen1Param) {
        return parseRange(matcher.group("range"), defaultLimit, parseLimitWhen1Param);
    }

    /**
     * @param defaultLimit         第二个参数的默认值
     * @param parseLimitWhen1Param 只有一个参数时，匹配 1-此位置
     */
    public static Range parseRange(String str, Integer defaultLimit, boolean parseLimitWhen1Param) {
        int n;
        int m;

        try {
            var rangeArray = str.split("-");
            if (rangeArray.length == 2) {
                n = Integer.parseInt(rangeArray[0]) - 1;
                m = Integer.parseInt(rangeArray[1]);
            } else if (parseLimitWhen1Param) {
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

    public record UserAndRange(@Nullable OsuUser user, Range range) {
    }

    /**
     * @param matcher 正则
     * @return 用户和范围
     */
    private static UserAndRange getUserAndRange(Matcher matcher, OsuMode mode) {
        if (!matcher.namedGroups().containsKey("ur")) throw new RuntimeException("No match found");
        if (Objects.isNull(mode)) mode = OsuMode.DEFAULT;

        var text = matcher.group("ur");

        int rangeStart;
        int rangeEnd = -1;

        // 只有数字, 不包含名字
        if (text.matches("^(\\d{1,2}[\\-－ ])?\\d{1,3}$")) {

            if (text.contains("-") || text.contains("－") || text.contains(" ")) {
                var range = text.split("[\\-－ ]");
                rangeStart = Integer.parseInt(range[0]);
                rangeEnd = Integer.parseInt(range[1]);
            } else {
                rangeStart = Integer.parseInt(text);
            }
            return new UserAndRange(null, new Range(rangeStart, rangeEnd));
        }

        // 包含名字
        int[] data;
        if (text.contains("#")) {
            data = getNameAndRangeHasHash(text);
        } else {
            data = getNameAndRangeWithoutHash(text);
        }

        rangeStart = data[3];
        rangeEnd = data[4];
        // 优先级: 双参数 > 单参数 > 无参数
        // yhc 22 33 优先级: yhc#22-33 > yhc 22#23 > yhc 22 23
        if (data[2] > 0) {
            // 双参数
            try {
                var name = text.substring(0, data[2]);
                var id = userApiService.getOsuId(name);
                var user = getOsuUser(id, mode);
                return new UserAndRange(user, new Range(rangeStart, rangeEnd));
            } catch (Exception ignore) {
            }
        }

        if (data[1] > 0) {
            // 单参数
            try {
                var name = text.substring(0, data[1]);
                var id = userApiService.getOsuId(name);
                var user = getOsuUser(id, mode);
                return new UserAndRange(user, new Range(rangeStart, -1));
            } catch (Exception ignore) {
            }
        }

        // 无参数
        try {
            var name = text.substring(0, data[0]);
            var id = userApiService.getOsuId(name);
            var user = getOsuUser(id, mode);
            return new UserAndRange(user, new Range(-1, -1));
        } catch (Exception ignore) {
            throw new BindException(BindException.Type.BIND_Player_NotFound);
        }
    }

    private static int[] getNameAndRangeHasHash(String text) {
        final int hashIndex = text.indexOf('#');
        final var result = new int[]{hashIndex, -1, -1, -1, -1};
        final String rangeStr = text.substring(hashIndex + 1);

        try {
            final String[] range = rangeStr.split("[\\-－ ]");
            result[3] = Integer.parseInt(range[0]);
            if (range.length == 2) {
                result[4] = Integer.parseInt(range[1]);
            }
        } catch (Exception e) {
            log.debug("range 解析参数有误: {}", rangeStr, e);
            return result;
        }
        return result;
    }

    /**
     *
     * @param text data&range
     * @return [nameIndex, name1, name2, range1, range2]
     */
    private static int[] getNameAndRangeWithoutHash(String text) {
        int[] nameSet = new int[]{-1, -1};
        Consumer<Integer> setNameSet = value -> {
            if (nameSet[0] < 0) nameSet[0] = value;
            else if (nameSet[1] < 0) nameSet[1] = value;
        };
        // 这是 osu_name 最小长度的 index 值
        final int minIndex = 2;
        // 记录完整的名字
        int nameAll = text.length();

        int m = -1, n = -1;

        // 倒序解析
        int index = nameAll - 1;
        int i = 0;
        int tempChar;
        while ((tempChar = text.charAt(index)) >= '0' && tempChar <= '9') {
            index--;
            i++;
        }

        numAll:
        {
            if (i <= 0 || i > 3 || index < minIndex) {
                // 对应末尾不是数字, 直接忽略 range
                // 对应着末尾的数字大于1000, 直接认为是名字的一部分不进行处理, la2333 这种
                break numAll;
            }

            m = Integer.parseInt(text.substring(index + 1));
            // 记录名字减去末尾1-3位数字
            setNameSet.accept(index + 1);

            if (tempChar != '-' && tempChar != '－' && tempChar != ' ') {
                // 对应末尾不是 - 或者 空格, 直接忽略剩余 range
                break numAll;
            }

            index--;
            i = 0;
            while ((tempChar = text.charAt(index)) >= '0' && tempChar <= '9') {
                index--;
                i++;
            }

            if (i <= 0 || i > 3 || index < minIndex) {
                // 与上面同理
                break numAll;
            }
            n = Integer.parseInt(text.substring(index + 1, nameSet[1] - 1));
            setNameSet.accept(index + 1);
        }

        return new int[]{nameAll, nameSet[0], nameSet[1], m, n};
    }

    /**************************************************************************************************/




































    public static OsuUser getOsuUser(String name, OsuMode mode) throws TipsException {
        try {
            return userApiService.getPlayerInfo(name, mode);
        } catch (WebClientResponseException.NotFound e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Null_Player, name);
        } catch (WebClientResponseException.Forbidden e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Banned_Player, name);
        } catch (WebClientResponseException e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI);
        } catch (Exception e) {
            log.error("HandleUtil：获取玩家信息失败！", e);
            throw new TipsException("获取玩家信息失败！");
        }
    }

    public static OsuUser getOsuUser(long uid, OsuMode mode) throws TipsException {
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
            throw new TipsException("获取玩家信息失败！");
        }
    }

    // 指令样式生成
    public static class CommandPatternBuilder {
        StringBuilder patternStr = new StringBuilder();

        public CommandPatternBuilder() {
            patternStr.append('^').append(REG_START).append(REG_SPACE);
        }

        /**
         * 一些特殊的指令开始 比如 #calc 等
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
         *
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
         *
         * @param areas 注意，areas 可以是单个字符，也可以是包含连字符 - 的区间。
         * @return this
         */
        public CommandPatternBuilder appendIgnoreArea(@NonNull String... areas) {

            patternStr.append("(?![");
            for (var area : areas) {
                patternStr.append(area);
            }
            // 你这别硬抄啊, 前面是有 .append('|') 为了去掉最后一个 | 才这样写的
            // patternStr.deleteCharAt(patternStr.length() - 1);
            patternStr.append("])");
            return this;
        }

        /**
         * 加一个
         */
        public CommandPatternBuilder append(String str) {
            patternStr.append(str);
            return this;
        }

        /**
         * 加一段
         */
        public CommandPatternBuilder appendArea(String str) {
            patternStr.append('[').append(str).append(']');
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

        public CommandPatternBuilder appendNameAndRange(boolean nullable) {
            append(REG_USER_AND_RANGE);
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
         *
         * @return this
         */
        public CommandPatternBuilder whatever() {
            patternStr.append('?');
            return this;
        }

        /**
         * 0-∞，*
         *
         * @return this
         */
        public CommandPatternBuilder any() {
            patternStr.append('*');
            return this;
        }

        /**
         * 1-∞，+
         *
         * @return this
         */
        public CommandPatternBuilder more() {
            patternStr.append('+');
            return this;
        }
    }
}

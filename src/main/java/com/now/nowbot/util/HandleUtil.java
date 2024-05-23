package com.now.nowbot.util;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.TipsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// 封装一些消息处理（Handle）的常用方法
public class HandleUtil {
    public static final  String             REG_START       = "[!！/](?i)(ym)?";
    public static final  String             REG_SPACE       = "(\\s*)";
    public static final  String             REG_SPACE_COLON = "([:：])";
    public static final  String             REG_SPACE_HASH  = "([#＃])";
    public static final  String             REG_SPACE_HYPEN = "(-)";
    public static final  String             REG_NAME        = "(?<name>[0-9a-zA-Z\\[\\]-_][0-9a-zA-Z\\[\\]-_ ]{2,}?)";
    public static final  String             REG_QQ          = "(qq=(?<qq>\\d{5,}))";
    public static final  String             REG_MOD         = "(?<mod>(EZ|NF|HT|HR|SD|PF|DT|NC|HD|FI|FL|SO|[1-9]K|CP|MR|RD|TD)+)";
    public static final  String             REG_MODE        = "(?<mode>osu|taiko|ctb|fruits?|mania|std|0|1|2|3|o|m|c|f|t)";
    public static final  String             REG_RANGE       = "(?<range>\\d{1,2}([-－]\\d{1,3})?)";
    public static final  String             REG_ID          = "(?<id>\\d+)";
    public static final  String             REG_BID         = "(?<bid>\\d+)";
    public static final  String             REG_SID         = "(?<sid>\\d+)";
    private static final Logger             log             = LoggerFactory.getLogger(HandleUtil.class);
    private static       BindDao            bindDao;
    private static       OsuUserApiService  userApiService;
    private static       OsuScoreApiService scoreApiService;

    public static void init(ApplicationContext applicationContext) {
        bindDao = applicationContext.getBean(BindDao.class);
        userApiService = applicationContext.getBean(OsuUserApiService.class);
        scoreApiService = applicationContext.getBean(OsuScoreApiService.class);
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
    public static OsuUser getUser(@NonNull MessageEvent event, @NonNull Matcher matcher) throws TipsException {
        OsuMode mode = getMode(matcher);

        var u = getOtherUser(event, matcher, mode);

        if (Objects.nonNull(u)) return u;

        return getMyselfUser(event, mode);
    }

    public static OsuMode getMode(Matcher matcher) {
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

    @Nullable
    public static OsuUser getOtherUser(MessageEvent event, Matcher matcher, @Nullable OsuMode mode) throws TipsException {
        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);

        long qq = 0;

        try {
            var qqStr = matcher.group("qq");

            // at 比 qq= 等级高
            if (Objects.nonNull(at)) {
                qq = at.getTarget();
            } else if (StringUtils.hasText(qqStr)) {
                qq = Long.parseLong(qqStr);
            }
        } catch (IllegalArgumentException | IllegalStateException ignore) {
            // 没 @ 也没 qq=
        }

        if (qq != 0) {
            var user = bindDao.getUserFromQQ(qq);

            try {
                if (OsuMode.isDefault(mode)) mode = user.getMode();
                return userApiService.getPlayerInfo(user, mode);
            } catch (WebClientResponseException.Unauthorized e) {
                // at 对象没绑定提示
                throw new TipsException("此玩家的令牌已过期，请提醒他重新授权。(!ymbind)。");
                //throw new TipsException(BindException.Type.BIND_Player_NoBind.message);
            } catch (WebClientResponseException.NotFound e) {
                throw new TipsException("找不到玩家 %s，请检查。", user.getOsuName());
            } catch (WebClientResponseException.Forbidden e) {
                throw new TipsException("%s 被办了。", user.getOsuName());
            } catch (WebClientResponseException e) {
                throw new TipsException("ppy API 状态异常！");
            } catch (Exception e) {
                log.error("HandleUtil：获取玩家信息失败！", e);
                throw new TipsException("HandleUtil：获取玩家信息失败！");
            }
        }

        String name;
        try {
            name = matcher.group("name");
            // 对叫100的人直接取消处理
            if (StringUtils.hasText(name) && name.length() > 2 && ! "100".equals(name.trim())) {

                try {
                    return userApiService.getPlayerInfo(name, mode);
                } catch (WebClientResponseException.NotFound e) {
                    throw new TipsException("找不到玩家 %s，请检查。", name);
                } catch (WebClientResponseException.Forbidden e) {
                    throw new TipsException("%s 被办了。", name);
                } catch (WebClientResponseException e) {
                    throw new TipsException("ppy API 状态异常！");
                } catch (Exception e) {
                    log.error("HandleUtil：获取玩家信息失败！", e);
                    throw new TipsException("HandleUtil：获取玩家信息失败！");
                }
            }
        } catch (IllegalArgumentException ignore) {
            // 没名字
        }

        // 没 at 没 qq= 也没名字 直接返回 null
        return null;
    }

    public static OsuUser getMyselfUser(@NonNull MessageEvent event, @Nullable OsuMode mode) throws TipsException {
        var qq = event.getSender().getId();
        var user = bindDao.getUserFromQQ(qq);

        try {
            if (OsuMode.isDefault(mode)) mode = user.getMode();
            return userApiService.getPlayerInfo(user, mode);

        } catch (WebClientResponseException.Unauthorized e) {
            throw new TipsException("您的令牌已过期，请重新授权。(!ymbind)。");
        } catch (WebClientResponseException.NotFound e) {
            throw new TipsException("找不到你的玩家信息，请检查。", user.getOsuName());
        } catch (WebClientResponseException.Forbidden e) {
            throw new TipsException("你被办了。", user.getOsuName());
        } catch (WebClientResponseException e) {
            throw new TipsException("ppy API 状态异常！");
        } catch (Exception e) {
            log.error("HandleUtil：获取自我信息失败！", e);
            throw new TipsException("HandleUtil：获取自我信息失败！");
        }
    }

    public static Map<Integer, Score> getOsuBPList(OsuUser user, Matcher matcher, @Nullable OsuMode mode) throws WebClientResponseException {
        return getOsuBPList(user, matcher, mode, false);
    }

    //isMultipleDefault20是给bs默认 20 用的，其他情况下 false 就可以
    public static Map<Integer, Score> getOsuBPList(OsuUser user, Matcher matcher, @Nullable OsuMode mode, boolean isMultipleDefault20) throws WebClientResponseException {
        int n;
        int m;
        try {
            var range = matcher.group("range");
            var rangeArray = range.split("-");
            if (rangeArray.length == 2) {
                n = Integer.parseInt(rangeArray[0]) - 1;
                m = Integer.parseInt(rangeArray[1]);
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
        } catch (NumberFormatException | NullPointerException e) {
            // 没有 range 默认是 1？
            // !bs = !BP 1 - 20，默认是 1 直接给我功能干废了！
            n = 0;
            m = isMultipleDefault20 ? 20 : 1;
        }

        var result = scoreApiService.getBestPerformance(user.getUID(), mode, n, m);
        var dataMap = new TreeMap<Integer, Score>();
        int finalN = n;
        result.forEach(ContextUtil.consumerWithIndex((s, index) -> dataMap.put(index + finalN, s)));
        return dataMap;
    }

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
        public CommandPatternBuilder appendCommand(Iterable<String> commands) {
            patternStr.append('(');
            for (var command : commands) {
                patternStr.append(command).append('|');
            }
            patternStr.deleteCharAt(patternStr.length() - 1);
            patternStr.append(')');
            return this;
        }

        /**
         * 加命令
         */
        public CommandPatternBuilder appendCommand(String... commands) {
            patternStr.append('(');
            for (var command : commands) {
                patternStr.append(command).append('|');
            }
            patternStr.deleteCharAt(patternStr.length() - 1);
            patternStr.append(')');
            return this;
        }

        /**
         * 避免不必要的命令重复，所带来的指令污染。比如：!ymb 和 !ymbind。如果前面并未加 (?!ind)，则后面的指令也会在前面被错误匹配（获取叫 ind 玩家的 bp）
         * @param ignores 需要忽略的其后字符。
         * @return this
         */
        public CommandPatternBuilder appendIgnore(@NonNull String... ignores) {
            patternStr.append("(?!(");
            for (var ignore : ignores) {
                patternStr.append(ignore).append('|');
            }
            patternStr.deleteCharAt(patternStr.length() - 1);
            patternStr.append("))");
            return this;
        }

        /**
         * 避免不必要的命令重复，所带来的指令污染。比如：!ymb 和 !ymbind。如果前面并未加 (?!ind)，则后面的指令也会在前面被错误匹配（获取叫 ind 玩家的 bp）
         * @param areas 注意，areas 可以是单个字符，也可以是连字符包含的区间。
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

        public CommandPatternBuilder appendSpace() {
            patternStr.append(REG_SPACE);
            return this;
        }

        public CommandPatternBuilder more() {
            patternStr.append('+');
            return this;
        }

        /**
         * 前面自己带冒号
         */
        public CommandPatternBuilder appendMode(boolean nullable) {
            startGroup();
            append(REG_SPACE_COLON);
            append(REG_MODE);
            endGroup();
            if (nullable) any();
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
        public CommandPatternBuilder appendKeyWord(String str, boolean include0) {
            patternStr.append("(?<").append(str).append('>').append(str).append(include0 ? ")?" : ")+");
            return this;
        }

        // 构建类似于 (?<s>s)? 这样匹配特定字符的序列，加空格
        public CommandPatternBuilder appendKeyWordWithSpace(String str, boolean include0) {
            return this.appendKeyWord(str, include0).append(REG_SPACE);
        }

        public CommandPatternBuilder appendWithSpace(String str) {
            patternStr.append(str).append(REG_SPACE);
            return this;
        }

        public CommandPatternBuilder any() {
            patternStr.append('?');
            return this;
        }

        public CommandPatternBuilder appendQQ(boolean nullable) {
            append(REG_QQ);
            if (nullable) any();
            return this;
        }

        public CommandPatternBuilder appendName(boolean nullable) {
            append(REG_NAME);
            if (nullable) any();
            return this;
        }

        public CommandPatternBuilder appendRange(boolean nullable) {
            startGroup();
            append("\\s+");
            append(REG_SPACE_HASH).any().appendSpace();
            append(REG_RANGE);
            endGroup();
            if (nullable) any();
            return this;
        }

        public CommandPatternBuilder end() {
            append("$");
            return this;
        }

        public Pattern build() {
            return Pattern.compile(patternStr.toString());
        }

    }
}

package com.now.nowbot.util;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.LogException;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.TipsException;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 封装一些消息处理（Handle）的常用方法
public class HandleUtil {
    private static final String REG_START       = "([!！/])";
    private static final String REG_SPACE       = "(\\s*)";
    private static final String REG_SPACE_COLON = "([:：])";
    private static final String REG_SPACE_HASH  = "(#)";
    private static final String REG_SPACE_HYPEN = "(-)";
    private static final String REG_NAME        = "(?<name>[0-9a-zA-Z\\[\\]-_ ]{3,})";
    private static final String REG_QQ          = "(qq=(?<qq>\\d{5,}))";
    private static final String REG_MOD         = "(?<mod>(EZ|NF|HT|HR|SD|PF|DT|NC|HD|FI|FL|SO|[1-9]K|CP|MR|RD|TD)+)";
    private static final String REG_MODE        = "(?<mode>osu|taiko|ctb|mania|std|0|1|2|3|o|m|c|f|t)";
    private static final String REG_RANGE       = "(?<range>\\d{1,2}(-\\d{1,2})?)";
    private static final String REG_ID          = "(?<id>\\d+)";
    private static final String REG_BID         = "(?<bid>\\d+)";
    private static final String REG_SID         = "(?<sid>\\d+)";
    private static BindDao            bindDao;
    private static OsuUserApiService  userApiService;
    private static OsuScoreApiService scoreApiService;

    public static void init(ApplicationContext applicationContext) {
        bindDao = applicationContext.getBean(BindDao.class);
        userApiService = applicationContext.getBean(OsuUserApiService.class);
        scoreApiService = applicationContext.getBean(OsuScoreApiService.class);
    }

    /**
     * 获取一个 user, 优先获取别人, 没找到就自己
     */
    public static OsuUser getUser(MessageEvent event, Matcher matcher) {
        OsuMode mode = getMode(matcher);

        var u = getOtherUser(event, matcher, mode);

        if (Objects.nonNull(u)) return u;

        return getSelfUser(event, mode);
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
    public static OsuUser getOtherUser(MessageEvent event, Matcher matcher, @Nullable OsuMode mode) {
        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);

        long qq = 0;

        try {
            var qqStr = matcher.group("qq");
            if (StringUtils.hasText(qqStr)) {
                qq = Long.parseLong(qqStr);
            } else if (Objects.nonNull(at)) {
                qq = at.getTarget();
            }
        } catch (Exception ignore) {
            // 没 @ 也没 qq=
        }

        if (qq != 0) {
            BinUser user;
            try {
                user = bindDao.getUserFromQQ(qq);
                if (OsuMode.isDefault(mode)) mode = user.getMode();
            } catch (BindException e) {
                // at 对象没绑定提示
                throw new BindException(BindException.Type.BIND_Player_NoBind);
            }
            return userApiService.getPlayerInfo(user, mode);
        }

        String name;
        try {
            name = matcher.group("name");
            if (StringUtils.hasText(name)) {
                return userApiService.getPlayerInfo(name, mode);
            }
        } catch (IllegalArgumentException ignore) {
            // 没名字
        }

        // 没 at 没 qq= 也没名字 直接返回 null
        return null;
    }

    public static OsuUser getSelfUser(MessageEvent event, @Nullable OsuMode mode) {
        var qq = event.getSender().getId();
        var user = bindDao.getUserFromQQ(qq);
        if (OsuMode.isDefault(mode)) mode = user.getMode();
        return userApiService.getPlayerInfo(user, mode);
    }

    public static List<Score> getOsuBPList(OsuUser user, Matcher matcher, @Nullable OsuMode mode) {
        int n = 0;
        int m;
        try {
            var range = matcher.group("range");
            var rangeArray = range.split("-");
            if (rangeArray.length == 2) {
                n = Integer.parseInt(rangeArray[0]) - 1;
                m = Integer.parseInt(rangeArray[1]) - 1;
            } else {
                m = Integer.parseInt(rangeArray[0]) - 1;
            }

            // 处理 m n 的极值
            if (n < m) {
                n = n + m;
                m = n - m;
                n = n - m;
            } else if (n == m) {
                m = n + 1;
            }

            if (n < 0) n = 0;
            m = m - n;
            if (m < 1) m = 1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return scoreApiService.getBestPerformance(user.getUID(), mode, n, m);
    }

    /**
     * 把从消息段拿到的正则组匹配成 List<Score> 对象。
     * 这个方法是一步到位，不返回中间生成的 osuUser。然而大多数使用场景会用到 osuUser，那就请使用另一个重载方法，先拿 osuUser 再拿 List<Score>。
     *
     * @param event           消息事件
     * @param name            名字字符串
     * @param qqStr           qq字符串
     * @param bindDao         绑定中间层的实例
     * @param userApiService  获取 osuUser 的实例
     * @param scoreApiService 获取 Score 的实例
     * @return List<Score>
     * @throws TipsException 提示
     */
    @NonNull
    public static List<Score> getOsuBPFromMessageText(@NonNull MessageEvent event, String name, String qqStr, String mode, BindDao bindDao, OsuUserApiService userApiService, OsuScoreApiService scoreApiService) throws TipsException {
        Pattern.compile("(?<mode>osu|taiko|ctb|mania|std|0|1|2|3|o|m|c|f|t)");
        var user = getOsuUserFromMessageText(event, name, qqStr, mode, bindDao, userApiService);
        return getOsuBPFromMessageText(user, mode, scoreApiService);
    }

    /*****************************************************************************/

    /**
     * 把从消息段拿到的正则组匹配成 osuUser 对象
     *
     * @param event          消息事件
     * @param name           名字字符串
     * @param qqStr          qq字符串
     * @param bindDao        绑定中间层的实例
     * @param userApiService 获取 osuUser 的实例
     * @return osuUser 对象
     * @throws TipsException 提示
     */
    @NonNull
    public static OsuUser getOsuUserFromMessageText(@NonNull MessageEvent event, String name, String qqStr, String mode, BindDao bindDao, OsuUserApiService userApiService) throws TipsException {
        var myQQ = event.getSender().getId();

        var nameMatcher = Pattern.compile("(?<name>[\\w\\s\\-\\[\\]]{3,})\\s*\\|")
                .matcher(Objects.requireNonNullElse(event.getSender().getName(), ""));

        var myName = nameMatcher.find() ? nameMatcher.group("name") : null;
        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);

        BinUser binUser;

        if (Objects.nonNull(at)) {
            binUser = bindDao.getUserFromQQ(at.getTarget());
        } else if (StringUtils.hasText(name)) {
            binUser = new BinUser();
            binUser.setOsuName(name.trim());
        } else if (StringUtils.hasText(qqStr)) {
            long qq;

            try {
                qq = Long.parseLong(qqStr);
            } catch (NumberFormatException e) {
                throw new TipsException("请输入正确的 qq！");
            }

            binUser = bindDao.getUserFromQQ(qq);
        } else {
            try {
                binUser = bindDao.getUserFromQQ(myQQ);
            } catch (BindException e) {
                if (StringUtils.hasText(myName)) {
                    binUser = new BinUser();
                    binUser.setOsuName(myName.trim());
                } else {
                    throw new TipsException("您的令牌已过期，请重新授权。(!ymbind)");
                    // throw new BindException(BindException.Type.BIND_Me_TokenExpired);
                }
            }
        }

        var m = (OsuMode.getMode(mode) == OsuMode.DEFAULT) ? binUser.getMode() : OsuMode.getMode(mode);

        try {
            return userApiService.getPlayerInfo(binUser.getOsuName(), m);
        } catch (WebClientResponseException.Forbidden e) {
            throw new TipsException("该玩家被 ban 了。");
        } catch (WebClientResponseException.NotFound e) {
            throw new TipsException("找不到此玩家的 osu 信息！");
        } catch (WebClientResponseException e) {
            throw new TipsException("ppy API 连接失败！");
        } catch (Exception e) {
            throw new LogException("MessageUtil：获取 osu 信息失败！", e);
        }
    }

    /**
     * 把从消息段拿到的正则组匹配成 List<Score> 对象。
     * 封装一层的原因是，这样可以统一发送报错信息，节省部分 TipsException 子类的重复
     *
     * @param user            OsuUser 对象
     * @param scoreApiService 获取 Score 的实例
     * @return List<Score>
     * @throws TipsException 提示
     */
    @NonNull
    public static List<Score> getOsuBPFromMessageText(@NonNull OsuUser user, String mode, int offset, int limit, OsuScoreApiService scoreApiService) throws TipsException {
        var m = (OsuMode.getMode(mode) == OsuMode.DEFAULT) ? user.getOsuMode() : OsuMode.getMode(mode);

        try {
            return scoreApiService.getBestPerformance(user.getId(), m, offset, limit);
        } catch (WebClientResponseException.NotFound e) {
            throw new TipsException("找不到此玩家的最好成绩！");
        } catch (WebClientResponseException e) {
            throw new TipsException("ppy API 连接失败！");
        } catch (Exception e) {
            throw new TipsException("MessageUtil：获取最好成绩失败！");
        }
    }

    @NonNull
    public static List<Score> getOsuBPFromMessageText(@NonNull OsuUser user, String mode, OsuScoreApiService scoreApiService) throws TipsException {
        return getOsuBPFromMessageText(user, mode, 0, 100, scoreApiService);
    }

    public static class CommandPatternBuilder {
        StringBuilder patternStr = new StringBuilder();

        public CommandPatternBuilder() {
            patternStr.append('^').append(REG_START).append(REG_SPACE);
        }

        /**
         * 一些特殊的指令开始 比如 #calc 等
         *
         * @param start
         */
        public CommandPatternBuilder(String start) {
            patternStr.append('^').append(start).append(REG_SPACE);
        }

        /**
         * 加命令
         */
        public CommandPatternBuilder addCommand(Iterable<String> commands) {
            patternStr.append('(');
            for (var command : commands) {
                patternStr.append(command).append('|');
            }
            patternStr.deleteCharAt(patternStr.length() - 1);
            patternStr.append(')').append(REG_SPACE);
            return this;
        }

        /**
         * 加命令
         */
        public CommandPatternBuilder addCommand(String... commands) {
            patternStr.append('(');
            for (var command : commands) {
                patternStr.append(command).append('|');
            }
            patternStr.deleteCharAt(patternStr.length() - 1);
            patternStr.append(')');
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

        public CommandPatternBuilder appendWithSpace(String str) {
            patternStr.append(str).append(REG_SPACE);
            return this;
        }

        public Pattern build() {
            return Pattern.compile(patternStr.toString());
        }

    }
}

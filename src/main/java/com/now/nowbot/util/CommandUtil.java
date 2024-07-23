package com.now.nowbot.util;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.command.CmdObject;
import com.now.nowbot.util.command.CmdPatternStatic;
import com.now.nowbot.util.command.CmdRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.now.nowbot.util.command.CmdPatternStatic.*;

public class CommandUtil {


    public static OsuUser getUserWithOutRange(
            MessageEvent event,
            Matcher matcher,
            CmdObject<OsuMode> mode,
            AtomicBoolean isMyself
    ) throws TipsException {
        isMyself.set(false);
        var userObj = getOsuUser(event, matcher, mode);
        if (Objects.isNull(userObj)) {
            isMyself.set(true);
            var bind = bindDao.getUserFromQQ(event.getSender().getId());
            checkOsuMode(mode, bind.getOsuMode());
            userObj = userApiService.getPlayerOsuInfo(bind);
        }
        return userObj;
    }

    public static CmdRange<OsuUser> getUserWithRange(
            MessageEvent event,
            Matcher matcher,
            CmdObject<OsuMode> mode,
            AtomicBoolean isMyself
    ) throws TipsException {
        isMyself.set(false);
        var range = getUserAndRange(matcher, mode);
        if (Objects.isNull(range.getData())) {
            range.setData(getUserWithOutRange(event, matcher, mode, isMyself));
        }
        return range;
    }

    private static final Pattern JUST_RANGE = Pattern.compile("^(\\d{1,2}[\\-－ ])?\\d{1,3}$");

    private static CmdRange<OsuUser> getUserAndRange(Matcher matcher, CmdObject<OsuMode> mode) {

        if (!matcher.namedGroups().containsKey(FLAG_USER_AND_RANGE))
            throw new IllegalArgumentException("Matcher 中不包含 ur 分组");
        if (Objects.isNull(mode.getData())) {
            mode.setData(OsuMode.DEFAULT);
        }

        var text = matcher.group(FLAG_USER_AND_RANGE);

        if (JUST_RANGE.matcher(text).matches()) {
            var range = $parseRange(text);
            return new CmdRange<>(null, range[0], range[1]);
        }
        LinkedList<CmdRange<String>> ranges;
        if (text.charAt(CHAR_HASH) > 0 || text.charAt(CHAR_HASH_FULL) > 0) {
            ranges = $parseNameAndRangeHasHash(text);
        } else {
            ranges = $parseNameAndRangeWithoutHash(text);
        }

        CmdRange<OsuUser> result = new CmdRange<>(null, null, null);
        for (var range : ranges) {
            try {
                long id = userApiService.getOsuId(range.getData());
                OsuUser user = getOsuUser(id, mode.getData());
                result = new CmdRange<>(user, range.getStart(), range.getEnd());
                break;
            } catch (Exception ignore) {
            }
        }

        // 使其顺序
        if (Objects.nonNull(result.getEnd()) &&
                Objects.nonNull(result.getStart()) &&
                result.getStart() > result.getEnd()) {
            int temp = result.getStart();
            result.setStart(result.getEnd());
            result.setEnd(temp);
        }
        return result;
    }

    private static LinkedList<CmdRange<String>> $parseNameAndRangeHasHash(String text) {
        var ranges = new LinkedList<CmdRange<String>>();
        int hashIndex = text.indexOf(CHAR_HASH);
        if (hashIndex < 0) hashIndex = text.indexOf(CHAR_HASH_FULL);
        String nameStr = text.substring(0, hashIndex).trim();
        if (!StringUtils.hasText(nameStr)) nameStr = null;
        String rangeStr = text.substring(hashIndex + 1).trim();
        var rangeInt = $parseRange(rangeStr);
        ranges.add(new CmdRange<>(
                nameStr, rangeInt[0], rangeInt[1]
        ));
        return ranges;
    }

    private static LinkedList<CmdRange<String>> $parseNameAndRangeWithoutHash(String text) {
        var ranges = new LinkedList<CmdRange<String>>();
        CmdRange<String> tempRange = new CmdRange<>(text, null, null);
        // 保底 只有名字
        ranges.push(tempRange);
        int index = text.length() - 1;
        int rangeN;
        int i = 0;
        char tempChar;
        // 第一个 range
        while (isNumber(tempChar = text.charAt(index))) {
            index--;
            i++;
        }

        // 对于 末尾无数字 / 数字大于3位 / 实际名称小于最小值 认为无 range
        if (i <= 0 || i > 3 || index < OSU_MIN_INDEX) {
            return ranges;
        }
        rangeN = Integer.parseInt(text.substring(index + 1));
        tempRange = new CmdRange<>(
                text.substring(0, index + 1).trim(),
                rangeN,
                null
        );
        ranges.push(tempRange);
        if (tempChar != '-' && tempChar != '－' && tempChar != ' ') {
            // 对应末尾不是 - 或者 空格, 直接忽略剩余 range
            // 优先认为紧贴的数字是名字的一部分, 也就是目前结果集的第一个
            tempRange = ranges.pollLast();
            ranges.push(tempRange);
            return ranges;
        }

        do {
            index--;
        } while (text.charAt(index) == ' ');

        // 第二组数字
        i = 0;

        while (isNumber(tempChar = text.charAt(index))) {
            index--;
            i++;
        }

        if (i <= 0 || i > 3 || index < OSU_MIN_INDEX) {
            // 与上面同理
            return ranges;
        }

        tempRange = new CmdRange<>(
                text.substring(0, index + 1),
                rangeN,
                Integer.parseInt(text.substring(index + 1, index + i + 1))
        );

        if (tempChar != ' ') {
            // 优先认为紧贴的数字是名字的一部分, 交换位置
            var ttmp = ranges.poll();
            ranges.push(tempRange);
            ranges.push(ttmp);
        } else {
            ranges.push(tempRange);
        }

        return ranges;
    }

    private static Integer[] $parseRange(String text) {
        Integer[] rangeInt = new Integer[]{null, null};

        try {
            String[] range = text.split("[\\-－ ]");
            if (range.length >= 2) {
                rangeInt[0] = Integer.parseInt(range[range.length - 2]);
                rangeInt[1] = Integer.parseInt(range[range.length - 1]);
            } else if (range.length == 1) {
                rangeInt[0] = Integer.parseInt(range[0]);
            }
        } catch (Exception e) {
            log.debug("range 解析参数有误: {}", text, e);
        }

        return rangeInt;
    }

    private static boolean isNumber(char c) {
        return c >= '0' && c <= '9';
    }

    private static OsuUser getOsuUser(MessageEvent event, Matcher matcher, CmdObject<OsuMode> mode) throws TipsException {
        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);

        long qq = 0;
        if (Objects.nonNull(at)) {
            qq = at.getTarget();
        } else if (matcher.namedGroups().containsKey(FLAG_QQ)) {
            try {
                qq = Long.parseLong(matcher.group(FLAG_QQ));
            } catch (RuntimeException ignore) {
            }
        }

        if (qq != 0) {
            var bind = bindDao.getUserFromQQ(qq);
            return getOsuUser(bind, checkOsuMode(mode, bind.getOsuMode()));
        }

        long uid = 0;
        if (matcher.namedGroups().containsKey(FLAG_UID)) {
            try {
                uid = Long.parseLong(matcher.group(FLAG_UID));
            } catch (RuntimeException ignore) {
            }
            if (uid != 0) return getOsuUser(uid, mode.getData());
        }

        if (matcher.namedGroups().containsKey(FLAG_NAME)) {
            var name = matcher.group(FLAG_NAME);
            if (!StringUtils.hasText(name)) return getOsuUser(name, mode.getData());
        }
        return null;
    }

    public static OsuUser getOsuUser(BinUser user, OsuMode mode) throws TipsException {
        return getOsuUser(() -> userApiService.getPlayerInfo(user, mode), user.getOsuID());
    }

    public static OsuUser getOsuUser(String name, OsuMode mode) throws TipsException {
        return getOsuUser(() -> userApiService.getPlayerInfo(name, mode), name);
    }

    public static OsuUser getOsuUser(long uid, OsuMode mode) throws TipsException {
        return getOsuUser(() -> userApiService.getPlayerInfo(uid, mode), uid);
    }

    private static <T> T getOsuUser(Supplier<T> consumer, Object tips) throws TipsException {
        try {
            return consumer.get();
        } catch (WebClientResponseException.NotFound e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Null_Player, tips.toString());
        } catch (WebClientResponseException.Forbidden e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Banned_Player, tips.toString());
        } catch (WebClientResponseException e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI);
        } catch (Exception e) {
            log.error("HandleUtil：获取玩家信息失败！", e);
            throw new TipsException("获取玩家信息失败！");
        }
    }

    public static CmdObject<OsuMode> getMode(Matcher matcher) {
        var result = new CmdObject<>(OsuMode.DEFAULT);
        if (matcher.namedGroups().containsKey(FLAG_MODE)) {
            result.setData(OsuMode.getMode(matcher.group(FLAG_MODE)));
        }
        return result;
    }

    private static OsuMode checkOsuMode(CmdObject<OsuMode> mode, OsuMode other) {
        if (OsuMode.isDefaultOrNull(mode.getData()) && !OsuMode.isDefaultOrNull(other)) {
            mode.setData(other);
        }
        return mode.getData();
    }

    private static final int OSU_MIN_INDEX = 2;

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

    public static CmdPatternStatic createPatternBuilder() {
        return new CmdPatternStatic();
    }
}

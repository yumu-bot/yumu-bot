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
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.TipsException;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

// 封装一些消息处理（Handle）的常用方法
public class HandleUtil {

    /**
     * 把从消息段拿到的正则组匹配成 List<Score> 对象。
     * 这个方法是一步到位，不返回中间生成的 osuUser。然而大多数使用场景会用到 osuUser，那就请使用另一个重载方法，先拿 osuUser 再拿 List<Score>。
     * @param event 消息事件
     * @param name 名字字符串
     * @param qqStr qq字符串
     * @param bindDao 绑定中间层的实例
     * @param userApiService 获取 osuUser 的实例
     * @param scoreApiService 获取 Score 的实例
     * @return List<Score>
     * @throws TipsException 提示
     */
    @NonNull
    public static List<Score> getOsuBPFromMessageText(@NonNull MessageEvent event, String name, String qqStr, String mode, BindDao bindDao, OsuUserApiService userApiService, OsuScoreApiService scoreApiService) throws TipsException {
        var user = getOsuUserFromMessageText(event, name, qqStr, mode, bindDao, userApiService);
        return getOsuBPFromMessageText(user, mode, scoreApiService);
    }

    /**
     * 把从消息段拿到的正则组匹配成 List<Score> 对象。
     * 封装一层的原因是，这样可以统一发送报错信息，节省部分 TipsException 子类的重复
     * @param user OsuUser 对象
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

    /**
     * 把从消息段拿到的正则组匹配成 osuUser 对象
     * @param event 消息事件
     * @param name 名字字符串
     * @param qqStr qq字符串
     * @param bindDao 绑定中间层的实例
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
            throw new TipsException("MessageUtil：获取 osu 信息失败！");
        }

    }
}

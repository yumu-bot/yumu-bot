package com.now.nowbot.service.OsuApiService;

import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.ActivityEvent;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.enums.OsuMode;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collection;
import java.util.List;

public interface OsuUserApiService {
    /**
     * 拼合授权链接
     *
     * @param state QQ[+群号]
     * @return
     */
    String getOauthUrl(String state) throws WebClientResponseException;

    String refreshUserToken(BinUser user) throws WebClientResponseException;

    String refreshUserTokenFirst(BinUser user) throws WebClientResponseException;

    OsuUser getPlayerInfo(BinUser user, OsuMode mode) throws WebClientResponseException;

    OsuUser getPlayerInfo(String userName, OsuMode mode) throws WebClientResponseException;

    OsuUser getPlayerInfo(Long id, OsuMode mode) throws WebClientResponseException;

    default OsuUser getPlayerInfo(BinUser user) throws WebClientResponseException {
        return getPlayerInfo(user, OsuMode.DEFAULT);
    }

    default OsuUser getPlayerInfo(String userName) throws WebClientResponseException {
        return getPlayerInfo(userName, OsuMode.DEFAULT);
    }

    default OsuUser getPlayerInfo(Long userId) throws WebClientResponseException {
        return getPlayerInfo(userId, OsuMode.DEFAULT);
    }

    default OsuUser getPlayerOsuInfo(BinUser user) throws WebClientResponseException {
        return getPlayerInfo(user, OsuMode.OSU);
    }

    default OsuUser getPlayerTaikoInfo(BinUser user) throws WebClientResponseException {
        return getPlayerInfo(user, OsuMode.TAIKO);
    }

    default OsuUser getPlayerCatchInfo(BinUser user) throws WebClientResponseException {
        return getPlayerInfo(user, OsuMode.CATCH);
    }

    default OsuUser getPlayerManiaInfo(BinUser user) throws WebClientResponseException {
        return getPlayerInfo(user, OsuMode.MANIA);
    }

    default OsuUser getPlayerOsuInfo(Long userId) throws WebClientResponseException {
        return getPlayerInfo(userId, OsuMode.OSU);
    }

    default OsuUser getPlayerTaikoInfo(Long userId) throws WebClientResponseException {
        return getPlayerInfo(userId, OsuMode.TAIKO);
    }

    default OsuUser getPlayerCatchInfo(Long userId) throws WebClientResponseException {
        return getPlayerInfo(userId, OsuMode.CATCH);
    }

    default OsuUser getPlayerManiaInfo(Long userId) throws WebClientResponseException {
        return getPlayerInfo(userId, OsuMode.MANIA);
    }

    Long getOsuId(String name);

    /**
     * 批量获取用户信息
     *
     * @param users 注意, 单次请求数量必须小于50
     */
    <T extends Number> List<MicroUser> getUsers(Collection<T> users) throws WebClientResponseException;

    List<MicroUser> getFriendList(BinUser user) throws WebClientResponseException;

    List<ActivityEvent> getUserRecentActivity(long userId, int s, int e);
}

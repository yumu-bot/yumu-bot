package com.now.nowbot.service.osuApiService;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.json.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collection;
import java.util.List;

public interface OsuUserApiService {
    boolean isPlayerExist(String name);

    /**
     * 拼合授权链接
     *
     * @param state QQ[+群号]
     */
    default String getOauthUrl(String state) throws WebClientResponseException {
        return getOauthUrl(state, false);
    }

    String getOauthUrl(String state, boolean full) throws WebClientResponseException;

    @CanIgnoreReturnValue
    String refreshUserToken(BinUser user) throws WebClientResponseException;

    void refreshUserTokenFirst(BinUser user) throws WebClientResponseException;

    OsuUser getPlayerInfo(BinUser user, OsuMode mode) throws WebClientResponseException;

    OsuUser getPlayerInfo(String userName, OsuMode mode) throws WebClientResponseException;

    OsuUser getPlayerInfo(Long id, OsuMode mode) throws WebClientResponseException;

    default OsuUser getPlayerInfo(BinUser user) throws WebClientResponseException {
        return getPlayerInfo(user, OsuMode.DEFAULT);
    }

    default OsuUser getPlayerInfo(String userName) throws WebClientResponseException {
        return getPlayerInfo(userName, OsuMode.DEFAULT);
    }

    default OsuUser getPlayerInfo(Long UID) throws WebClientResponseException {
        return getPlayerInfo(UID, OsuMode.DEFAULT);
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

    default OsuUser getPlayerOsuInfo(Long UID) throws WebClientResponseException {
        return getPlayerInfo(UID, OsuMode.OSU);
    }

    default OsuUser getPlayerTaikoInfo(Long UID) throws WebClientResponseException {
        return getPlayerInfo(UID, OsuMode.TAIKO);
    }

    default OsuUser getPlayerCatchInfo(Long UID) throws WebClientResponseException {
        return getPlayerInfo(UID, OsuMode.CATCH);
    }

    default OsuUser getPlayerManiaInfo(Long UID) throws WebClientResponseException {
        return getPlayerInfo(UID, OsuMode.MANIA);
    }

    Long getOsuId(String name);

    /**
     * 批量获取用户信息
     *
     * @param users 注意, 单次请求数量必须小于50
     */
    <T extends Number> List<MicroUser> getUsers(Collection<T> users) throws WebClientResponseException;

    List<LazerFriend> getFriendList(BinUser user) throws WebClientResponseException;

    List<ActivityEvent> getUserRecentActivity(long UID, int s, int e);

    KudosuHistory getUserKudosu(BinUser user);

    JsonNode sendPrivateMessage(BinUser sender, Long target, String message);

    JsonNode acknowledgmentPrivateMessageAlive(BinUser user, Long since);

    default JsonNode acknowledgmentPrivateMessageAlive(BinUser user) {
        return acknowledgmentPrivateMessageAlive(user, null);
    }

    JsonNode getPrivateMessage(BinUser sender, Long channel, Long since);
}

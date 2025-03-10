package com.now.nowbot.service.osuApiService;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.now.nowbot.model.BindUser;
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
    String refreshUserToken(BindUser user) throws WebClientResponseException;

    void refreshUserTokenFirst(BindUser user) throws WebClientResponseException;

    OsuUser getPlayerInfo(BindUser user, OsuMode mode) throws WebClientResponseException;

    OsuUser getPlayerInfo(String name, OsuMode mode) throws WebClientResponseException;

    OsuUser getPlayerInfo(Long id, OsuMode mode) throws WebClientResponseException;

    default OsuUser getPlayerInfo(BindUser user) throws WebClientResponseException {
        return getPlayerInfo(user, user.getOsuMode());
    }

    default OsuUser getPlayerInfo(String name) throws WebClientResponseException {
        return getPlayerInfo(name, OsuMode.DEFAULT);
    }

    default OsuUser getPlayerInfo(Long UID) throws WebClientResponseException {
        return getPlayerInfo(UID, OsuMode.DEFAULT);
    }

    Long getOsuId(String name);

    /**
     * 批量获取用户信息
     *
     * @param users 注意, 单次请求数量必须小于50
     */
    <T extends Number> List<MicroUser> getUsers(Collection<T> users, Boolean isVariant) throws WebClientResponseException;


    /**
     * 批量获取用户信息
     *
     * @param users 注意, 单次请求数量必须小于50
     */
    default <T extends Number> List<MicroUser> getUsers(Collection<T> users) {
        return getUsers(users, false);
    }

    List<LazerFriend> getFriendList(BindUser user) throws WebClientResponseException;

    List<ActivityEvent> getUserRecentActivity(long UID, int s, int e);

    KudosuHistory getUserKudosu(BindUser user);

    JsonNode sendPrivateMessage(BindUser sender, Long target, String message);

    JsonNode acknowledgmentPrivateMessageAlive(BindUser user, Long since);

    default JsonNode acknowledgmentPrivateMessageAlive(BindUser user) {
        return acknowledgmentPrivateMessageAlive(user, null);
    }

    JsonNode getPrivateMessage(BindUser sender, Long channel, Long since);

    TeamInfo getTeamInfo(int id);

    record TeamInfo(
            Integer id,
            String name,
            String abbr,
            String formed,

            String banner,
            String flag,

            List<OsuUser> users,
            OsuMode ruleset,
            String application,

            Integer rank,
            Integer pp,
            @JsonProperty("ranked_score") Long rankedScore,
            @JsonProperty("play_count") Long playCount,
            Integer members,

            String description
    ) {
    }
}
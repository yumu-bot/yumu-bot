package com.now.nowbot.service.osuApiService.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.dao.OsuUserInfoDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.json.ActivityEvent;
import com.now.nowbot.model.json.KudosuHistory;
import com.now.nowbot.model.json.MicroUser;
import com.now.nowbot.model.json.OsuUser;
import com.now.nowbot.service.osuApiService.OsuUserApiService;
import com.now.nowbot.throwable.TipsRuntimeException;
import com.now.nowbot.util.JacksonUtil;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
public class UserApiImpl implements OsuUserApiService {
    private static final Logger log = LoggerFactory.getLogger(UserApiImpl.class);
    OsuApiBaseService base;
    BindDao           bindDao;
    OsuUserInfoDao    userInfoDao;

    public UserApiImpl(
            OsuApiBaseService osuApiBaseService,
            BindDao bind,
            OsuUserInfoDao info
    ) {
        base = osuApiBaseService;
        bindDao = bind;
        userInfoDao = info;
    }

    // 用来确认玩家是否存在于服务器，而无需使用 API 请求。
    @Override
    public boolean isPlayerExist(String name) {
        var response = base.osuApiWebClient.get()
                .uri("https://osu.ppy.sh/users/{name}", name)
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorReturn("")
                .block();

        return StringUtils.isNotEmpty(response);
    }

    @Override
    public String getOauthUrl(String state, boolean full) {
        return UriComponentsBuilder.fromHttpUrl("https://osu.ppy.sh/oauth/authorize")
                .queryParam("client_id", base.oauthId)
                .queryParam("redirect_uri", base.redirectUrl)
                .queryParam("response_type", "code")
                .queryParam("scope", full
                        ? "chat.read chat.write chat.write_manage forum.write friends.read identify public"
                        : "friends.read identify public")
                .queryParam("state", state)
                .build().encode().toUriString();
    }

    @Override
    public String refreshUserToken(BinUser user) {
        if (! user.isAuthorized()) return base.getBotToken();
        return base.refreshUserToken(user, false);
    }

    @Override
    public void refreshUserTokenFirst(BinUser user) {
        base.refreshUserToken(user, true);
        var osuInfo = getPlayerInfo(user);
        var uid = osuInfo.getUserID();
        user.setOsuID(uid);
        user.setOsuName(user.getOsuName());
        user.setOsuMode(user.getOsuMode());
    }

    @Override
    public OsuUser getPlayerInfo(BinUser user, OsuMode mode) {
        if (! user.isAuthorized()) return getPlayerInfo(user.getOsuID(), mode);
        return base.osuApiWebClient.get()
                .uri("me/{mode}", mode.getName())
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToMono(OsuUser.class)
                .map((data) -> {
                    userInfoDao.saveUser(data, mode);
                    user.setOsuID(data.getUserID());
                    user.setOsuName(data.getUsername());
                    user.setOsuMode(data.getCurrentOsuMode());
                    return data;
                }).block();
    }

    @Override
    public OsuUser getPlayerInfo(String userName, OsuMode mode) {
        return base.osuApiWebClient.get()
                .uri(l -> l
                        .path("users/{data}/{mode}")
                        .build('@'+userName, mode.getName())
                )
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(OsuUser.class)
                .map((data) -> {
                    userInfoDao.saveUser(data, mode);
                    return data;
                }).block();
    }

    @Override
    public OsuUser getPlayerInfo(Long id, OsuMode mode) {
        return base.osuApiWebClient.get()
                .uri(l -> l
                        .path("users/{id}/{mode}")
                        .build(id, mode.getName()))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(OsuUser.class)
                .map((data) -> {
                    userInfoDao.saveUser(data, mode);
                    return data;
                }).block();
    }

    @Override
    public Long getOsuId(String name) {
        Long id = bindDao.getOsuId(name);
        if (id != null) {
            return id;

        }
        var osuUser = getPlayerInfo(name);
        bindDao.removeOsuNameToId(osuUser.getUserID());
        String[] nameStrs = new String[osuUser.getPreviousNames().size() + 1];
        int i = 0;
        nameStrs[i++] = osuUser.getUsername().toUpperCase();
        for (var n : osuUser.getPreviousNames()) {
            nameStrs[i++] = n.toUpperCase();
        }
        bindDao.saveOsuNameToId(osuUser.getUserID(), nameStrs);
        return osuUser.getUserID();
    }

    /**
     * 批量获取用户信息
     *
     * @param users 注意, 单次请求数量必须小于50
     */
    @Override
    public <T extends Number> List<MicroUser> getUsers(Collection<T> users) {
        return base.osuApiWebClient.get()
                .uri(b -> b.path("users")
                        .queryParam("ids[]", users)
                        .build())
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(result -> {
                    List<MicroUser> userList = JacksonUtil.parseObjectList(result.get("users"), MicroUser.class);
                    userInfoDao.saveUsers(userList);
                    return userList;
                })
                .block();
    }

    @Override
    public List<MicroUser> getFriendList(BinUser user) {
        if (! user.isAuthorized()) throw new TipsRuntimeException("无权限");
        return base.osuApiWebClient.get()
                .uri("friends")
                .headers(base.insertHeader(user))
                .retrieve().bodyToFlux(MicroUser.class)
                .collectList()
                .block();
    }

    @Override
    public List<ActivityEvent> getUserRecentActivity(long userId, int s, int e) {
        return base.osuApiWebClient.get()
                .uri(b -> b.path("users/{userId}/recent_activity")
                        .queryParam("offset", s)
                        .queryParam("limit", e)
                        .build(userId))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToFlux(ActivityEvent.class)
                .collectList()
                .block();
    }

    @Override
    public KudosuHistory getUserKudosu(BinUser user) {
        return base.osuApiWebClient.get()
                .uri("users/{uid}/kudosu")
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToMono(KudosuHistory.class)
                .block();
    }

    @Override
    public JsonNode sendPrivateMessage(BinUser sender, Long target, String message) {
        var body = Map.of("target_id", target, "message", message, "is_action", false);
        return base.osuApiWebClient.post()
                .uri("chat/new")
                .headers(base.insertHeader(sender))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    @Override
    public JsonNode acknowledgmentPrivateMessageAlive(BinUser user, Long since) {
        return base.osuApiWebClient.post()
                .uri(b -> b.path("chat/ack")
                        .queryParamIfPresent("since", Optional.ofNullable(since))
                        .build())
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    @Override
    public JsonNode getPrivateMessage(BinUser sender, Long channel, Long since) {
        return base.osuApiWebClient.get()
                .uri("chat/channels/{channel}/messages?since={since}", channel, since)
                .headers(base.insertHeader(sender))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }
}

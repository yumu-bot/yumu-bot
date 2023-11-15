package com.now.nowbot.service.OsuApiService.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.dao.OsuUserInfoDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.TipsRuntimeException;
import com.now.nowbot.util.JacksonUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.List;

@Service
public class UserApiImpl implements OsuUserApiService {
    OsuApiBaseService base;
    BindDao bindDao;
    OsuUserInfoDao userInfoDao;

    public UserApiImpl(
            OsuApiBaseService osuApiBaseService,
            BindDao bind,
            OsuUserInfoDao info
    ) {
        base = osuApiBaseService;
        bindDao = bind;
        userInfoDao = info;
    }

    @Override
    public String getOauthUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl("https://osu.ppy.sh/oauth/authorize")
                .queryParam("client_id", base.oauthId)
                .queryParam("redirect_uri", base.redirectUrl)
                .queryParam("response_type", "code")
                .queryParam("scope", "friends.read identify public")
                .queryParam("state", state)
                .build().encode().toUriString();
    }

    @Override
    public String refreshUserToken(BinUser user) {
        return base.refreshUserToken(user, false);
    }

    @Override
    public String refreshUserTokenFirst(BinUser user) {
        String access = base.refreshUserToken(user, true);
        getPlayerInfo(user);
        bindDao.saveUser(user);
        return access;
    }

    @Override
    public OsuUser getPlayerInfo(BinUser user, OsuMode mode) {
        if (!user.isAuthorized()) return getPlayerInfo(user.getOsuID(), mode);
        return base.osuApiWebClient.get()
                .uri("me/{mode}", mode.getName())
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToMono(OsuUser.class)
                .map((data) -> {
                    userInfoDao.saveUser(data);
                    user.setOsuID(data.getUID());
                    user.setOsuName(data.getUsername());
                    user.setMode(data.getPlayMode());
                    return data;
                }).block();
    }

    @Override
    public OsuUser getPlayerInfo(String userName, OsuMode mode) {
        return base.osuApiWebClient.get()
                .uri("users/{name}/{mode}", userName, mode.getName())
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(OsuUser.class)
                .map((data) -> {
                    userInfoDao.saveUser(data);
                    return data;
                }).block();
    }

    @Override
    public OsuUser getPlayerInfo(Long id, OsuMode mode) {
        return base.osuApiWebClient.get()
                .uri(l -> l
                        .path("users/{id}/{mode}")
                        .queryParam("key", "id")
                        .build(id, mode.getName()))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(OsuUser.class).block();
    }

    @Override
    public <T extends Number> List<MicroUser> getUsers(Collection<T> users) {
        return base.osuApiWebClient.get()
                .uri(b -> b.path("users").queryParam("ids[]", users).build())
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
        if (!user.isAuthorized()) throw new TipsRuntimeException("无权限");
        return base.osuApiWebClient.get()
                .uri("friends")
                .headers(base.insertHeader(user))
                .retrieve().bodyToFlux(MicroUser.class)
                .collectList()
                .block();
    }
}

package com.now.nowbot.service.osuApiService.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.dao.OsuUserInfoDao;
import com.now.nowbot.model.BindUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.json.*;
import com.now.nowbot.service.osuApiService.OsuUserApiService;
import com.now.nowbot.throwable.TipsRuntimeException;
import com.now.nowbot.util.JacksonUtil;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class UserApiImpl implements OsuUserApiService {
    // private static final Logger log = LoggerFactory.getLogger(UserApiImpl.class);
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
        return UriComponentsBuilder.fromUriString("https://osu.ppy.sh/oauth/authorize")
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
    public String refreshUserToken(BindUser user) {
        if (!user.isAuthorized()) return base.getBotToken();
        return base.refreshUserToken(user, false);
    }

    @Override
    public void refreshUserTokenFirst(BindUser user) {
        base.refreshUserToken(user, true);
        var osuInfo = getPlayerInfo(user);
        var uid = osuInfo.getUserID();
        user.setOsuID(uid);
        user.setOsuName(user.getOsuName());
        user.setOsuMode(user.getOsuMode());
    }

    @Override
    public OsuUser getPlayerInfo(BindUser user, OsuMode mode) {
        if (!user.isAuthorized()) return getPlayerInfo(user.getOsuID(), mode);
        return base.request(client -> client.get()
                .uri("me/{mode}", mode.shortName)
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToMono(OsuUser.class)
                .map((data) -> {
                    userInfoDao.saveUser(data, mode);
                    user.setOsuID(data.getUserID());
                    user.setOsuName(data.getUsername());
                    user.setOsuMode(mode);
                    data.setCurrentOsuMode(OsuMode.getMode(mode, data.getDefaultOsuMode()));

                    return data;
                })
        );
    }

    @Override
    public OsuUser getPlayerInfo(String name, OsuMode mode) {
        return base.request(client -> client
                .get()
                .uri(l -> l
                        .path("users/{data}/{mode}")
                        .build('@' + name, mode.shortName)
                )
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(OsuUser.class)
                .map((data) -> {
                    userInfoDao.saveUser(data, mode);
                    data.setCurrentOsuMode(OsuMode.getMode(mode, data.getDefaultOsuMode()));
                    return data;
                })
        );
    }

    @Override
    public OsuUser getPlayerInfo(Long id, OsuMode mode) {
        return base.request(client -> client.get()
                .uri(l -> l
                        .path("users/{id}/{mode}")
                        .build(id, mode.shortName))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(OsuUser.class)
                .map((data) -> {
                    userInfoDao.saveUser(data, mode);
                    data.setCurrentOsuMode(OsuMode.getMode(mode, data.getDefaultOsuMode()));
                    return data;
                })
        );
    }

    @Override
    public Long getOsuId(String name) {
        Long id = bindDao.getOsuID(name);
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
     * @param isVariant 是否获取玩家的多模式信息
     */
    @Override
    public <T extends Number> List<MicroUser> getUsers(Collection<T> users, Boolean isVariant) {
        return base.request(client -> client.get()
                .uri(b -> b.path("users")
                           .queryParam("ids[]", users)
                           .queryParam("include_variant_statistics", isVariant)
                           .build())
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(result -> {
                    List<MicroUser> userList = JacksonUtil.parseObjectList(result.get("users"), MicroUser.class);
                    userInfoDao.saveUsers(userList);
                    return userList;
                })
        );
    }

    @Override
    public List<LazerFriend> getFriendList(BindUser user) {
        if (!user.isAuthorized()) throw new TipsRuntimeException("无权限");
        return base.request(client -> client.get()
                .uri("friends")
                .headers(base.insertHeader(user))
                .retrieve().bodyToFlux(LazerFriend.class)
                .collectList()
        );
    }

    @Override
    public List<ActivityEvent> getUserRecentActivity(long userId, int s, int e) {
        return base.request(client -> client.get()
                .uri(b -> b.path("users/{userId}/recent_activity")
                        .queryParam("offset", s)
                        .queryParam("limit", e)
                        .build(userId))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToFlux(ActivityEvent.class)
                .collectList()
        );
    }

    @Override
    public KudosuHistory getUserKudosu(BindUser user) {
        return base.request(client -> client.get()
                .uri("users/{uid}/kudosu")
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToMono(KudosuHistory.class)
        );
    }

    @Override
    public JsonNode sendPrivateMessage(BindUser sender, Long target, String message) {
        var body = Map.of("target_id", target, "message", message, "is_action", false);
        return base.request(client -> client.post()
                .uri("chat/new")
                .headers(base.insertHeader(sender))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
        );
    }

    @Override
    public JsonNode acknowledgmentPrivateMessageAlive(BindUser user, Long since) {
        return base.request(client -> client.post()
                .uri(b -> b.path("chat/ack")
                        .queryParamIfPresent("since", Optional.ofNullable(since))
                        .build())
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToMono(JsonNode.class)
        );
    }

    @Override
    public JsonNode getPrivateMessage(BindUser sender, Long channel, Long since) {
        return base.request(client -> client.get()
                .uri("chat/channels/{channel}/messages?since={since}", channel, since)
                .headers(base.insertHeader(sender))
                .retrieve()
                .bodyToMono(JsonNode.class)
        );
    }

    private final Pattern teamFormedPattern = Pattern.compile(
            "Formed</div>\\s+<div class=\"team-info-entry__value\">\\s+(.+)\\s+</div>");
    private final Pattern teamUserPattern = Pattern.compile("data-user=\"(?<json>.+)\"");
    private final Pattern teamModePattern = Pattern.compile("<div class=\"team-info-entry__title\">Default ruleset</div>\\s+<div class=\"team-info-entry__value\">\\s+<span class=\"fal fa-extra-mode-(\\w+)\">");
    // 有点刻晴了
    // "<a\s+class="game-mode-link"\s+href="https://osu.ppy.sh/teams/\d+/(.+)"\s+>"
    // "<div class=\"team-info-entry__title\">Default ruleset</div>\\s+<div class=\"team-info-entry__value\">\\s+<span class=\"fal fa-extra-mode-mania\">[^<]+</span>\\s+(.+)\\s+</div>"
    private final Pattern teamNamePattern = Pattern.compile(
            "<h1 class=\"profile-info__name\">\\s*<span class=\"u-ellipsis-overflow\">\\s*([\\S\\s]+)\\s*</span>\\s*</h1>");
    private final Pattern teamAbbrPattern = Pattern.compile(
            "<p class=\"profile-info__flag\">\\s+\\[([\\S\\s]+)]\\s+</p>");
    private final Pattern teamApplicationPattern = Pattern.compile(
            "application</div>\\s+<div class=\"team-info-entry__value\">\\s+(.+)\\s+</div>");

    private final Pattern rankPattern = Pattern.compile(
            "<div class=\"team-info-entry__value team-info-entry__value--large\">\\s+#([\\d,]+)\\s+</div>");

    private final Pattern ppPattern = Pattern.compile(
            "<div class=\"team-info-entry__title\">\\s+Performance\\s+</div>\\s+<div class=\"team-info-entry__value\">\\s+([\\d,]+)\\s+</div>");
    private final Pattern rankedScorePattern = Pattern.compile(
            "<div class=\"team-info-entry__title\">\\s+Ranked Score\\s+</div>\\s+<div class=\"team-info-entry__value\">\\s+([\\d,]+)\\s+</div>");
    private final Pattern playCountPattern = Pattern.compile(
            "<div class=\"team-info-entry__title\">\\s+Play Count\\s+</div>\\s+<div class=\"team-info-entry__value\">\\s+([\\d,]+)\\s+</div>");
    private final Pattern membersPattern = Pattern.compile(
            "<div class=\"team-info-entry__title\">\\s+Members\\s+</div>\\s+<div class=\"team-info-entry__value\">\\s+([\\d,]+)\\s+</div>");

    private final Pattern teamDescriptionPattern = Pattern.compile("<div class='bbcode'>(.+)</div>");
    private final Pattern teamBannerPattern = Pattern.compile("url\\('(https://assets.ppy.sh/teams/header/.+)'\\)");
    private final Pattern teamFlagPattern = Pattern.compile("url\\('(https://assets.ppy.sh/teams/flag/.+)'\\)");
    @Override
    public TeamInfo getTeamInfo(int id) {
        var html = base.request(client -> client.get()
                .uri("https://osu.ppy.sh/teams/{id}", id)
                .retrieve()
                .bodyToMono(String.class)
        );

        String banner;
        var bannerMatcher = teamBannerPattern.matcher(html);
        if (bannerMatcher.find()) {
            banner = bannerMatcher.group(1);
        } else {
            banner = "";
        }

        String name;
        var nameMatcher = teamNamePattern.matcher(html);
        if (nameMatcher.find()) {
            name = unescapeHTML(nameMatcher.group(1).trim());
        } else {
            name = "";
        }

        String abbr;
        var abbrMatcher = teamAbbrPattern.matcher(html);
        if (abbrMatcher.find()) {
            abbr = unescapeHTML(abbrMatcher.group(1).trim());
        } else {
            abbr = "";
        }

        String flag;
        var flagMatcher = teamFlagPattern.matcher(html);
        if (flagMatcher.find()) {
            flag = flagMatcher.group(1);
        } else {
            flag = "";
        }

        String formed;
        var formedMatcher = teamFormedPattern.matcher(html);
        if (formedMatcher.find()) {
            formed = formedMatcher.group(1);
        } else {
            formed = "";
        }

        OsuMode mode;
        var modeMatcher = teamModePattern.matcher(html);
        if (modeMatcher.find()) {
            mode = OsuMode.getMode(modeMatcher.group(1));
        } else {
            mode = OsuMode.DEFAULT;
        }

        String application;
        var applicationMatcher = teamApplicationPattern.matcher(html);
        if (applicationMatcher.find()) {
            application = applicationMatcher.group(1);
        } else {
            application = "";
        }

        Integer rank;
        var rankMatcher = rankPattern.matcher(html);
        if (rankMatcher.find()) {
            try {
                rank = Integer.parseInt(rankMatcher.group(1).replace(",", ""));
            } catch (NumberFormatException e) {
                rank = null;
            }
        } else {
            rank = null;
        }

        Integer pp;
        var ppMatcher = ppPattern.matcher(html);
        if (ppMatcher.find()) {
            try {
                pp = Integer.parseInt(ppMatcher.group(1).replace(",", ""));
            } catch (NumberFormatException e) {
                pp = null;
            }
        } else {
            pp = null;
        }

        Long rankedScore;
        var rankedScoreMatcher = rankedScorePattern.matcher(html);
        if (rankedScoreMatcher.find()) {
            try {
                rankedScore = Long.parseLong(rankedScoreMatcher.group(1).replace(",", ""));
            } catch (NumberFormatException e) {
                rankedScore = null;
            }
        } else {
            rankedScore = null;
        }

        Long playCount;
        var playCountMatcher = playCountPattern.matcher(html);
        if (playCountMatcher.find()) {
            try {
                playCount = Long.parseLong(playCountMatcher.group(1).replace(",", ""));
            } catch (NumberFormatException e) {
                playCount = null;
            }
        } else {
            playCount = null;
        }

        Integer members;
        var membersMatcher = membersPattern.matcher(html);
        if (membersMatcher.find()) {
            try {
                members = Integer.parseInt(membersMatcher.group(1).replace(",", ""));
            } catch (NumberFormatException e) {
                members = null;
            }
        } else {
            members = null;
        }

        String description;
        var descriptionMatcher = teamDescriptionPattern.matcher(html);
        if (descriptionMatcher.find()) {
            description = descriptionMatcher.group(1);
        } else {
            description = "";
        }

        var userMatcher = teamUserPattern.matcher(html);
        var users = new ArrayList<OsuUser>();
        while (userMatcher.find()) {
            var json = unescapeHTML(userMatcher.group("json"));
            users.add(JacksonUtil.parseObject(json, OsuUser.class));
        }

        return new TeamInfo(
                id,
                name,
                abbr,
                formed,
                banner,
                flag,
                users,
                mode,
                application,

                rank,
                pp,
                rankedScore,
                playCount,
                members,

                description
        );
    }

    // 反转义字符
    private String unescapeHTML(String str) {
        return str.replaceAll("&amp;", "&")
                  .replaceAll("&lt;", "<")
                  .replaceAll("&gt;", ">")
                  .replaceAll("&quot;", "\"")
                  .replaceAll("&apos;", "'")
                  .replaceAll("&nbsp;", " ")

                  .replaceAll("&#038;", "&")
                  .replaceAll("&#034;", "\"")
                  .replaceAll("&#039;", "'")
                  .replaceAll("&#160;", " ")

                ;
    }
}

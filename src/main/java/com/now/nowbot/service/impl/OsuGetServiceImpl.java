package com.now.nowbot.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.NowbotApplication;
import com.now.nowbot.config.OSUConfig;
import com.now.nowbot.dao.BeatMapDao;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.entity.BeatmapLite;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.*;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.match.Match;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.TipsRuntimeException;
import com.now.nowbot.util.JacksonUtil;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;

@Service
public class OsuGetServiceImpl implements OsuGetService {
    public static        BinUser botUser = new BinUser();
    private static final Logger  log     = LoggerFactory.getLogger(OsuGetServiceImpl.class);

    private final int    oauthId;
    private final String redirectUrl;
    private final String oauthToken;
    private final String URL;
    BindDao      bindDao;
    RestTemplate template;
    BeatMapDao   beatMapDao;

    @Autowired
    OsuGetServiceImpl(RestTemplate restTemplate, OSUConfig osuConfig, BindDao bind, @Lazy BeatMapDao beatMap) {
        oauthId = osuConfig.getId();
        redirectUrl = osuConfig.getCallBackUrl();
        oauthToken = osuConfig.getToken();
        URL = osuConfig.getUrl();

        bindDao = bind;
        template = restTemplate;
        beatMapDao = beatMap;
    }


    /***
     * 拼合授权链接
     * @param state QQ[+群号]
     * @return
     */
    @Override
    public String getOauthUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl("https://osu.ppy.sh/oauth/authorize").queryParam("client_id", oauthId).queryParam("redirect_uri", redirectUrl).queryParam("response_type", "code").queryParam("scope", "friends.read identify public").queryParam("state", state).build().encode().toUriString();
    }

    /***
     * 初次得到授权令牌
     * @param binUser
     * @return
     */
    @Override
    public JsonNode getToken(BinUser binUser) {
        String url = "https://osu.ppy.sh/oauth/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED));
        MultiValueMap body = new LinkedMultiValueMap();
        body.add("client_id", oauthId);
        body.add("client_secret", oauthToken);
        body.add("code", binUser.getRefreshToken());
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", redirectUrl);

        HttpEntity<?> httpEntity = new HttpEntity<>(body, headers);
        var s = template.postForObject(url, httpEntity, JsonNode.class);
        binUser.setAccessToken(s.get("access_token").asText());
        binUser.setRefreshToken(s.get("refresh_token").asText());
        binUser.nextTime(s.get("expires_in").asLong());

        getPlayerInfo(binUser);
        bindDao.saveUser(binUser);

        return s;
    }

    /***
     * 拿到机器人访客令牌
     * @return
     */
    @Override
    public String getToken() {
        if (!botUser.isPassed()) {
            return botUser.getAccessToken();
        }
        String url = "https://osu.ppy.sh/oauth/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED));
        MultiValueMap body = new LinkedMultiValueMap();
        body.add("client_id", oauthId);
        body.add("client_secret", oauthToken);
        body.add("grant_type", "client_credentials");
        body.add("scope", "public");

        HttpEntity<?> httpEntity = new HttpEntity<>(body, headers);
        var s = template.postForObject(url, httpEntity, JsonNode.class);
        botUser.setAccessToken(s.get("access_token").asText());
        botUser.nextTime(s.get("expires_in").asLong());
        return botUser.getAccessToken();
    }

    /***
     * 刷新令牌
     * @param binUser
     * @return
     */
    @Override
    public JsonNode refreshToken(BinUser binUser) {
        String url = "https://osu.ppy.sh/oauth/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED));
        MultiValueMap body = new LinkedMultiValueMap();
        body.add("client_id", oauthId);
        body.add("client_secret", oauthToken);
        body.add("refresh_token", binUser.getRefreshToken());
        body.add("grant_type", "refresh_token");
        body.add("redirect_uri", redirectUrl);

        HttpEntity<?> httpEntity = new HttpEntity<>(body, headers);
        JsonNode s = template.postForObject(url, httpEntity, JsonNode.class);
        bindDao.updateToken(binUser.getOsuID(), s.get("access_token").asText(), s.get("refresh_token").asText(), binUser.nextTime(s.get("expires_in").asLong()));
        return s;
    }

    /***
     * 拿到osu id值
     * @param name
     * @return
     */
    @Override
    public Long getOsuId(String name) {
        Long id = bindDao.getOsuId(name);
        if (id != null) {
            return id;

        }
        var date = getPlayerInfo(name);
        bindDao.removeOsuNameToId(date.getId());
        String[] names = new String[date.getPreviousName().size() + 1];
        int i = 0;
        names[i++] = date.getUsername().toUpperCase();
        for (var nName : date.getPreviousName()) {
            names[i++] = nName.toUpperCase();
        }
        bindDao.saveOsuNameToId(date.getId(), names);
        return date.getId();
    }

    @Override
    public List<MicroUser> getFriendList(BinUser user) {
        if (user.getAccessToken() == null) throw new TipsRuntimeException("无权限");
        String url = this.URL + "friends";
        HttpHeaders headers = getHeader(user);
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<List<MicroUser>> c = template.exchange(url, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<List<MicroUser>>() {
        });
        return c.getBody();
    }

    /***
     * 拿到详细的个人信息 新
     * @param user
     * @return
     */
    @Override
    public OsuUser getPlayerOsuInfo(BinUser user) {
        return getPlayerInfo(user, OsuMode.OSU);
    }

    @Override
    public OsuUser getPlayerTaikoInfo(BinUser user) {
        return getPlayerInfo(user, OsuMode.TAIKO);
    }

    @Override
    public OsuUser getPlayerCatchInfo(BinUser user) {
        return getPlayerInfo(user, OsuMode.CATCH);
    }

    @Override
    public OsuUser getPlayerManiaInfo(BinUser user) {
        return getPlayerInfo(user, OsuMode.MANIA);
    }

    @Override
    public OsuUser getPlayerInfo(BinUser user, OsuMode mode) {
        if (user.getAccessToken() == null) return getPlayerInfo(user.getOsuID(), mode);
        String url;
        if (mode == OsuMode.DEFAULT) {
            url = this.URL + "me";
        } else {
            url = this.URL + "me" + '/' + mode.getName();
        }
        HttpHeaders headers = getHeader(user);
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<OsuUser> c = template.exchange(url, HttpMethod.GET, httpEntity, OsuUser.class);
        return c.getBody();
    }

    /**
     * 通过绑定信息获得user数据 刷新osu name
     *
     * @param user
     * @return
     */
    @Override
    public OsuUser getPlayerInfo(BinUser user) {
        if (user.getAccessToken() == null) return getPlayerInfo(user.getOsuID());
        String url;
        if (user.getMode() == null || user.getMode() == OsuMode.DEFAULT) {
            url = this.URL + "me";
        } else {
            url = this.URL + "me" + '/' + user.getMode().getName();
        }
        HttpHeaders headers = getHeader(user);
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<OsuUser> c = template.exchange(url, HttpMethod.GET, httpEntity, OsuUser.class);
        var data = c.getBody();
        user.setOsuID(data.getId());
        user.setOsuName(data.getUsername());
        user.setMode(data.getPlayMode());
        return data;
    }

    /**
     * 仅通过name获取user信息
     *
     * @param userName
     * @return
     */
    @Override
    public OsuUser getPlayerInfo(String userName) {
        String url = this.URL + "users/" + userName;

        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + userName).queryParam("key", "username").build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<OsuUser> c = template.exchange(uri, HttpMethod.GET, httpEntity, OsuUser.class);
        var data = c.getBody();
        return data;
    }
    @Override
    public OsuUser getPlayerInfo(String userName, OsuMode mode) {
        String url = this.URL + "users/" + userName;
        String modeStr;
        if (mode == OsuMode.DEFAULT) {
            modeStr = "";
        } else {
            modeStr = "/" + mode.getName();
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + userName + modeStr).queryParam("key", "username").build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<OsuUser> c = template.exchange(uri, HttpMethod.GET, httpEntity, OsuUser.class);
        var data = c.getBody();
        return data;
    }


    /***
     * 使用本机token获取user信息
     * @param id
     * @return
     */
    @Override
    public OsuUser getPlayerOsuInfo(Long id) {
        return getPlayerInfo(id, OsuMode.OSU);
    }

    @Override
    public OsuUser getPlayerTaikoInfo(Long id) {
        return getPlayerInfo(id, OsuMode.TAIKO);
    }

    @Override
    public OsuUser getPlayerCatchInfo(Long id) {
        return getPlayerInfo(id, OsuMode.CATCH);
    }

    @Override
    public OsuUser getPlayerManiaInfo(Long id) {
        return getPlayerInfo(id, OsuMode.MANIA);
    }

    @Override
    public OsuUser getPlayerInfo(Long id) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + id).queryParam("key", "id").build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<OsuUser> c = template.exchange(uri, HttpMethod.GET, httpEntity, OsuUser.class);
        return c.getBody();
    }


    @Override
    public OsuUser getPlayerInfo(Long id, OsuMode mode) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + id + '/' + mode).queryParam("key", "id").build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<OsuUser> c = template.exchange(uri, HttpMethod.GET, httpEntity, OsuUser.class);
        return c.getBody();
    }

    @Override
    public String getPlayerInfoStr(Long id, OsuMode mode) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + id + '/' + mode).queryParam("key", "id").build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JsonNode> c = template.exchange(uri, HttpMethod.GET, httpEntity, JsonNode.class);
        return c.getBody().toPrettyString();
    }

    /***
     * 使用他人token获取user信息
     * @param id
     * @param user 绑定用户
     * @param mode
     * @return
     */
    @Override
    public OsuUser getPlayerInfo(Long id, BinUser user, OsuMode mode) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + id + '/' + mode).queryParam("key", "id").build().encode().toUri();
        HttpHeaders headers = getHeader(user);

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<OsuUser> c = template.exchange(uri, HttpMethod.GET, httpEntity, OsuUser.class);
        return c.getBody();
    }

    @Override
    public String getPlayerInfoN(Long id) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + id).queryParam("key", "id").build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JsonNode> c = template.exchange(uri, HttpMethod.GET, httpEntity, JsonNode.class);
        return c.getBody().toPrettyString();
    }

    /***
     * 批量获取玩家信息 小于50
     * @param users id
     * @return
     */
    @Override
    public <T extends Number> JsonNode getUsers(Collection<T> users) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users")
                .queryParam("ids[]", users).build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JsonNode> c = template.exchange(uri, HttpMethod.GET, httpEntity, JsonNode.class);
        return c.getBody();
    }

    /**
     * 获得某个模式的bp表
     *
     * @param user
     * @param mode
     * @param s
     * @param e
     * @return
     */
    @Override
    public List<Score> getBestPerformance(BinUser user, OsuMode mode, int s, int e) {
        if (user.getAccessToken() == null) return getBestPerformance(user.getOsuID(), mode, s, e);
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + user.getOsuID() + "/scores/best").queryParam("limit", e).queryParam("offset", s);
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        URI uri = data.build().encode().toUri();
        HttpHeaders headers = getHeader(user);

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<List<Score>> c = template.exchange(uri, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<List<Score>>() {
        });
        return c.getBody();
    }

    /**
     * @param id
     * @param mode
     * @param s
     * @param e
     * @return
     */
    @Override
    public List<Score> getBestPerformance(Long id, OsuMode mode, int s, int e) {
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + id + "/scores/best").queryParam("limit", e).queryParam("offset", s);
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        URI uri = data.build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity<HttpHeaders> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<List<Score>> c = template.exchange(uri, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<List<Score>>() {
        });
        return c.getBody();
    }

    @Override
    public List<JsonNode> getBestPerformance_raw(Long id, OsuMode mode, int s, int e) {
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + id + "/scores/best")
                .queryParam("limit", e)
                .queryParam("offset", s);
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        URI uri = data.build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity<HttpHeaders> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<List<JsonNode>> c = template.exchange(uri, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<List<JsonNode>>() {
        });
        return c.getBody();
    }


    /***
     * 获得score(最近游玩列表
     * @param user
     * @param offset
     * @param limit
     * @return
     */
    @Override
    public List<Score> getRecentN(BinUser user, OsuMode mode, int offset, int limit) {
        if (user.getAccessToken() == null) return getRecentN(user.getOsuID(), mode, offset, limit);
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + user.getOsuID() + "/scores/recent")
                .queryParam("limit", limit)
                .queryParam("offset", offset);
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        var uri = data.build().encode().toUri();
        HttpHeaders headers = getHeader(user);

        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<List<Score>> c = template.exchange(uri, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<List<Score>>() {
        });
        return c.getBody();
    }

    /***
     * 获得成绩 不包含fail
     * @param userId
     * @param mode 模式
     * @param offset 从开始
     * @param limit 不包含本身
     * @return
     */
    @Override
    public List<Score> getRecentN(long userId, OsuMode mode, int offset, int limit) {
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + userId + "/scores/recent")
                .queryParam("limit", limit)
                .queryParam("offset", offset);
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        var uri = data.build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<List<Score>> c = template.exchange(uri, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<List<Score>>() {
        });
        return c.getBody();
    }

    @Override
    public List<JsonNode> getRecentNR(long userId, OsuMode mode, String s3, int s, int e) {
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + userId + "/scores/recent")
                .queryParam("limit", e)
                .queryParam("offset", s);
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        var uri = data.build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<List<JsonNode>> c = template.exchange(uri, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<List<JsonNode>>() {
        });
        return c.getBody();
    }

    @Override
    public List<Score> getAllRecentN(long userId, OsuMode mode, int s, int e) {
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + userId + "/scores/recent")
                .queryParam("include_fails", 1)
                .queryParam("limit", e)
                .queryParam("offset", s);
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        var uri = data.build().encode().toUri();

        HttpHeaders headers = getHeader();
        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<List<Score>> c = template.exchange(uri, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<List<Score>>() {
        });
        return c.getBody();
    }

    @Override
    public List<Score> getAllRecentN(BinUser user, OsuMode mode, int s, int e) {
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + user.getOsuID() + "/scores/recent")
                .queryParam("include_fails", 1)
                .queryParam("limit", e)
                .queryParam("offset", s);
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        var uri = data.build().encode().toUri();

        HttpHeaders headers = getHeader(user);
        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<List<Score>> c = template.exchange(uri, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<List<Score>>() {
        });
        return c.getBody();
    }

    @Override
    public List<Score> getRecentN(int userId, OsuMode mode, int s, int e) {
        return getRecentN((long) userId, mode, s, e);
    }

    @Override
    public List<Score> getAllRecentN(int userId, OsuMode mode, int s, int e) {
        return getAllRecentN((long) userId, mode, s, e);
    }

    @Override
    public BeatmapUserScore getScore(long bid, long uid, OsuMode mode){
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + bid + "/scores/users/" + uid);
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        URI uri = data.build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<BeatmapUserScore> c = template.exchange(uri, HttpMethod.GET, httpEntity, BeatmapUserScore.class);
        if (c.getStatusCode().is4xxClientError()) {
            return null;
        }
        return c.getBody();
    }

    @Override
    public BeatmapUserScore getScore(long bid, long uid, OsuMode mode, Mod... mods) throws JsonProcessingException {
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + bid + "/scores/users/" + uid);
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        if (mods.length != 0) {
            StringBuilder sb = new StringBuilder("[");
            for (var mod : mods) {
                sb.append(mod.name()).append(',');
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(']');
            data.queryParam("mods", sb.toString());
        }
        URI uri = data.build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<BeatmapUserScore> c = template.exchange(uri, HttpMethod.GET, httpEntity, BeatmapUserScore.class);
        if (c.getStatusCode().is4xxClientError()) {
            return null;
        }
        return c.getBody();
    }

    @Override
    public BeatmapUserScore getScore(long bid, BinUser user, OsuMode mode) {
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + bid + "/scores/users/" + user.getOsuID());
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        URI uri = data.build().encode().toUri();
        HttpHeaders headers = getHeader(user);

        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<BeatmapUserScore> c = template.exchange(uri, HttpMethod.GET, httpEntity, BeatmapUserScore.class);
        if (c.getStatusCode().is4xxClientError()) {
            return null;
        }
        return c.getBody();
    }

    @Override
    public List<Score> getScoreAll(long bid, BinUser user, OsuMode mode) {
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + bid + "/scores/users/" + user.getOsuID() + "/all");
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        URI uri = data.build().encode().toUri();
        HttpHeaders headers = getHeader(user);

        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        return getBeatmapUserScores(uri, httpEntity);
    }

    @Override
    public List<Score> getScoreAll(long bid, long uid, OsuMode mode) {
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + bid + "/scores/users/" + uid + "/all");
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        URI uri = data.build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        return getBeatmapUserScores(uri, httpEntity);
    }

    @Override
    public List<Score> getBeatmapScores(long bid, OsuMode mode) {
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + bid + "/scores");
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        URI uri = data.build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        return getBeatmapUserScores(uri, httpEntity);
    }

    @Override
    public JsonNode getScoreR(long bid, BinUser user, OsuMode mode) {
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + bid + "/scores/users/" + user.getOsuID());
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        URI uri = data.build().encode().toUri();
        HttpHeaders headers = getHeader(user);

        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> c = template.exchange(uri, HttpMethod.GET, httpEntity, JsonNode.class);
        if (c.getStatusCode().is4xxClientError()) {
            return null;
        }
        return c.getBody();
    }


    /***
     * 下载beatmap(.osu)文件
     * @param bid 谱面id
     * @return osu文件字符串流
     */
    @Override
    public String getBeatMapFile(long bid) throws IOException {
        URL url = new URL("https://osu.ppy.sh/osu/" + bid);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        byte[] byteData;
        try {
            httpConn.connect();
            InputStream cin = httpConn.getInputStream();
            byteData = cin.readAllBytes();
        } catch (IOException e) {
            NowbotApplication.log.error("getBeatMapFile Error: ", e);
            return null;
        }
        return new String(byteData);
    }

    /***
     * 获取map信息
     * @param bid bid
     * @return
     */
    @Override
    public BeatMap getBeatMapInfo(int bid) {
        return getBeatMapInfo((long) bid);
    }


    @Override
    public BeatMap getBeatMapInfo(long bid) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + bid).build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<BeatMap> c = template.exchange(uri, HttpMethod.GET, httpEntity, BeatMap.class);
        var map = c.getBody();
        beatMapDao.saveMap(map);
        return map;
    }

    @Override
    public BeatMap getMapInfoFromDB(long bid) {
        try {
            var lite = beatMapDao.getBeatMapLite(bid);
            return BeatMapDao.fromBeatmapLite(lite);
        } catch (Exception e) {
            return getBeatMapInfo(bid);
        }
    }

    @Override
    public BeatmapLite getMapInfoLite(long bid) {
        try {
            return beatMapDao.getBeatMapLite(bid);
        } catch (NullPointerException ignore) {
            return BeatMapDao.fromBeatmapModel(getBeatMapInfo(bid));
        }
    }

    @Override
    public JsonNode getMapInfoR(long bid) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + bid).build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JsonNode> c = template.exchange(uri, HttpMethod.GET, httpEntity, JsonNode.class);
        return c.getBody();
    }

    /***
     * 下载replay文件 字节文件
     * @param mode
     * @param id
     * @return
     */
    @Override
    public byte[] getReplay(String mode, long id) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "scores/" + mode + "/" + id + "/download")
//                .queryParam("mode","osu")
                .build().encode().toUri();
        HttpHeaders headers = getHeader();

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<byte[]> c = null;
        try {
            System.out.println(uri);
            c = template.exchange(uri, HttpMethod.GET, httpEntity, byte[].class);
        } catch (RestClientException e) {
            e.printStackTrace();
            return null;
        }
        return c.getBody();
    }

    /***
     * PP+获取
     * @param name
     * @return
     */
    @Override
    @Retryable(value = {SocketTimeoutException.class, ConnectException.class, UnknownHttpStatusCodeException.class}, //超时类 SocketTimeoutException, 连接失败ConnectException, 其他未知异常UnknownHttpStatusCodeException
            maxAttempts = 5, backoff = @Backoff(delay = 5000L, random = true, multiplier = 1))
    public PpPlus ppPlus(String name) {
        URI uri = UriComponentsBuilder.fromHttpUrl("https://syrin.me/pp+/api/user/" + name).build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JsonNode> response = null;

        response = template.exchange(uri, HttpMethod.GET, httpEntity, JsonNode.class);


        var data = response.getBody().get("user_data");
        if (data != null) return JacksonUtil.parseObject(data, PpPlus.class);
        else throw new RuntimeException("get response error");
    }

    /***
     * pp+比例
     * @param ppP
     * @return
     */
    @Override
    public float[] ppPlus(float[] ppP) {
        if (ppP.length != 6) return null;
        float[] date = new float[6];
        int[] fall = {5800, 1400, 3200, 2800, 3800, 1200};
        for (int i = 0; i < date.length; i++) {
            date[i] = ppP[i] / fall[i];
            if (date[i] > 1) date[i] = 1;
        }
        return date;
    }


    /***
     * 比赛信息
     * @param mid
     * @return
     */
    @Override
    public Match getMatchInfo(int mid) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "matches/" + mid).build().encode().toUri();
        HttpHeaders headers = getHeader();
        HttpEntity httpEntity = new HttpEntity(headers);
        Match data = null;
        try {
            data = template.exchange(uri, HttpMethod.GET, httpEntity, Match.class).getBody();
        } catch (Exception exc) {
            log.error("match error ", exc);

            throw new TipsRuntimeException(exc.getMessage());
        }
        return data;
    }

    @Override
    public Match getMatchInfo(int mid, long before) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "matches/" + mid).queryParam("before", before).queryParam("limit", 100).build().encode().toUri();
        HttpHeaders headers = getHeader();
        HttpEntity httpEntity = new HttpEntity(headers);
        Match data = null;
        try {
            data = template.exchange(uri, HttpMethod.GET, httpEntity, Match.class).getBody();
        } catch (Exception exc) {
            log.error("match error ", exc);

            throw new TipsRuntimeException(exc.getMessage());
        }
        return data;
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Long id) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + id + "/attributes").build().encode().toUri();
        ResponseEntity<JsonNode> c = template.exchange(uri, HttpMethod.POST, null, JsonNode.class);
        return JacksonUtil.parseObject(c.getBody().get("attributes"), BeatmapDifficultyAttributes.class);
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Integer id) {
        return getAttributes((long) id);
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Long id, Mod... mods) {
        int i = 0;
        for (var m : mods) {
            i |= m.value;
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL).path("beatmaps/").path(String.valueOf(id)).path("/attributes").build().encode().toUri();
        HttpHeaders headers = getHeader();
        HashMap body = new HashMap<>();
        body.put("mods", i);
        HttpEntity httpEntity = new HttpEntity(JacksonUtil.objectToJsonPretty(body), headers);
        ResponseEntity<JsonNode> c = template.exchange(uri, HttpMethod.POST, httpEntity, JsonNode.class);
        return JacksonUtil.parseObject(c.getBody().get("attributes"), BeatmapDifficultyAttributes.class);
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Integer id, Mod... mods) {
        return getAttributes((long) id, mods);
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Long id, OsuMode osuMode, Mod... mods) {
        int i = 0;
        for (var m : mods) {
            i |= m.value;
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + id + "/attributes").build().encode().toUri();


        HttpHeaders headers = getHeader();
        HashMap body = new HashMap<>();
        body.put("mods", i);
        body.put("ruleset_id", osuMode.getModeValue());
        HttpEntity httpEntity = new HttpEntity(JacksonUtil.objectToJsonPretty(body), headers);
        ResponseEntity<JsonNode> c = template.exchange(uri, HttpMethod.POST, httpEntity, JsonNode.class);
        return JacksonUtil.parseObject(c.getBody().get("attributes"), BeatmapDifficultyAttributes.class);
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Long id, OsuMode osuMode, int modInt) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + id + "/attributes").build().encode().toUri();


        HttpHeaders headers = getHeader();
        HashMap body = new HashMap<>();
        body.put("mods", modInt);
        body.put("ruleset_id", osuMode.getModeValue());
        HttpEntity httpEntity = new HttpEntity(JacksonUtil.objectToJson(body), headers);
        ResponseEntity<JsonNode> c = template.exchange(uri, HttpMethod.POST, httpEntity, JsonNode.class);
        return JacksonUtil.parseObject(c.getBody().get("attributes"), BeatmapDifficultyAttributes.class);
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Long id, int modInt) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + id + "/attributes").build().encode().toUri();


        HttpHeaders headers = getHeader();
        HashMap body = new HashMap<>();
        body.put("mods", modInt);
        HttpEntity httpEntity = new HttpEntity(JacksonUtil.objectToJsonPretty(body), headers);
        ResponseEntity<JsonNode> c = template.exchange(uri, HttpMethod.POST, httpEntity, JsonNode.class);
        return JacksonUtil.parseObject(c.getBody().get("attributes"), BeatmapDifficultyAttributes.class);
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Integer id, OsuMode osuMode, Mod... mods) {
        return getAttributes((long) id, osuMode, mods);
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Long id, OsuMode osuMode) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + id + "/attributes").build().encode().toUri();
        HttpHeaders headers = getHeader();
        HashMap body = new HashMap<>();
        body.put("ruleset_id", osuMode.getModeValue());
        HttpEntity httpEntity = new HttpEntity(JacksonUtil.objectToJsonPretty(body), headers);

        var c = template.postForObject(uri, httpEntity, JsonNode.class);
        return JacksonUtil.parseObject(c.get("attributes"), BeatmapDifficultyAttributes.class);
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Integer id, OsuMode osuMode) {
        return getAttributes((long) id, osuMode);
    }

    @Override
    public KudosuHistory getUserKudosu(BinUser user) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + user.getOsuID() + "/kudosu").build().encode().toUri();
        var headers = getHeader(user);

        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        var x = template.exchange(uri, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<KudosuHistory>() {
        });
        return x.getBody();
    }

    @Override
    public JsonNode lookupBeatmap(String checksum, String filename, Long id) {
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmapsets/lookup");
        if (checksum != null) {
            data.queryParam("checksum", checksum);
        }
        if (filename != null) {
            data.queryParam("filename", filename);
        }
        if (id != null) {
            data.queryParam("id", id);
        }
        var uri = data.build().toUri();
        var headers = getHeader();

        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        var x = template.exchange(uri, HttpMethod.GET, httpEntity, JsonNode.class);
        return x.getBody();
    }

    @Override
    public Search searchBeatmap(Map<String, Object> query) {
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmapsets/search/");
        for (var e : query.entrySet()) {
            if (e.getValue() != null) {
                data.queryParam(e.getKey(), e.getValue());
            } else {
                data.queryParam(e.getKey());
            }
        }
        var uri = data.build().toUri();
        var headers = getHeader();

        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        var x = template.exchange(uri, HttpMethod.GET, httpEntity, Search.class);
        var resolute = x.getBody();
        if (resolute != null) {
            resolute.setRule(query.toString());
        }
        return resolute;
    }


    @Override
    public JsonNode chatGetChannels(BinUser user) {
        var header = getHeader(user);
        HashMap body = new HashMap<>();
        body.put("target_id", 7003013);
        body.put("message", "osuMode.getModeValue()");
        body.put("is_action", false);
        HttpEntity httpEntity = new HttpEntity(JacksonUtil.objectToJson(body), header);
        var res = template.exchange(URL + "/chat/new", HttpMethod.POST, httpEntity, JsonNode.class);
        return res.getBody();
    }

    @Nullable
    private List<Score> getBeatmapUserScores(URI uri, HttpEntity<?> httpEntity) {
        ResponseEntity<JsonNode> c = template.exchange(uri, HttpMethod.GET, httpEntity, JsonNode.class);
        if (c.getStatusCode().is4xxClientError()) {
            return new ArrayList<>();
        }
        var s = JacksonUtil.parseObjectList(c.getBody().get("scores"), Score.class);
        return s;
    }

    private HttpHeaders getHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + getToken());
        return headers;
    }

    private HttpHeaders getHeader(BinUser user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        try {
            headers.set("Authorization", "Bearer " + (user == null ? getToken() : user.getAccessToken(this)));
        } catch (BindException e) {
            OsuGetServiceImpl.log.error("绑定丢失: [{}], 移除绑定信息", user.getOsuID());
            bindDao.removeBind(user.getOsuID());
            throw e;
        }
        return headers;
    }
}
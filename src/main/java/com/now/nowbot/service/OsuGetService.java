package com.now.nowbot.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.now.nowbot.config.OSUConfig;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.PpPlus;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.throwable.RequestException;
import com.now.nowbot.throwable.TipsRuntimeException;
import com.now.nowbot.util.JacksonUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Collections;
import java.util.List;

@Service
public class OsuGetService {
    public static BinUser botUser = new BinUser();

    private int oauthId;
    private String redirectUrl;
    private String oauthToken;
    private String URL;
    BindDao bindDao;

    static final ObjectMapper jsonMapper = new ObjectMapper();

    RestTemplate template;

    @Autowired
    OsuGetService(RestTemplate restTemplate, OSUConfig osuConfig, BindDao bind) {
        oauthId = osuConfig.getId();
        redirectUrl = osuConfig.getCallBackUrl();
        oauthToken = osuConfig.getToken();
        URL = osuConfig.getUrl();

        bindDao = bind;
        template = restTemplate;
    }


    /***
     * 拼合授权链接
     * @param state qq[+群号]
     * @return
     */
    public String getOauthUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl("https://osu.ppy.sh/oauth/authorize")
                .queryParam("client_id", oauthId)
                .queryParam("redirect_uri", redirectUrl)
                .queryParam("response_type", "code")
                .queryParam("scope", "friends.read identify public")
                .queryParam("state", state)
                .build().encode().toUriString();
    }

    /***
     * 初次得到授权令牌
     * @param binUser
     * @return
     */
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
        JSONObject s = template.postForObject(url, httpEntity, JSONObject.class);
        botUser.setAccessToken(s.getString("access_token"));
        botUser.nextTime(s.getLong("expires_in"));
        return botUser.getAccessToken();
    }

    /***
     * 刷新令牌
     * @param binUser
     * @return
     */
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
//        BindingUtil.writeOsuID(date.getString("username"), id);
    }

    public JsonNode getFrendList(BinUser user) {
        if (user.getAccessToken() == null) throw new TipsRuntimeException("无权限");
        String url = this.URL + "friends";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + user.getAccessToken(this));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JsonNode> c = template.exchange(url, HttpMethod.GET, httpEntity, JsonNode.class);
        return c.getBody();
    }

    /***
     * 拿到详细的个人信息 新
     * @param user
     * @return
     */
    public OsuUser getPlayerOsuInfo(BinUser user) {
        return getPlayerInfo(user, OsuMode.OSU);
    }

    public OsuUser getPlayerTaikoInfo(BinUser user) {
        return getPlayerInfo(user, OsuMode.TAIKO);
    }

    public OsuUser getPlayerCatchInfo(BinUser user) {
        return getPlayerInfo(user, OsuMode.CATCH);
    }

    public OsuUser getPlayerManiaInfo(BinUser user) {
        return getPlayerInfo(user, OsuMode.MANIA);
    }

    public OsuUser getPlayerInfo(BinUser user, OsuMode mode) {
        if (user.getAccessToken() == null) return getPlayerInfo(user.getOsuID(), mode);
        String url = this.URL + "me" + '/' + mode.getName();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + user.getAccessToken(this));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED));
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<OsuUser> c = template.exchange(url, HttpMethod.GET, httpEntity, OsuUser.class);
        return c.getBody();
    }

    /**
     * 通过绑定信息获得user数据 刷新osu name
     * @param user
     * @return
     */
    public OsuUser getPlayerInfo(BinUser user) {
        if (user.getAccessToken() == null) return getPlayerInfo(user.getOsuID());
        String url;
        if (user.getMode() == null) {
            url = this.URL + "me";
        }
        else {
            url = this.URL + "me" + '/' + user.getMode().getName();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + user.getAccessToken(this));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED));
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
    public OsuUser getPlayerInfo(String userName) {
        String url = this.URL + "users/" + userName;

        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + userName)
                .queryParam("key", "username")
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + getToken());

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
    public OsuUser getPlayerOsuInfo(Long id) {
        return getPlayerInfo(id, OsuMode.OSU);
    }

    public OsuUser getPlayerTaikoInfo(Long id) {
        return getPlayerInfo(id, OsuMode.TAIKO);
    }

    public OsuUser getPlayerCatchInfo(Long id) {
        return getPlayerInfo(id, OsuMode.CATCH);
    }

    public OsuUser getPlayerManiaInfo(Long id) {
        return getPlayerInfo(id, OsuMode.MANIA);
    }

    public OsuUser getPlayerInfo(Long id) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + id)
                .queryParam("key", "id")
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + getToken());

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<OsuUser> c = template.exchange(uri, HttpMethod.GET, httpEntity, OsuUser.class);
        return c.getBody();
    }


    public OsuUser getPlayerInfo(Long id, OsuMode mode) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + id + '/' + mode)
                .queryParam("key", "id")
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + getToken());

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<OsuUser> c = template.exchange(uri, HttpMethod.GET, httpEntity, OsuUser.class);
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
    public List<BpInfo> getBestPerformance(BinUser user, OsuMode mode, int s, int e) {
        if (user.getAccessToken() == null) return getBestPerformance(user.getOsuID(), mode, s, e);
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + user.getOsuID() + "/scores/best")
                .queryParam("limit", e)
                .queryParam("offset", s);
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        URI uri = data.build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + user.getAccessToken(this));

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<List<BpInfo>> c = template.exchange(uri, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<List<BpInfo>>() {
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
    public List<BpInfo> getBestPerformance(Long id, OsuMode mode, int s, int e) {
        var data = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + id + "/scores/best")
                .queryParam("limit", e)
                .queryParam("offset", s);
        if (mode != OsuMode.DEFAULT) data.queryParam("mode", mode.getName());
        URI uri = data.build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + getToken());

        HttpEntity<HttpHeaders> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<List<BpInfo>> c = template.exchange(uri, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<List<BpInfo>>() {
        });
        return c.getBody();
    }


    /***
     * 获得score(最近游玩列表
     * @param user
     * @param s
     * @param e
     * @return
     */
    public JSONArray getOsuRecent(BinUser user, int s, int e) {
        return getRecent(user, "osu", s, e);
    }

    public JSONArray getOsuRecent(int id, int s, int e) {
        return getRecent(id, "osu", s, e);
    }

    public JSONArray getOsuAllRecent(BinUser user, int s, int e) {
        return getAllRecent(user, "osu", s, e);
    }

    public JSONArray getOsuAllRecent(int id, int s, int e) {
        return getAllRecent(id, "osu", s, e);
    }

    public JSONArray getRecent(BinUser user, String mode, int s, int e) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + user.getOsuID() + "/scores/recent")
                .queryParam("mode", mode)
                .queryParam("limit", e)
                .queryParam("offset", s)
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + user.getAccessToken(this));

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JSONArray> c = template.exchange(uri, HttpMethod.GET, httpEntity, JSONArray.class);
        return c.getBody();
    }
    public List<Object> getRecentN(BinUser user, String mode, int s, int e) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + user.getOsuID() + "/scores/recent")
                .queryParam("mode", mode)
                .queryParam("limit", e)
                .queryParam("offset", s)
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + user.getAccessToken(this));

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JSONArray> c = template.exchange(uri, HttpMethod.GET, httpEntity, JSONArray.class);
        return c.getBody();
    }

    /***
     * 包含fail的
     * @param user
     * @param mode
     * @param s
     * @param e
     * @return
     */
    public JSONArray getAllRecent(BinUser user, String mode, int s, int e) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + user.getOsuID() + "/scores/recent")
                .queryParam("mode", mode)
                .queryParam("include_fails", 1)
                .queryParam("limit", e)
                .queryParam("offset", s)
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + user.getAccessToken(this));

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JSONArray> c = template.exchange(uri, HttpMethod.GET, httpEntity, JSONArray.class);
        return c.getBody();
    }

    /**
     * 获得上次成绩,不包含fail
     *
     * @param id
     * @param mode
     * @param s
     * @param e
     * @return
     */
    public JSONArray getRecent(int id, String mode, int s, int e) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + id + "/scores/recent")
                .queryParam("mode", mode)
                .queryParam("limit", e)
                .queryParam("offset", s)
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + getToken());

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JSONArray> c = null;
        try {
            c = template.exchange(uri, HttpMethod.GET, httpEntity, JSONArray.class);
        } catch (RestClientException restClientException) {
            restClientException.printStackTrace();
            return null;
        }
        return c.getBody();
    }

    public JSONArray getAllRecent(int id, String mode, int s, int e) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/" + id + "/scores/recent")
                .queryParam("mode", mode)
                .queryParam("include_fails", 1)
                .queryParam("limit", e)
                .queryParam("offset", s)
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + getToken());

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JSONArray> c = template.exchange(uri, HttpMethod.GET, httpEntity, JSONArray.class);
        return c.getBody();
    }

    /***
     * 获取Score成绩的基类
     * @param bid bid
     * @param id 用户id
     * @param mode 游戏模式
     * @param mods mod,仅支持类似:[HD,DT]
     * @return
     */
    public JSONObject getScore(int bid, int id, String mode, String[] mods) {
        var uribulid = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + bid + "/scores/users/" + id)
                .queryParam("mode", mode);
        if (mods != null)
            for (int i = 0; i < mods.length; i++) {
                uribulid.queryParam("mods[]", mods[i]);
            }

        URI uri = uribulid.build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + getToken());

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JSONObject> c = null;
        try {
            c = template.exchange(uri, HttpMethod.GET, httpEntity, JSONObject.class);
        } catch (RestClientException e) {
            e.printStackTrace();
            return null;
        }
        return c.getBody();
    }

    public JSONObject getScore(int bid, int uid) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + bid + "/scores/users/" + uid)
                .queryParam("mode", "osu")
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + getToken());

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JSONObject> c = null;
        try {
            c = template.exchange(uri, HttpMethod.GET, httpEntity, JSONObject.class);
        } catch (RestClientException e) {
            e.printStackTrace();
            return null;
        }
        return c.getBody();
    }

    public JSONObject getScore(int bid, BinUser user) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + bid + "/scores/users/" + user.getOsuID())
                .queryParam("mode", "osu")
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + user.getAccessToken(this));

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JSONObject> c = null;
        try {
            c = template.exchange(uri, HttpMethod.GET, httpEntity, JSONObject.class);
        } catch (RestClientException e) {
            e.printStackTrace();
            return null;
        }
        return c.getBody();
    }

    public String getBitmapFile(int mapId) throws IOException {
        return getBitmapFile(mapId, "osu");
    }

    /***
     * 下载bitmap(.osu)文件
     * @param mapId
     * @param mode
     * @return
     * @throws IOException
     */
    public String getBitmapFile(int mapId, String mode) throws IOException {
        //osu taiko mania catch
        URL url = new URL("https://osu.ppy.sh/" + mode + '/' + mapId);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.connect();
        InputStream cin = httpConn.getInputStream();
        byte[] datebyte = cin.readAllBytes();
        return new String(datebyte);
    }

    /***
     * 获取map信息
     * @param bid bid
     * @return
     */
    public BeatMap getMapInfo(int bid) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + bid)
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + getToken());

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<BeatMap> c = template.exchange(uri, HttpMethod.GET, httpEntity, BeatMap.class);
        return c.getBody();
    }

    /***
     * 获取map信息
     * @param bid bid
     * @param uset
     * @return
     */
    public BeatMap getMapInfo(int bid, BinUser uset) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "beatmaps/" + bid)
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + uset.getAccessToken(this));

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<BeatMap> c = template.exchange(uri, HttpMethod.GET, httpEntity, BeatMap.class);
        return c.getBody();
    }

    /***
     * 下载replay文件 字节文件
     * @param mode
     * @param id
     * @return
     */
    public byte[] getReplay(String mode, long id) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "scores/" + mode + "/" + id + "/download")
//                .queryParam("mode","osu")
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + getToken());

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
    @Retryable(
            value = {SocketTimeoutException.class, ConnectException.class, UnknownHttpStatusCodeException.class}, //超时类 SocketTimeoutException, 连接失败ConnectException, 其他未知异常UnknownHttpStatusCodeException
            maxAttempts = 5,
            backoff = @Backoff(delay = 5000L, random = true, multiplier = 1)
    )
    public PpPlus ppPlus(String name) {
        URI uri = UriComponentsBuilder.fromHttpUrl("https://syrin.me/pp+/api/user/" + name)
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JsonNode> response = null;

        response = template.exchange(uri, HttpMethod.GET, httpEntity, JsonNode.class);


        var data = response.getBody().get("user_data");
        if (data != null)
            return JacksonUtil.parseObject(data ,PpPlus.class);
        else throw new RuntimeException("get response error");
    }

    /***
     * pp+比例
     * @param ppP
     * @return
     */
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
     * 批量获取玩家信息 0-100
     * @param id
     * @return
     */
    public JsonNode getPlayersInfo(int... id) {
        if (id.length <= 0 || id.length > 50) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i : id) {
            sb.append(i).append(',');
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "users/")
                .queryParam("ids[]", sb.toString())
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + getToken());

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JsonNode> c = template.exchange(uri, HttpMethod.GET, httpEntity, JsonNode.class);
        return c.getBody();

    }

    /***
     * 比赛信息
     * @param mid
     * @return
     */
    public JsonNode getMatchInfo(int mid) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "matches/" + mid)
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        headers.set("Authorization", "Bearer " + getToken());
        HttpEntity httpEntity = new HttpEntity(headers);
        JsonNode data = null;
        try {
            data = template.exchange(uri, HttpMethod.GET, httpEntity, JsonNode.class).getBody();
        } catch (Exception exc) {
            var e = (RequestException) exc.getCause();
            System.out.println(e.message);
        }
        return data;
    }

    public JsonNode getMatchInfo(int mid, long before) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.URL + "matches/" + mid)
                .queryParam("before", before)
                .queryParam("limit", 100)
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        headers.set("Authorization", "Bearer " + getToken());
        HttpEntity httpEntity = new HttpEntity(headers);
        JsonNode data = null;
        try {
            data = template.exchange(uri, HttpMethod.GET, httpEntity, JsonNode.class).getBody();
        } catch (Exception exc) {
            var e = (RequestException) exc.getCause();
            System.out.println(e.message);
        }
        return data;
    }
}

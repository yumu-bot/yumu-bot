package com.now.nowbot.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.util.BindingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collections;

@Service
public class OsuGetService {
    public static BinUser botUser = new BinUser();
    @Value("${ppy.id}")
    private int oauthId;
    @Value("${ppy.callBackUrl}")
    private String redirectUrl;
    @Value("${ppy.token}")
    private String oauthToken;
    @Value("${ppy.url}")
    private String url;
    @Value("${ppy.v1token:null}")
    private String tokenv1;

    @Autowired
    RestTemplate template;

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
    public JSONObject getToken(BinUser binUser) {
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
        JSONObject s = template.postForObject(url, httpEntity, JSONObject.class);
        binUser.setAccessToken(s.getString("access_token"));
        binUser.setRefreshToken(s.getString("refresh_token"));
        binUser.nextTime(s.getLong("expires_in"));
        try {
            BindingUtil.writeUser(binUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    public JSONObject refreshToken(BinUser binUser) {
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
        JSONObject s = template.postForObject(url, httpEntity, JSONObject.class);
        binUser.setAccessToken(s.getString("access_token"));
        binUser.setRefreshToken(s.getString("refresh_token"));
        binUser.nextTime(s.getLong("expires_in"));
        try {
            BindingUtil.writeUser(binUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    /***
     * 拿到osu id值
     * @param name
     * @return
     */
    public int getOsuId(String name) {
        int id = BindingUtil.readOsuID(name);
        if (id == 0) {
            var date = getPlayerOsuInfo(name);
            id = date.getIntValue("id");
            BindingUtil.writeOsuID(date.getString("username"), id);
        }
        return id;
    }

    public JSONArray getFrendList(BinUser user) {
        String url = this.url + "friends";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + user.getAccessToken(this));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JSONArray> c = template.exchange(url, HttpMethod.GET, httpEntity, JSONArray.class);
        return c.getBody();
    }

    /***
     * 拿到详细的个人信息
     * @param user
     * @return
     */
    public JSONObject getPlayerOsuInfo(BinUser user) {
        return getPlayerInfo(user, "osu");
    }
    public JSONObject getPlayerTaikoInfo(BinUser user) {
        return getPlayerInfo(user, "taiko");
    }
    public JSONObject getPlayerCatchInfo(BinUser user) {
        return getPlayerInfo(user, "fruits");
    }

    public JSONObject getPlayerInfo(BinUser user, String mode) {
        String url = this.url + "me" + '/' + mode;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + user.getAccessToken(this));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED));
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JSONObject> c = template.exchange(url, HttpMethod.GET, httpEntity, JSONObject.class);
        user.setOsuID(c.getBody().getIntValue("id"));
        user.setOsuName(c.getBody().getString("username"));
        return c.getBody();
    }

    /***
     * 使用本机token获取user信息
     * @param id
     * @return
     */
    public JSONObject getPlayerOsuInfo(int id) {
        return getPlayerInfo(id, "osu");
    }
    public JSONObject getPlayerTaikoInfo(int id) {
        return getPlayerInfo(id, "taiko");
    }
    public JSONObject getPlayerCatchInfo(int id) {
        return getPlayerInfo(id, "fruits");
    }


    public JSONObject getPlayerInfo(int id, String mode) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.url + "users/" + id + '/' + mode)
                .queryParam("key", "id")
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + getToken());

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JSONObject> c = template.exchange(uri, HttpMethod.GET, httpEntity, JSONObject.class);
        return c.getBody();
    }

    /***
     * 使用本机token获取user信息
     * @param name
     * @return
     */
    public JSONObject getPlayerOsuInfo(String name) {
        return getPlayerInfo(name, "osu");
    }

    public JSONObject getPlayerInfo(String name, String mode) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.url + "users/" + name + '/' + mode)
                .queryParam("key", "name")
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + getToken());

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JSONObject> c = template.exchange(uri, HttpMethod.GET, httpEntity, JSONObject.class);
        return c.getBody();
    }

    /***
     * 获得个人bp
     * @param user
     * @param s 开始坐标，最小为0
     * @param e 列表长度 1-100之间
     * @return
     */
    public JSONArray getOsuBestMap(BinUser user, int s, int e) {
        return getBestMap(user, "osu", s, e);
    }

    public JSONArray getOsuBestMap(int id, int s, int e) {
        return getBestMap(id, "osu", s, e);
    }

    @Deprecated
    public JSONArray getOsuBestMap(String name, int limit) {
        URI uri = UriComponentsBuilder.fromHttpUrl("https://osu.ppy.sh/api/get_user_best")
                .queryParam("k", tokenv1)
                .queryParam("u", name)
                .queryParam("limit", limit)
                .queryParam("type", "string")
                .build().encode().toUri();
        System.out.println(uri.toString());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JSONArray> c = template.exchange(uri, HttpMethod.GET, httpEntity, JSONArray.class);
        return c.getBody();
    }
    public JSONArray getTaikoBestMap(BinUser user, int s, int e) {
        return getBestMap(user, "taiko", s, e);
    }

    public JSONArray getTaikoBestMap(int id, int s, int e) {
        return getBestMap(id, "taiko", s, e);
    }

    public JSONArray getCatchBestMap(BinUser user, int s, int e) {
        return getBestMap(user, "fruits", s, e);
    }

    public JSONArray getCatchBestMap(int id, int s, int e) {
        return getBestMap(id, "fruits", s, e);
    }
    /***
     * 获得某个模式的bp表
     * @param user user
     * @param mode 模式
     * @param s 起始坐标
     * @param e 长度
     * @return
     */
    public JSONArray getBestMap(BinUser user, String mode, int s, int e) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.url + "users/" + user.getOsuID() + "/scores/best")
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

    public JSONArray getBestMap(int id, String mode, int s, int e) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.url + "users/" + id + "/scores/best")
                .queryParam("mode", mode)
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

    public JSONArray getOsuRecent(BinUser user, int s, int e) {
        return getRecent(user, "osu", s, e);
    }

    public JSONArray getRecent(BinUser user, String mode, int s, int e) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.url + "users/" + user.getOsuID() + "/scores/recent")
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

    public JSONArray getOsuRecent(int id, int s, int e) {
        return getRecent(id, "osu", s, e);
    }

    public JSONArray getRecent(int id, String mode, int s, int e) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.url + "users/" + id + "/scores/recent")
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

    public JSONObject getScore(int bid, int id, String mode, String[] mods) {
        var uribulid = UriComponentsBuilder.fromHttpUrl(this.url + "beatmaps/" + bid + "/scores/users/" + id)
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

    public JSONObject getScore(int bid, int id) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.url + "beatmaps/" + bid + "/scores/users/" + id)
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
        URI uri = UriComponentsBuilder.fromHttpUrl(this.url + "beatmaps/" + bid + "/scores/users/" + user.getOsuID())
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

    public String getOsuFile(String mapId) throws IOException {
        return getOsuFile(mapId, "osu");
    }

    public String getOsuFile(String mapId, String mode) throws IOException {
        //osu taiko mania catch
        URL url = new URL("https://osu.ppy.sh/" + mode + '/' + mapId);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.connect();
        InputStream cin = httpConn.getInputStream();
        byte[] datebyte = cin.readAllBytes();
        return new String(datebyte);
    }

    public JSONObject getMapInfo(int bid) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.url + "beatmaps/" + bid)
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

    public JSONObject getMapInfo(int bid, BinUser uset) {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.url + "beatmaps/" + bid)
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + uset.getAccessToken(this));

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

    public Object getReplay() {
        URI uri = UriComponentsBuilder.fromHttpUrl(this.url + "scores/osu/82445750/download")
//                .queryParam("mode","osu")
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + getToken());

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity c = null;
        try {
            System.out.println(uri);
            c = template.exchange(uri, HttpMethod.GET, httpEntity, byte[].class);
        } catch (RestClientException e) {
            e.printStackTrace();
            return null;
        }
        return c.getBody();
    }

    public JSONObject ppPlus(String name) {
        URI uri = UriComponentsBuilder.fromHttpUrl("https://syrin.me/pp+/api/user/" + name)
                .build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<JSONObject> c = null;

        c = template.exchange(uri, HttpMethod.GET, httpEntity, JSONObject.class);

        return c.getBody().getJSONObject("user_data");
    }

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
}

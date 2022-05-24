package com.now.nowbot.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.entity.UserAccountLite;
import com.now.nowbot.mapper.AccountRepository;
import com.now.nowbot.throwable.TipsRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * 下图工具,待实现
 */
@Component
public class OsuMapDownloadUtil {

    AccountRepository accountRepository;
    RestTemplate template;

    private static final String BEATMAP_API_URL = "https://osu.ppy.sh/api/get_beatmaps?k=%s&b=%s";
    private static final String BEATMAP_SET_API_URL = "https://osu.ppy.sh/api/get_beatmaps?k=%s&s=%s";
    private static final String RANKED_BEATMAP_SET_API_URL = "https://osu.ppy.sh/api/get_beatmaps?k=%s&since=%s";
    private static final String HOME_PAGE_URL = "https://osu.ppy.sh/home";
    private static final String LOGIN_URL = "https://osu.ppy.sh/session";
    private static final String DOWNLOAD_URL = "https://osu.ppy.sh/beatmapsets/%s/download";
    @Autowired
    public OsuMapDownloadUtil(AccountRepository accountRepository, RestTemplate restTemplate){
        this.accountRepository = accountRepository;
        this.template = restTemplate;
    }

    record OsuWebSessionBO(String csrfToken, String session){}

    private UserAccountLite getAccount(){
        long count = accountRepository.count();
        if (count != 0){
            return accountRepository.getByIndex(ThreadLocalRandom.current().nextLong(count));
        } throw new RuntimeException("账号池获取账号失败");
    }

    public byte[] download(Long sid, UserAccountLite account){
        HttpHeaders headers = new HttpHeaders();
        headers.set("referer", "https://osu.ppy.sh/beatmapsets/" + sid);
        headers.set("cookie", "osu_session=" + account.getSession());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<byte[]> data = null;
        try {
            data = template.exchange(String.format(DOWNLOAD_URL, sid), HttpMethod.GET, httpEntity, byte[].class);
        } catch (RestClientException e) {
            var homePageSession = visitHomePage();
            login(account, homePageSession);
            HttpHeaders newHeaders = new HttpHeaders();
            newHeaders.set("referer", "https://osu.ppy.sh/beatmapsets/" + sid);
            newHeaders.set("cookie", "osu_session=" + account.getSession());
            newHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity newHttpEntity = new HttpEntity(newHeaders);
            try {
                data = template.exchange(String.format(DOWNLOAD_URL, sid), HttpMethod.GET, newHttpEntity, byte[].class);

                var session = fromCookie(data.getHeaders());
                account.setSession(session.session);
                accountRepository.save(account);
            } catch (RestClientException ex) {
                throw new TipsRuntimeException("下图失败");
            }
        }

        if (data != null){
            return data.getBody();
        }
        return new byte[0];
    }

    private OsuWebSessionBO login(UserAccountLite account, OsuWebSessionBO homePageSession){
        HttpHeaders headers = new HttpHeaders();
        headers.set("referer", HOME_PAGE_URL);
        headers.set("cookie", "XSRF-TOKEN=" + homePageSession.csrfToken() + "; osu_session=" + homePageSession.session());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.set("_token", homePageSession.csrfToken());
        body.set("username", account.getUsername());
        body.set("password", account.getPassword());
        HttpEntity httpEntity = new HttpEntity(body, headers);
        var data = template.exchange(LOGIN_URL, HttpMethod.POST, httpEntity, String.class);

        var nowSession = fromCookie(data.getHeaders());
        account.setSession(nowSession.session());
        accountRepository.save(account);
        return nowSession;
    }

    private OsuWebSessionBO visitHomePage(){
        var response = template.getForEntity(HOME_PAGE_URL, String.class);
        return fromCookie(response.getHeaders());
    }

    private OsuWebSessionBO fromCookie(HttpHeaders headers){
        String token = "";
        String session = "";
        var setCookie = headers.get("set-cookie");
        if (setCookie != null) {
            var pattern = Pattern.compile("(^XSRF-TOKEN=(?<token>[\\w\\d]+);)|(^osu_session=(?<session>[\\w\\d%]+);)");
            for (var str : setCookie){
                var matcher = pattern.matcher(str);
                if (matcher.find()) {
                    if (matcher.group("token") != null && !matcher.group("token").equalsIgnoreCase("deleted")) token = matcher.group("token");
                    else if (matcher.group("session") != null) session = matcher.group("session");
                }
                if (!token.equals("") && !session.equals("")) break;
            }
        }
        return new OsuWebSessionBO(token, session);
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class BeatmapDTO {
    @JsonProperty("beatmapset_id")
    private Integer setId;

    @JsonProperty("beatmap_id")
    private Integer beatmapId;

    private Integer approved;

    @JsonProperty("file_md5")
    private String md5;

    @JsonProperty("last_update")
    private String lastUpdate;

    private Integer timeZone;

    public Integer getSetId() {
        return setId;
    }

    public void setSetId(Integer setId) {
        this.setId = setId;
    }

    public Integer getBeatmapId() {
        return beatmapId;
    }

    public void setBeatmapId(Integer beatmapId) {
        this.beatmapId = beatmapId;
    }

    public Integer getApproved() {
        return approved;
    }

    public void setApproved(Integer approved) {
        this.approved = approved;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Integer getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(Integer timeZone) {
        this.timeZone = timeZone;
    }
}
class OsuWebSessionBO {
    private String csrfToken;
    private String session;

    public String getCsrfToken() {
        return csrfToken;
    }

    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }
}
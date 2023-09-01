package com.now.nowbot.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.entity.UserAccountLite;
import com.now.nowbot.mapper.AccountRepository;
import com.now.nowbot.throwable.TipsRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public UserAccountLite getAccount(){
        long count = accountRepository.count();
        if (count != 0){
            return accountRepository.getByIndex(ThreadLocalRandom.current().nextLong(count));
        } throw new RuntimeException("账号池获取账号失败");
    }

    public InputStream download(Long sid, UserAccountLite account) throws IOException {
        if (account.getAkV1() == null) {
            initAccount(account);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("referer", "https://osu.ppy.sh/beatmapsets/" + sid);
        headers.set("cookie", "osu_session=" + account.getSession());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity httpEntity = new HttpEntity(headers);
        ResponseEntity<Resource> data = null;
        try {
            data = template.exchange(String.format(DOWNLOAD_URL, sid), HttpMethod.GET, httpEntity, Resource.class);
        } catch (RestClientException e) {
            visitHomePage(account);
            login(account);
            HttpHeaders newHeaders = new HttpHeaders();
            newHeaders.set("referer", "https://osu.ppy.sh/beatmapsets/" + sid);
            newHeaders.set("cookie", "osu_session=" + account.getSession());
            newHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity newHttpEntity = new HttpEntity(newHeaders);
            try {
                data = template.exchange(String.format(DOWNLOAD_URL, sid), HttpMethod.GET, newHttpEntity, Resource.class);
            } catch (RestClientException ex) {
                throw new TipsRuntimeException("下图失败");
            }
        }
        var in = data.getBody();
        if (in != null){
            fromCookie(data.getHeaders(), account);
            return in.getInputStream();
        }
        throw new IOException("");
    }

    private void initAccount(UserAccountLite account) {
        visitHomePage(account);
        login(account);
    }

    private void login(UserAccountLite account){
        HttpHeaders headers = new HttpHeaders();
        headers.set("referer", HOME_PAGE_URL);
        headers.set("cookie", "XSRF-TOKEN=" + account.getAkV1() + "; osu_session=" + account.getSession());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.set("_token", account.getAkV1());
        body.set("username", account.getUsername());
        body.set("password", account.getPassword());
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(body, headers);
        var data = template.exchange(LOGIN_URL, HttpMethod.POST, httpEntity, String.class);

        fromCookie(data.getHeaders(), account);
    }

    private void visitHomePage(UserAccountLite account){
        var response = template.getForEntity(HOME_PAGE_URL, String.class);
        fromCookie(response.getHeaders(), account);
    }

    private void fromCookie(HttpHeaders headers, UserAccountLite account){
        String token = "";
        String session = "";
        var setCookie = headers.get("set-cookie");
        if (setCookie != null) {
            var pattern = Pattern.compile("(^XSRF-TOKEN=(?<token>[\\w]+);)|(^osu_session=(?<session>[\\w%]+);)");
            for (var str : setCookie){
                var matcher = pattern.matcher(str);
                if (matcher.find()) {
                    if (matcher.group("token") != null && !matcher.group("token").equalsIgnoreCase("deleted")) token = matcher.group("token");
                    else if (matcher.group("session") != null) session = matcher.group("session");
                }
                if (!token.equals("") && !session.equals("")) break;
            }
        }
        if (!token.isBlank()) {
            account.setAkV1(token);
        }
        if (!session.isBlank()){
            account.setSession(session);
        }
        accountRepository.save(account);
    }
}
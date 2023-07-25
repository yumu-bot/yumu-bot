package com.now.nowbot.service.MessageService;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.throwable.ServiceException.SetuException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;

@Service("setu")
public class SetuService implements MessageService {
    static final Logger log = LoggerFactory.getLogger(SetuService.class);
    Long time = 0L;
    final Object lock = new Object();

    RestTemplate template;

    @Autowired
    SetuService(RestTemplate template) {
        this.template = template;
    }


    @Override
    @CheckPermission(isWhite = true, userSet = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        long qq = event.getSender().getId();

        synchronized (lock) {
            if (time + (15 * 1000) > System.currentTimeMillis()) {
                try {
                    byte[] img;
                    img = Files.readAllBytes(Path.of(NowbotConfig.BG_PATH, "xxoo.jpg"));
                    from.sendImage(img);
                    from.sendMessage("休息一下好不好");
                } catch (IOException e) {
                    throw new SetuException(SetuException.Type.SETU_Send_Error);
                }
                return;
            } else time = System.currentTimeMillis();
        }

        byte[] img = null;
        try {
            byte[] data;
            int random = Math.toIntExact((System.currentTimeMillis() % 5));
            if (random % 2 == 0) {
                data = api3();
            } else {
                data = api2();
            }
            img = data;
        } catch (IOException e) {
            throw new SetuException(SetuException.Type.SETU_Download_Error);
        }

        try {
            from.sendImage(img);
        } catch (Exception e) {
            throw new SetuException(SetuException.Type.SETU_Send_Error);
        }
    }

    public byte[] api3() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        HttpEntity<Byte[]> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> data = null;
        try {
            data = template.exchange("https://www.dmoe.cc/random.php", HttpMethod.GET, httpEntity, byte[].class);
        } catch (Exception e) {
            throw new Exception(e);
        }
        return data.getBody();
    }

    public byte[] api2() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        HttpEntity<Byte[]> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> date = null;
        try {
            date = template.exchange("https://api.vvhan.com/api/acgim", HttpMethod.GET, httpEntity, byte[].class);
        } catch (Exception e) {
            throw new Exception(e);
        }
        return date.getBody();
    }

    public byte[] api1() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        HttpEntity<Byte[]> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> date = null;
        try {
            date = template.exchange("https://iw233.cn/api.php?sort=random", HttpMethod.GET, httpEntity, byte[].class);
        } catch (Exception e) {
            throw new Exception(e);
        }
        return date.getBody();
    }
}

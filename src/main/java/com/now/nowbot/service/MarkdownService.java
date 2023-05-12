package com.now.nowbot.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

@Service
public class MarkdownService {
    @Resource
    RestTemplate restTemplate;
    public byte[] getImage(String markdown){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        var body = Map.of("md",markdown,"width", 1500);
        HttpEntity<Map> httpEntity = new HttpEntity<>(body, headers);
        ResponseEntity<byte[]> s = restTemplate.exchange(URI.create("http://127.0.0.1:1611/md"), HttpMethod.POST, httpEntity, byte[].class);
        return s.getBody();
    }

    /***
     * 宽度是px,最好600以上
     * @param width 宽度
     * @return 图片
     */
    public byte[] getImage(String markdown, int width){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        var body = Map.of("md",markdown,"width", width);
        HttpEntity<Map> httpEntity = new HttpEntity<>(body, headers);
        ResponseEntity<byte[]> s = restTemplate.exchange(URI.create("http://127.0.0.1:1611/md"), HttpMethod.POST, httpEntity, byte[].class);
        return s.getBody();
    }
}

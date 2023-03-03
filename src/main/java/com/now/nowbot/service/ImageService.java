package com.now.nowbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

//@Service("nowbot-image")
public class ImageService {
    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    RestTemplate restTemplate;

    @Autowired
    ImageService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public byte[] getCardH() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            InputStream in = new FileInputStream("/home/spring/Downloads/background.jpg");
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            ByteArrayResource fs = new ByteArrayResource(in.readAllBytes(), "") {
                @Override
                public String getFilename() {
                    return "bg.png";
                }
            };
            form.add("background", fs);
            form.add("title", "title");
            form.add("artist", "artist");
            form.add("info", "info");
            form.add("mod", "mod");
            form.add("star_b", "3");
            form.add("star_m", ".33");
            HttpEntity<MultiValueMap<String, Object>> datas = new HttpEntity<>(form, headers);
            var t = restTemplate.postForEntity("http://localhost:8555/card-d", datas, byte[].class);
            if (t.getStatusCode().is2xxSuccessful()) {
                return t.getBody();
            }
        } catch (IOException e) {
            log.error("File error", e);
        }
        return new byte[0];
    }
}

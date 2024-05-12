package com.now.nowbot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.config.FileConfig;
import com.now.nowbot.mapper.BeatMapFileRepository;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.util.JacksonUtil;
import com.now.nowbot.util.OsuMapDownloadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;

// qnmd, 瞎 warning
@SuppressWarnings("all")
// ??

public class OsuGetServiceImpl {
    private static final Logger log = LoggerFactory.getLogger(OsuGetServiceImpl.class);


    RestTemplate template;

    FileConfig fileConfig;

    OsuMapDownloadUtil osuMapDownloadUtil;

    BeatMapFileRepository beatMapFileRepository;

    /*************************************
     * 下载replay文件 字节文件
     * @param mode
     * @param id
     * @return
     */

    public byte[] getReplay(String mode, long id) {
        URI uri = UriComponentsBuilder.fromHttpUrl("scores/" + mode + "/" + id + "/download")
//                .queryParam("mode","osu")
                .build().encode().toUri();
        HttpHeaders headers = null;

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





    public JsonNode chatGetChannels(BinUser user) {
        HashMap body = new HashMap<>();
        body.put("target_id", 7003013);
        body.put("message", "osuMode.getModeValue()");
        body.put("is_action", false);
        HttpEntity httpEntity = new HttpEntity(JacksonUtil.objectToJson(body), null);
        var res = template.exchange("/chat/new", HttpMethod.POST, httpEntity, JsonNode.class);
        return res.getBody();
    }

}

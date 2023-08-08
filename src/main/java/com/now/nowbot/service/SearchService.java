package com.now.nowbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SearchService {
    RestTemplate restTemplate;

    static final String BASE_URL = "";
    @Autowired
    SearchService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }



    <T>T doPost (String api, HttpHeaders headers, Object body, Class<T> clazz){
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        var resp = restTemplate.exchange(BASE_URL + api, HttpMethod.POST, entity, clazz);
        return resp.getBody();
    }
}

package com.now.nowbot.throwable;

import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class RequestError extends IOException {
    public ClientHttpResponse response;
    public HttpStatus status;
    public JSONObject message;

    public RequestError(ClientHttpResponse response, HttpStatus status){
        try {
            message = JSONObject.parseObject(response.getBody().readAllBytes(), JSONObject.class);
        } catch (IOException e) {
            message = new JSONObject();
        }
        this.response = response;
        this.status = status;
    }
}

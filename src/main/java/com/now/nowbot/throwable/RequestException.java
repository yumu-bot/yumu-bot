package com.now.nowbot.throwable;

import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RequestException extends IOException {
    public ClientHttpResponse response;
    public HttpStatus status;
    public String message;

    public RequestException(ClientHttpResponse response, HttpStatus status){
        try {

            message = new String(response.getBody().readAllBytes());
        } catch (IOException e) {
            message = "none body";
        }
        this.response = response;
        this.status = status;
    }
}

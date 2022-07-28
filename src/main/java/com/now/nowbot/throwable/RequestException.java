package com.now.nowbot.throwable;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RequestException{");
        sb.append("response=").append(response);
        sb.append(", status=").append(status);
        sb.append(", message='").append(message).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

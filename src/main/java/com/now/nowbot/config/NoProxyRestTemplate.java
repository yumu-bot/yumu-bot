package com.now.nowbot.config;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class NoProxyRestTemplate extends RestTemplate {
    public NoProxyRestTemplate(ClientHttpRequestFactory requestFactory) {
        super();
        setRequestFactory(requestFactory);
    }

}

package com.now.nowbot.config;

import org.springframework.web.socket.server.standard.ServerEndpointExporter;

//@Configuration
public class WebsocketConfig{
//    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}

package com.now.nowbot.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.now.nowbot.service.MessageService.BindService;
import net.mamoe.mirai.Bot;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class WsController extends WebSocketListener {
    private static Logger log = LoggerFactory.getLogger(WsController.class);
    static Bot bot;
    static WsController ws;

    static OkHttpClient client = new OkHttpClient.Builder()
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .build();
    static Request req = new Request.Builder().url("ws://1.116.209.39:20007").build();
    WebSocket webSocket;

    msgController msgController;

    public static WsController getInstance(Bot bot){
        WsController.bot = bot;
        if (ws != null){
            return ws;
        }

        ws = new WsController();
        client.newWebSocket(req, ws);
        return ws;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        log.info("ws link:{}", response.code());
        bot.getGroup(746671531).sendMessage("绑定服务已连接");
        this.webSocket = webSocket;
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        super.onMessage(webSocket, text);
        log.info("receive ws message:{}", text);
        var om = new ObjectMapper();
        try {
            var data = om.readTree(text);
            if (!data.has("state") || !data.has("code") || !data.has("echo")){
                log.info("error:argument error");
            }
            var state = data.get("state").asText().split(" ");
            var code = data.get("code").asText();
            var echo = data.get("echo").asText();
            try {
                var l = Long.parseLong(state[1]);
                if (state.length != 2 || !BindService.BIND_MSG_MAP.containsKey(l)) {
                    // 不响应任何
                    log.error("no find key");
                    return;
                }
            } catch (NumberFormatException e) {
                log.error("parse error",e);
                return;
            }
            if (msgController != null){
                var resp = msgController.saveBind(code, state[1]);
                var p = new HashMap<String, String>();
                p.put("response", resp);
                p.put("echo", echo);
                webSocket.send(om.writeValueAsString(p));
                log.info("bind over -> {}", resp);
            } else {
                log.error("ws error:init");
            }
        } catch (JsonProcessingException e) {
            log.error("ws error:not json");
        }
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        super.onClosed(webSocket, code, reason);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("ws 重连中");
        log.error("{}\n{}",code ,reason);
        bot.getGroup(746671531).sendMessage("绑定服务已断开...");
        client.newWebSocket(req, this);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        super.onFailure(webSocket, t, response);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("ws 重连中", t);
        log.error("{}",response.body());
        bot.getGroup(746671531).sendMessage("绑定服务已断开...");
        client.newWebSocket(req, this);
    }

    public void setMsgController(com.now.nowbot.controller.msgController msgController) {
        this.msgController = msgController;
    }

}

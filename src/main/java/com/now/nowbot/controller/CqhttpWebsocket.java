package com.now.nowbot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.util.JacksonUtil;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

//@ServerEndpoint("/websocket")
//@RestController
public class CqhttpWebsocket {
    private static final Logger log = LoggerFactory.getLogger(CqhttpWebsocket.class);

    /**
     * 连接数
     */
    private static volatile int SocketConut = 0;
    /**
     * 所有的链接
     */
    private static final CopyOnWriteArraySet<CqhttpWebsocket> websockets = new CopyOnWriteArraySet<>();
    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        session.addMessageHandler(String.class, message -> {
            log.info("string: {}", message);
        });
        session.addMessageHandler(byte[].class, message -> {
            try {
                var node = JacksonUtil.parseObject(message, JsonNode.class);
                if (node != null) {
                    log.info("json: {}", node.toString());
                }
            } catch (Exception e) {
                log.error("反序列化错误: ", e);
            }
        });
        websockets.add(this);
        addCount();
    }
    @OnClose
    public void onClose() {
        if (websockets.contains(this)) {
            on = false;
            websockets.remove(this);
            subCount();
        }
        System.out.println(session.getId() + " is closed");
    }



    boolean on = true;

    public void sendMessage(byte[] message) throws IOException {
        session.getBasicRemote().getSendStream().write(message);
        session.getBasicRemote().getSendStream().flush();
    }

    public void close() throws IOException {
        websockets.remove(this);
        session.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(1015),"closed"));
    }

    private static synchronized void addCount() {
        SocketConut++;
    }

    private static synchronized void subCount() {
        SocketConut--;
    }
}

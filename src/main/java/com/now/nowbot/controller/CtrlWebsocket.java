package com.now.nowbot.controller;

import com.now.nowbot.util.WebsocketUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/CtrlSocket/{key}/{token}")
@RestController
public class CtrlWebsocket {
    private static final Logger log = LoggerFactory.getLogger(CtrlWebsocket.class);

    /**
     * 连接数
     */
    private static volatile int SocketConut = 0;
    /**
     * 所有的链接
     */
    private static final CopyOnWriteArraySet<CtrlWebsocket> websockets = new CopyOnWriteArraySet<>();
    private Session session;

    @OnOpen
    public void onOpen(Session session, @PathParam("key") String key, @PathParam("token") String token) throws IOException {
        if (WebsocketUtil.login(key, token)) {
            this.session = session;
            websockets.add(this);
            addCount();
            pc();
        } else {
            log.error("no link --" + key + "--" + token);
            session.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(1015), "link:'" + key + "' ,'" + token + "' not allow"));
        }
    }
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            session.getBasicRemote().sendText("sd-"+message);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    @Async
    void pc() {
        while (on) {
            try {
                Thread.sleep(1000);
                if (on)
                    session.getBasicRemote().sendText("💕~");
                else
                    return;
            } catch (InterruptedException | IOException e) {
                //do nothing
                return;
            }
        }
    }

    private static synchronized void addCount() {
        SocketConut++;
    }

    private static synchronized void subCount() {
        SocketConut--;
    }
}

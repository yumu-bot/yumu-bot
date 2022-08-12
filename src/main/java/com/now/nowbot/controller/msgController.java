package com.now.nowbot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.service.MessageService.BindService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.UpdateUtil;
import net.mamoe.mirai.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping(produces = "application/json;charset=UTF-8")
public class msgController {
    public static final boolean debug = false;
    static final Logger log = LoggerFactory.getLogger(msgController.class);
    static final DateTimeFormatter format = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");
    OsuGetService osuGetService;
    Bot bot;
    BindDao bindDao;

    @Autowired
    public msgController(Bot bot, OsuGetService osuGetService, BindDao dao) {
        this.bot = bot;
        this.osuGetService = osuGetService;
        bindDao = dao;
        WsController.getInstance().setMsgController(this);
    }

    @GetMapping("${osu.callbackpath}")
    public String bind(@RequestParam("code") String code, @RequestParam("state") String stat) {
        var data = stat.split(" ");
        if (data.length != 2) {
            return "噶?";
        }
        return saveBind(code, data[1]);
    }

    public String saveBind(String code, String keyStr) {
        StringBuilder sb = new StringBuilder();

        long key = 0;
        try {
            key = Long.parseLong(keyStr);
        } catch (NumberFormatException e) {
            sb.append("非法的访问:参数异常")
                    .append(e.getMessage());
        }

        var msg = BindService.BIND_MSG_MAP.get(key);
        if (debug || msg != null) {
            try {
                if (!debug) {
                    try {
                        msg.receipt().recall();
                    } catch (Exception e) {
                        log.error("绑定消息撤回失败错误,一般为已经撤回(超时/管理撤回)", e);
                        sb.append("绑定连接已超时\n请重新绑定");
                        return sb.toString();
                    }
                }
                BinUser bd = new BinUser(msg.qq(), code);
                osuGetService.getToken(bd);
                if (!debug) {
                    msg.receipt().getTarget().sendMessage("成功绑定:" + bd.getQq() + "->" + bd.getOsuName());
                }
                BindService.BIND_MSG_MAP.remove(key);
                sb.append("成功绑定:\n")
                        .append(bd.getQq())
                        .append('>')
                        .append(bd.getOsuName());
            } catch (Exception e) {
                log.error("绑定时异常", e);
                sb.append("出现异常,请截图给开发者让他抓紧修bug\n")
                        .append(e.getLocalizedMessage());
            }
        } else {
            sb.append("绑定链接失效\n请重新绑定");
        }
        return sb.toString();
    }

    @PostMapping("/api")
    public String opa(@RequestHeader("state") @Nullable String stat,
                      @RequestBody @Nullable JsonNode body) {
        String[] data;
        String code;
        try {
            data = stat.split(" ");
            code = body.get("code").asText();
        } catch (NullPointerException e) {
            return e.getMessage();
        }
        if (data.length != 2) {
            return "槲?";
        }

        var ret = saveBind(code, data[1]);
        log.info("绑定api端口被访问,参数: state->{} code->{}:{}", stat, code, ret);
        return ret;
    }

    @PostMapping("/gitup")
    public void update(@RequestBody JsonNode body) {
        log.info("收到一条推送\n{}", body.toString());
        String name = body.findValue("user_name").asText("unknown user");
        String msg = body.findValue("title").asText("nothing");
        var gp = bot.getGroup(746671531L);
        if (gp != null) {
            gp.sendMessage("git收到" + name + "推送\n" + msg);
            if (msg.startsWith("update")) {
                try {
                    UpdateUtil.update();
                } catch (IOException e) {
                    gp.sendMessage("error");
                }
            }
        }
    }
}

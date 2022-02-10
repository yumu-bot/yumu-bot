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

import java.io.IOException;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping(produces = "application/json;charset=UTF-8")
public class msgController {
    static final Logger log = LoggerFactory.getLogger(msgController.class);
    static final DateTimeFormatter format = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");
    OsuGetService osuGetService;
    Bot bot;
    BindDao bindDao;
    @Autowired
    public msgController(Bot bot, OsuGetService osuGetService, BindDao dao){
        this.bot = bot;
        this.osuGetService = osuGetService;
        bindDao = dao;
    }
    @GetMapping("${osu.callbackpath}")
    public String bind(@RequestParam("code")String code, @RequestParam("state") String stat){
        var data = stat.split(" ");
        return saveBind(code, data);
    }
    public String saveBind(String code, String[] date) {
        StringBuffer sb = new StringBuffer();
        if (date.length == 2) {
            long key = 0;
            try {
                key = Long.parseLong(date[1]);
            } catch (NumberFormatException e) {
                sb.append("非法的访问:参数异常")
                        .append(e.getMessage());
            }
            var msg = BindService.BIND_MSG_MAP.get(key);
            if (msg != null) {
                try {
                    try {
                        msg.receipt().recall();
                    } catch (Exception e) {
                        log.error("绑定消息撤回失败错误,一般为已经撤回(超时/管理撤回)", e);
                        sb.append("绑定连接已超时\n请重新绑定");
                        return  sb.toString();
                    }
                    BinUser bd = new BinUser(msg.qq(), code);
                    osuGetService.getToken(bd);
                    msg.receipt().getTarget().sendMessage("成功绑定:" + bd.getQq() + "->" + bd.getOsuName());
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
        } else {
            sb.append("非法的访问\n连接异常,确认是否为绑定链接");
        }
        return sb.toString();
    }
    @PostMapping("/api")
    public String opa(@RequestHeader("state") @Nullable String stat,
                      @RequestBody @Nullable JsonNode body){
        var date = stat.split(" ");
        var code = body.get("code").asText();
        var ret = saveBind(code, date);
        log.info("绑定api端口被访问,参数: state->{} code->{}:{}",stat,code,ret);
        return ret;
    }
    @PostMapping("/gitup")
    public void update(@RequestHeader("User-Agent") String agent, @RequestHeader("X-Gitee-Event") String event, @RequestBody JsonNode body){
        if (agent.trim().equals("git-oschina-hook")){
            log.info("收到一条推送\n{}",body.toString());
            String msg = body.findValue("message").asText("nothing");
            var gp = bot.getGroup(746671531L);
            if (gp != null) {
                gp.sendMessage("git收到推送事件 " + event + "\n" + msg);
                if (msg.startsWith("update")) {
                    try {
                        gp.sendMessage("即将更新重启...");
                        UpdateUtil.update();
                    } catch (IOException e) {
                        gp.sendMessage("error");
                    }
                }
            }
        }
    }
}

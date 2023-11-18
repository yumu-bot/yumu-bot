package com.now.nowbot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.mikuac.shiro.core.BotContainer;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.service.MessageServiceImpl.BindService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.util.UpdateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@RestController
@RequestMapping(produces = "application/json;charset=UTF-8")
@ConditionalOnProperty(value = "osu.callbackpath")
public class BindController {
    public static final boolean debug = false;
    static final Logger log = LoggerFactory.getLogger(BindController.class);
    static final DateTimeFormatter format = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");
    OsuUserApiService userApiService;
    private BotContainer botContainer;
    BindDao bindDao;

    @Autowired
    public BindController(OsuUserApiService userApiService, BindDao dao, BotContainer botContainer) {
        this.userApiService = userApiService;
        bindDao = dao;
        this.botContainer = botContainer;
//        WsController.getInstance(bot).setMsgController(this);
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

        var msg = BindService.getBind(key);
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
                BinUser bd = BinUser.create(code);
                userApiService.refreshUserTokenFirst(bd);
                bindDao.bindQQ(msg.QQ(), bd);
                BindService.removeBind(key);
                sb.append("成功绑定:\n")
                        .append(msg.QQ())
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
        Group gp = null;
        if (gp != null) {
            var sb = new StringBuilder();
            sb.append("git收到").append(name).append("推送\n").append(msg);
            try {
                var added = body.findValue("added");
                var modified = body.findValue("modified");
                var removed = body.findValue("removed");

                if (added.isArray() && added.size() > 0) {
                    sb.append("\n新增文件:\n");
                    for (var x : added) {
                        sb.append(getEndFile(x.asText("null"))).append('\n');
                    }
                }
                if (modified.isArray() && modified.size() > 0) {
                    sb.append("\n变更文件:\n");
                    for (var x : modified) {
                        sb.append(getEndFile(x.asText("null"))).append('\n');
                    }
                }
                if (removed.isArray() && removed.size() > 0) {
                    sb.append("\n删除文件:\n");
                    for (var x : removed) {
                        sb.append(getEndFile(x.asText("null"))).append('\n');
                    }
                }
            } catch (Exception e) {
                // do nothing
            }
            sb.delete(sb.length()-1, sb.length());
            gp.sendMessage(sb.toString());
            if (msg.startsWith("update")) {
                try {
                    UpdateUtil.update();
                } catch (IOException e) {
                    gp.sendMessage("error");
                }
            }
        }
    }
    private Pattern p = Pattern.compile("(\\w+/)+(?<file>[\\w.]+)");

    String getEndFile(String path) {
        var m = p.matcher(path);
        if (m.find()){
            return m.group("file");
        }

        return "not file";
    }
}

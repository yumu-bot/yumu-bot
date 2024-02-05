package com.now.nowbot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.service.MessageServiceImpl.BindService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;

@RestController
@RequestMapping(produces = "application/json;charset=UTF-8")
@ConditionalOnProperty(value = "yumu.osu.callbackPath")
public class BindController {
    public static final boolean DEBUG = false;
    static final Logger log = LoggerFactory.getLogger(BindController.class);
    OsuUserApiService userApiService;
    BindDao bindDao;

    @Autowired
    public BindController(OsuUserApiService userApiService, BindDao dao) {
        this.userApiService = userApiService;
        bindDao = dao;
    }

    @GetMapping("${yumu.osu.callbackPath}")
    public String bind(@RequestParam("code") String code, @RequestParam("state") String stat) {
        var data = stat.split(" ");
        if (data.length != 2) {
            return "噶?";
        }
        return saveBind(code, data[1]);
    }

    public String saveBind(String code, String keyStr) {
        StringBuilder sb = new StringBuilder();

        long key;
        try {
            key = Long.parseLong(keyStr);
        } catch (NumberFormatException e) {
            sb.append("非法访问：参数异常")
                    .append(e.getMessage());
            return sb.toString();
        }

        var msg = BindService.getBind(key);
        if (DEBUG) {
            return doBind(sb, code, msg, key);
        }

        if (msg == null) {
            return """
                    绑定链接失效。
                    请重新绑定。
                    当然也有可能你已经绑好了。出去可以试试功能。
                    """;
        } else {
            try {
                msg.receipt().recall();
            } catch (Exception e) {
                log.error("绑定消息撤回失败", e);
                return """
                        绑定连接已超时。
                        请重新绑定。
                        """;
            }
        }

        return doBind(sb, code, msg, key);
    }

    private String doBind(StringBuilder sb, String code, BindService.Bind msg, long key) {
        try {

            BinUser bd = BinUser.create(code);
            userApiService.refreshUserTokenFirst(bd);
            var u = bindDao.bindQQ(msg.QQ(), bd);
            BindService.removeBind(key);
            sb.append("成功绑定:\n<br/>")
                    .append(msg.QQ())
                    .append(" -> ")
                    .append(bd.getOsuName())
                    .append("\n<br/>")
                    .append("您的默认游戏模式为：[")
                    .append(u.getOsuUser().getMainMode().getName()).append("]。")
                    .append("\n<br/>")
                    .append("如果您不是主模式 [osu] 玩家，请使用 `!ymmode [mode]` 来修改默认模式。否则可能会影响您查询成绩。")
                    .append("\n<br/>")
                    .append("[mode]：0 osu(standard)，1 taiko，2 catch，3 mania")
            ;
        } catch (HttpClientErrorException.BadRequest | WebClientResponseException.BadRequest e) {
            log.error("绑定时异常：400", e);
            sb.append("出现异常。但您大概已经绑定成功。这可能是回执的问题。")
                    .append('\n')
                    .append(e.getLocalizedMessage());
        } catch (Exception e) {
            log.error("绑定时异常：未知", e);
            sb.append("出现未知异常，请截图给开发者让他抓紧修 BUG。错误代码和信息：")
                    .append('\n')
                    .append(e.getLocalizedMessage());
        }
        return sb.toString();
    }

    @PostMapping("/api")
    public String opa(@RequestHeader("state") @Nullable String stat,
                      @RequestBody @Nullable JsonNode body) {
        String[] data = null;
        String code = null;

        try {
            if (Objects.nonNull(stat)) data = stat.split(" ");
            if (Objects.nonNull(body)) code = body.get("code").asText();
        } catch (NullPointerException e) {
        return e.getMessage();
    }

        /*
        try {
            if (stat != null) {
                data = stat.split(" ");
            }
            if (body != null) {
                code = body.get("code").asText();
            }
        } catch (NullPointerException e) {
            return e.getMessage();
        }

         */

        if (Objects.isNull(data) || data.length != 2) {
            return "蛤";
        }

        var ret = saveBind(code, data[1]);
        log.info("绑定api端口被访问,参数: state->{} code->{}:{}", stat, code, ret);
        return ret;
    }

    @PostMapping("/gitup")
    public void update(@RequestBody JsonNode body) {
        log.info("收到一条推送\n{}", body.toString());
    }
}

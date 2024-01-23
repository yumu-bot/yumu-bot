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
            sb.append("非法的访问:参数异常")
                    .append(e.getMessage());
            return sb.toString();
        }

        var msg = BindService.getBind(key);
        if (DEBUG) {
            return doBind(sb, code, msg, key);
        }

        if (msg == null) {
            return """
                    绑定链接失效
                    请重新绑定。
                    当然也有可能你已经绑好了。出去可以试试功能。
                    """;
        } else {
            try {
                msg.receipt().recall();
            } catch (Exception e) {
                log.error("绑定消息撤回失败错误,一般为已经撤回(超时/管理撤回)", e);
                return """
                        绑定连接已超时。
                        请重新绑定
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
                    .append('>')
                    .append(bd.getOsuName())
                    .append("\n<br/>")
                    .append("已经默认绑定为[")
                    .append(u.getOsuUser().getMainMode().getName()).append("]模式")
                    .append("\n<br/>")
                    .append("可以使用 `!ymmode [mode]` 来修改默认模式")
            ;
        } catch (Exception e) {
            log.error("绑定时异常", e);
            sb.append("出现异常,请截图给开发者让他抓紧修bug\n")
                    .append(e.getLocalizedMessage());
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
    }
}

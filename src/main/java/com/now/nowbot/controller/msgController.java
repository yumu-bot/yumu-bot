package com.now.nowbot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.service.MessageService.BindService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.message.MessageReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping(produces = "application/json;charset=UTF-8")
public class msgController {
    static final Logger log = LoggerFactory.getLogger(msgController.class);
    static final DateTimeFormatter format = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");
    @Autowired
    OsuGetService osuGetService;
    @Autowired
    ThreadPoolTaskExecutor threadPool;

    @RequestMapping("${ppy.callbackpath}")
    @ResponseBody
    //@RequestParam(name = "code") String code, @RequestParam(name = "state") String state
    public Object request(HttpServletRequest request) {
        log.info("{}:来源{} 访问绑定端口\n{}", format.format(LocalDateTime.now()), request.getRemoteAddr(), request.getRemoteUser());
        String[] date = new String[0];
        String code = null;
        try {
            date = request.getParameter("state").split(" ");
            code = request.getParameter("code");
        } catch (Exception e) {
            log.error("访问缺少必要参数", e);
            return "非法的访问,";
        }
        var returndata = saveBind(code, date);
        return returndata;
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
            MessageReceipt msg = BindService.BIND_MSG_MAP.get(key);

            if (msg != null) {
                try {
                    try {
                        msg.recall();
                    } catch (Exception e) {
                        log.error("绑定消息撤回失败错误,一般为已经撤回(超时/管理撤回)", e);
                        sb.append("怎么才来,超时了哦");
                    }
                    BinUser bd = new BinUser(Long.parseLong(date[0]), code);
                    osuGetService.getToken(bd);
                    osuGetService.getPlayerOsuInfo(bd);
                    BindingUtil.writeUser(bd);
                    BindingUtil.writeOsuID(bd.getOsuName(), bd.getOsuID());
                    msg.getTarget().sendMessage("成功绑定:" + bd.getQq() + "->" + bd.getOsuName());
                    BindService.BIND_MSG_MAP.remove(Long.valueOf(date[1]));
                    sb.append("成功绑定:")
                            .append(bd.getQq())
                            .append('>')
                            .append(bd.getOsuName());
                } catch (Exception e) {
                    log.error("绑定时异常", e);
                    sb.append("出现异常,请截图给开发者让他抓紧修bug\n")
                    .append(e.getLocalizedMessage());
                }
            } else {
                long secend = Long.parseLong(date[1]) / 1000;
                log.info("超时已被清理的绑定器\n超时时间:{}\n", LocalDateTime.ofEpochSecond(secend, 0, ZoneOffset.ofHours(8)));
                sb.append("绑定链接已失效,请重新绑定\n请勿重复绑定!请勿重复绑定!请勿重复绑定!");
            }

        } else {
            sb.append("非法的访问:连接异常,确认是否为绑定链接");
        }
        return sb.toString();
    }
    @PostMapping("/api")
    public Object opa(@RequestHeader("state") @Nullable String stat,
                      @RequestBody @Nullable JsonNode body){
        var date = stat.split(" ");
        var code = body.get("code").asText();
        var ret = saveBind(code, date);
        log.info("绑定api端口被访问:{}",ret);
        return ret;
    }
}

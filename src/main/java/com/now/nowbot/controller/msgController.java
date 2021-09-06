package com.now.nowbot.controller;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.entity.BinUser;
import com.now.nowbot.service.MessageService.bindService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = "application/json;charset=UTF-8")
public class msgController {
    bindService bin;

    @Autowired
    OsuGetService osuGetService;
    @Autowired
    ThreadPoolTaskExecutor threadPool;

    @RequestMapping("${ppy.callbackpath}")
    @ResponseBody
    public Object request(@RequestParam(name = "code") String code, @RequestParam(name = "state") String state) {
        String[] data = state.split(" ");
        return saveBind(code, data);
    }

    public String saveBind(String code, String[] data) {
        if (bin == null) {
            bin = NowbotConfig.applicationContext.getBean("bind", bindService.class);
        }
            if (data.length == 2) {

                var msg = bin.msgs.get(Long.valueOf(data[1]));

                if (msg != null) {
                    try {
                        msg.recall();
                        BinUser bd = new BinUser(Long.parseLong(data[0]), code);
                        osuGetService.getToken(bd);
                        osuGetService.getPlayerOsuInfo(bd);
                        BindingUtil.writeUser(bd);
                        BindingUtil.writeOsuID(bd.getOsuName(), bd.getOsuID());
                        msg.getTarget().sendMessage("成功绑定:" + bd.getQq() + "->" + bd.getOsuName());
                        bin.msgs.remove(Long.valueOf(data[1]));
                        return "成功绑定:" + bd.getQq() + "->" + bd.getOsuName();
                    } catch (Exception e) {
                        return "出现异常,请截图给开发者让他抓紧修bug\n" + e.getLocalizedMessage();
                    }
                } else {
                    return "绑定链接已失效,请重新绑定,请勿重复绑定";
                }

            }
            return "别试了,没用的";

    }
}

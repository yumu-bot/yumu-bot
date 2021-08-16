package com.now.nowbot.controller;

import com.now.nowbot.entity.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.service.msgServiceImpl.BindingServiceImpl;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.Bot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = "application/json;charset=UTF-8")
public class msgController {
    @Autowired
    BindingServiceImpl bin;
    @Autowired
    Bot bot;
    @Autowired
    OsuGetService osuGetService;
    @Autowired
    ThreadPoolTaskExecutor threadPool;
    @RequestMapping("${ppy.callbackpath}")
    @ResponseBody
    public Object request(@RequestParam(name = "code") String code, @RequestParam(name = "state")String state){
        String[] data = state.split(" ");
        saveBind(code, data);
        return data[0]+"接收成功！";
    }
    @Async
    public void saveBind(String code, String[] data){
        if(data.length == 2) {
            BinUser bd = new BinUser(Long.valueOf(data[0]), code);
            osuGetService.getToken(bd);
            osuGetService.getPlayerOsuInfo(bd);
            BindingUtil.writeUser(bd);
            BindingUtil.writeOsuID(bd.getOsuName(), bd.getOsuID());
            var msg = bin.msgs.get(Long.valueOf(data[1]));
            if (msg != null) {
                msg.getTarget().sendMessage("成功绑定:" + bd.getQq() + "->" + bd.getOsuName());
                msg.recall();
            }

        }
    }
}

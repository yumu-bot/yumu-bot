package com.now.nowbot.controller;

import com.now.nowbot.entity.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.Bot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = "application/json;charset=UTF-8")
public class msgController {
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
        threadPool.execute(()->{
            if(data.length != 0) {
                BinUser bd = new BinUser(Long.valueOf(data[0]), code);
                osuGetService.getToken(bd);
                osuGetService.getPlayerInfo(bd);
                BindingUtil.writeUser(bd);
                BindingUtil.writeOsuID(bd.getOsuName(),bd.getOsuID());
                if (data.length == 2){
                    bot.getGroup(Long.valueOf(data[1])).sendMessage("成功绑定:" + bd.getQq() + "->" + bd.getOsuName());
                }else
                    bot.getFriend(Long.valueOf(data[0])).sendMessage("成功绑定:" + bd.getQq() + "->" + bd.getOsuName());
            }
        });
        return data[0]+"成功绑定！";
    }
}

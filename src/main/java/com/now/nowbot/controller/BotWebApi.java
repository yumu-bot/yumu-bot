package com.now.nowbot.controller;

import com.now.nowbot.model.PPm.Ppm;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping(value = "/pub", method = RequestMethod.GET)
public class BotWebApi {
    @Resource
    OsuGetService osuGetService;
    @Resource
    ImageService  imageService;

    @GetMapping(value = "ppm", produces = {MediaType.IMAGE_PNG_VALUE})
    public byte[] getPPM(@RequestParam("u1") String user1, @Nullable @RequestParam("u2") String user2, @Nullable @RequestParam("mode") String playMode) {
        if (user2 != null) {
            return getPPMVS(user1, user2, playMode);
        }
        var mode = OsuMode.getMode(playMode);
        var info = osuGetService.getPlayerInfo(user1);
        if (mode == OsuMode.DEFAULT) mode = info.getPlayMode();
        var bplist = osuGetService.getBestPerformance(info.getId(), mode, 0, 100);
        var ppm = Ppm.getInstance(mode, info, bplist);
        return imageService.getPanelB(info, mode, ppm);
    }

    @GetMapping(value = "ppmvs", produces = {MediaType.IMAGE_PNG_VALUE})
    public byte[] getPPMVS(@RequestParam("u1") String user1, @RequestParam("u2") String user2, @Nullable @RequestParam("mode") String playMode) {
        var mode = OsuMode.getMode(playMode);
        var info1 = osuGetService.getPlayerInfo(user1);
        var info2 = osuGetService.getPlayerInfo(user2);
        if (mode == OsuMode.DEFAULT) mode = info1.getPlayMode();
        var bplist1 = osuGetService.getBestPerformance(info1.getId(), mode, 0, 100);
        var bplist2 = osuGetService.getBestPerformance(info2.getId(), mode, 0, 100);
        var ppm1 = Ppm.getInstance(mode, info1, bplist1);
        var ppm2 = Ppm.getInstance(mode, info2, bplist2);
        return imageService.getPanelB(info1, info2, ppm1, ppm2, mode);
    }
}

package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BeatMapDao;
import com.now.nowbot.mapper.BeatMapPoolMapper;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.OsuGetService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.regex.Matcher;

@Service("map")
public class MapPoolService implements MessageService{
    @Resource
    OsuGetService osuGetService;
    @Resource
    BeatMapDao beatMapDao;
    @Resource
    BeatMapPoolMapper beatMapPoolMapper;


    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {

    }
}

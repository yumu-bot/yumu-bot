package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BeatMapDao;
import com.now.nowbot.mapper.BeatMapPoolMapper;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service("MAPPOOL")
public class MapPoolService implements MessageService<Object> {
    @Resource
    OsuGetService osuGetService;
    @Resource
    BeatMapDao beatMapDao;
    @Resource
    BeatMapPoolMapper beatMapPoolMapper;

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Object> data) throws Exception {
        return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Object obj) throws Throwable {

    }
}

package com.now.nowbot.util;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.throwable.TipsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BindingUtil {
    private static final Logger log = LoggerFactory.getLogger(BindingUtil.class);

    public static BinUser readUser(long qq) throws TipsException, IOException {
        Path pt = Path.of(NowbotConfig.BIN_PATH + qq + ".json");
        BinUser date = null;
        if (Files.isRegularFile(pt)) {
            String s = Files.readString(pt);
            date = JSONObject.parseObject(s, BinUser.class);

        }
        if (date == null) throw new TipsException("当前用户未绑定");
        return date;
    }

}

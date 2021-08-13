package com.now.nowbot.util;

import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;

public class MiraiCodeUtil {
    public static MessageChain creatMsg(Message... msgs){
        MessageChainBuilder chain = new MessageChainBuilder();
        for(Message msg : msgs){
            chain.append(msg);
        }
        return chain.build();
    }
}

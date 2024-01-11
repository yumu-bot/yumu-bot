package com.now.nowbot.qq.message;

import com.mikuac.shiro.common.utils.MsgUtils;

public class AtMessage extends Message {
    private final long qq;

    public AtMessage(long qq) {
        this.qq = qq;
    }

    /***
     * 创建 &#064;全体成员 消息,但是只能 bot 为管理员使用
     */
    public AtMessage() {
        this.qq = -1L;
    }

    public boolean isAll() {
        return qq == -1L;
    }

    public long getQQ() {
        return qq;
    }
    public long getTarget() {
        return getQQ();
    }

    @Override
    public String toString() {
        return "[@" + qq + "]";
    }

    @Override
    public String getCQ() {
        return new MsgUtils().at(qq).build();
    }
}

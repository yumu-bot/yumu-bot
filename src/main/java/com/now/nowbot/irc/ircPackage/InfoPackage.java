package com.now.nowbot.irc.ircPackage;

import com.now.nowbot.irc.IRCMessagePackage;

public class InfoPackage extends IRCMessagePackage {
    /**
     * 命令:
     * JOIN [channel]
     * PART [channel]
     * PRIVMSG [channel/user] :[message]
     * QUIT
     */
    /***
     * 此包均以 :cho.ppy.sh typecode
     * 001: Welcome to the osu!Bancho.
     * 375: bancho连接开始
     * 372: 提示
     * 376: 提示结束
     * 311: 例子 :cho.ppy.sh 311 -Spring_Night- -Spring_Night- https://osu.ppy.sh/u/17064371 * :https://osu.ppy.sh/u/17064371
     * 319:
     * 324: chinese +nt
     * 329: chinese 1704437352
     * 332: 进入时介绍什么频道的
     * 333: 固定格式的 BanchoBot!BanchoBot@cho.ppy.sh [id] <- 能通过 JOIN #[id] 重新加入频道
     * 353: 已有的人名,可能会发送多个 例子 :cho.ppy.sh 353 -Spring_Night- = #Chinese :nanaucky_
     * 366: 包353人名结束后固定的包
     * 401: 错误消息 No such nick
     * 403: No such channel
     * 464: 密码错误
     */
    int type;
    String message;
    String channel;

    static boolean check(String msg) {
        return msg.startsWith(":cho.ppy.sh");
    }

    public String getChannel() {
        return channel;
    }// write /join #Chinese
}

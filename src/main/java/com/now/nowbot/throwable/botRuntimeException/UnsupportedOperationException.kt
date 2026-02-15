package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.throwable.BotException
import com.now.nowbot.throwable.TipsRuntimeException
import com.now.nowbot.throwable.botRuntimeException.PermissionException.GroupException

open class UnsupportedOperationException(message: String?): TipsRuntimeException(message), BotException {

    // class AudioNotSafeForWork:
    //    UnsupportedOperationException("这种歌曲，还是有点唱不出口呢。")

    open class BotOperation(message: String?): UnsupportedOperationException(message) {

        class SenderUnavailable():
            BotOperation("无法获取消息的发送者。")

        class MessageUnavailable(messageID: Any):
            BotOperation("无法获取消息 $messageID。")

        class BotOffline:
            BotOperation("这个机器人并未上线。")

        class BotMainOffline:
            BotOperation("主机器人并未上线。")

        class NotFriend(userID: Any):
            BotOperation("机器人不是 $userID 的好友，无法发送消息。")

        class NotInGroup(groupID: Any):
            BotOperation("机器人不在 $groupID 群组中，无法发送消息。")

        class MustReply :
            BotOperation("必须选中回复的消息，机器人才会尝试撤回。")

        class Overtime:
            GroupException("无法操作，因为超过 2 分钟了。")

    }

    class ExpiredOauthBind:
        UnsupportedOperationException("""
            您的 Oauth2 绑定已经过期。
            如果要使用这个功能，您需要重新使用 Oauth2 绑定 (!bi)。
        """.trimIndent())

//    class Invalid:
//        UnsupportedOperationException("非法操作。")

    class InvalidMode(mode: OsuMode):
        UnsupportedOperationException("不支持绑定 ${mode.fullName} 这个游戏模式。")

    class NotGroup:
        UnsupportedOperationException("请在群聊中使用这个功能！")

    class NotSupporter:
        UnsupportedOperationException("如果要使用这个功能，您需要成为 osu!supporter。")

    class NoOauthBind:
        UnsupportedOperationException("如果要使用这个功能，您需要使用 Oauth2 绑定 (!bi)。")

    class OnlyStandard:
        UnsupportedOperationException("抱歉，本功能暂不支持除 Standard 模式以外的谱面！")

    class OnlyMania:
        UnsupportedOperationException("抱歉，本功能暂不支持除 Mania 模式以外的谱面！")
}
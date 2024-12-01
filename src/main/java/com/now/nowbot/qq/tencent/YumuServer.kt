package com.now.nowbot.qq.tencent

import com.now.nowbot.permission.PermissionImplement
import com.now.nowbot.qq.message.ImageMessage
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.TextMessage
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.util.QQMsgUtil
import com.yumu.YumuService
import com.yumu.model.packages.Command
import com.yumu.model.packages.QueryName
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

object YumuServer : YumuService {
    lateinit var userApiService: OsuUserApiService

    override suspend fun onCommand(param: Command.Request): Command.Response {
        val channel = Channel<Command.Response>(0, BufferOverflow.DROP_LATEST)
        val scope = CoroutineScope(coroutineContext)
        val contact = Contact(param.uid) {
            scope.launch { channel.send(it) }
        }
        val df = CompletableDeferred<MessageChain>()
        val event = Event(contact, param.command)
        PermissionImplement.onTencentMessage(event) {
            df.complete(it)
        }
        val response = withTimeoutOrNull(10.seconds) {
            val messageChain = df.await()
            messageToResponse(messageChain)
        }
        return response ?: Command.Response("结果处理超时啦, 压力比较大, 请稍后再试")
    }

    override suspend fun onQueryName(param: QueryName.Request): QueryName.Response {
        val userID = userApiService.getOsuId(param.name) ?: -1
        return QueryName.Response(param.name, userID)
    }

    fun messageToResponse(messageChain: MessageChain): Command.Response {
        val (textList, imageList) = messageChain.messageList.filter { it is TextMessage || it is ImageMessage }
            .partition { it is TextMessage }
        var text = textList.joinToString { it.toString() }
        if (text.contains("!ymbind")) {
            text = text.replace("!ymbind", "/bind [osu name]")
        }
        var image: String? = null
        var isUrl = false
        if (imageList.isNotEmpty()) {
            val imageData = imageList.first() as ImageMessage
            if (imageData.isUrl || imageData.isUri) {
                isUrl = true
                image = imageData.path
            } else {
                image = QQMsgUtil.byte2str(imageData.data)
            }
        }
        val result = Command.Response(
            text,
            image,
            isUrl,
        )
        return result
    }
}
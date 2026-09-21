package com.now.nowbot.qq.tencent

import com.now.nowbot.qq.message.ImageMessage
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.TextMessage
import com.now.nowbot.restrict.RestrictImplement
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.util.QQMsgUtil
import com.yumu.YumuService
import com.yumu.model.packages.Command
import com.yumu.model.packages.QueryName
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlin.time.Duration.Companion.seconds

object YumuServer : YumuService {
    lateinit var userApiService: OsuUserApiService

    override suspend fun onCommand(param: Command.Request): Command.Response {
        val channel = Channel<Command.Response>(0, BufferOverflow.DROP_LATEST)
        val scope = CoroutineScope(currentCoroutineContext())
        val contact = Contact(param.uid) {
            scope.launch { channel.send(it) }
        }
        val df = CompletableDeferred<MessageChain>()
        val event = Event(contact, param.command)
        RestrictImplement.onTencentMessage(event) {
            df.complete(it)
        }
        val response = try {
            withTimeout(10.seconds) {
                val messageChain = df.await()
                messageToResponse(messageChain)
            }
        } catch (_: TimeoutCancellationException) {
            return Command.Response("结果处理超时啦, 压力比较大, 请稍后再试")
        }
        return response
    }

    override suspend fun onQueryName(param: QueryName.Request): QueryName.Response {
        val userID = userApiService.getOsuID(param.name)
        return QueryName.Response(param.name, userID)
    }

    fun messageToResponse(messageChain: MessageChain): Command.Response {
        val (textList, imageList) = messageChain.messageList.filter { it is TextMessage || it is ImageMessage }
            .partition { it is TextMessage }
        val md = messageChain.markdown
        var text: String
        // markdown 会覆盖普通消息
        if (md == null) {
            text = textList.joinToString { it.toString() }
            if (text.contains("(!bi)")) {
                text = BindException.TokenExpiredException.OfficialTokenExpired().message!!
            }
        } else {
            text = md.toString()
        }
        var image: String? = null
        var isUrl = false
        if (imageList.isNotEmpty()) {
            val imageData = imageList.first() as ImageMessage
            if (imageData.isUrl) {
                isUrl = true
                image = imageData.path
            } else {
                image = QQMsgUtil.byte2str(imageData.data)
            }
        }
        val keyboard: String? = messageChain.keyboard?.toString()
        val result = Command.Response(
            text,
            image,
            isUrl = isUrl,
            isMarkdown = md != null,
            keyboard = keyboard
        )
        return result
    }
}
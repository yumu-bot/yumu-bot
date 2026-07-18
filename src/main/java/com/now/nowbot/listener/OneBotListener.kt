package com.now.nowbot.listener

import com.mikuac.shiro.annotation.GroupMessageHandler
import com.mikuac.shiro.annotation.common.Shiro
import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.event.message.GroupMessageEvent
import com.now.nowbot.cache.QQMessageCacheProvider
import com.now.nowbot.config.Permission
import com.now.nowbot.permission.PermissionImplement
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.IdempotentService
import com.now.nowbot.service.MessageService
import com.now.nowbot.throwable.BotException
import com.now.nowbot.throwable.botRuntimeException.LogException
import com.now.nowbot.util.ContextUtil
import kotlinx.coroutines.TimeoutCancellationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.core.annotation.Order
import org.springframework.core.codec.DecodingException
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.client.UnknownHttpStatusCodeException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import kotlin.Exception
import kotlin.IllegalArgumentException
import kotlin.String
import kotlin.Suppress
import kotlin.Throwable
import kotlin.Throws
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

////////////////////////////////////////////////////////////////////
//                          _ooOoo_                               //
//                         o8888888o                              //
//                         88" . "88                              //
//                         (| ^_^ |)                              //
//                         O\  =  /O                              //
//                      ____/`---'\____                           //
//                    .'  \\|     |//  `.                         //
//                   /  \\|||  :  |||//  \                        //
//                  /  _||||| -:- |||||-  \                       //
//                  |   | \\\  -  /// |   |                       //
//                  | \_|  ''\---/''  |   |                       //
//                  \  .-\__  `-`  ___/-. /                       //
//                ___`. .'  /--.--\  `. . ___                     //
//              ."" '<  `.___\_<|>_/___.'  >'"".                  //
//            | | :  `- \`.;`\ _ /`;.`/ - ` : | |                 //
//            \  \ `-.   \_ __\ /__ _/   .-` /  /                 //
//      ========`-.____`-.___\_____/___.-`____.-'========         //
//                           `=---='                              //
//      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^        //
//              佛祖保佑       永不宕机     永无BUG                   //
////////////////////////////////////////////////////////////////////

@Shiro
@Order(9)
@Component("OneBotListener")
@Suppress("UNUSED")
class OneBotListener(
    private val idempotentService: IdempotentService,
    private val messageCacheProvider: QQMessageCacheProvider,
) {
    var log: Logger = LoggerFactory.getLogger(OneBotListener::class.java)

    @Throws(BeansException::class)
    fun init(beanMap: MutableMap<String?, MessageService<*>?>?) {
        messageServiceMap = beanMap
    }

    @GroupMessageHandler
    @Async
    fun handle(bot: Bot, groupEvent: GroupMessageEvent) {
        val groupID = groupEvent.groupId
        val messageID = "[${groupID}|${groupEvent.sender.userId}]${groupEvent.subType}(${groupEvent.time})"
        if (!idempotentService.checkByMessageId(messageID)) {
            return
        }

        // log.debug("收到消息 {}", messageID)

        var nowTime = System.currentTimeMillis()
        if (groupEvent.time < 1e10) {
            nowTime /= 1000
        }

        // 对于超过 30秒 的消息直接舍弃, 解决重新登陆后疯狂刷命令
        if (nowTime - groupEvent.time > 30) return

        val event = com.now.nowbot.qq.onebot.event.GroupMessageEvent(bot, groupEvent)

        messageCacheProvider.putMessage(
            message = groupEvent
        )

        // if (event.getGroup().getId() != 746671531) return;
        if (event.sender.contactID == 365246692L) {
            ContextUtil.setContext("isTest", java.lang.Boolean.TRUE)
        }
        try {
            PermissionImplement.onMessage(event) { event: MessageEvent, e: Throwable -> this.errorHandle(event, e) }
        } finally {
            ContextUtil.remove()
        }
    }

    fun errorHandle(event: MessageEvent, e: Throwable) {
        when (e) {
            is BotException -> handleBotException(event, e)
            is SocketTimeoutException,
            is ConnectException,
            is TimeoutException,
            is TimeoutCancellationException,
            is UnknownHttpStatusCodeException
                -> handleNetworkException(event, e)
            is LogException -> log.info(e.message)
            is ExecutionException -> event.replyAsync(MessageChain(e.cause!!.message!!))
            is IllegalArgumentException -> log.error("正则异常", e)
            is DecodingException -> log.error("JSON 解码异常", e)
            else -> handleOtherException(event, e)
        }
    }

    private fun handleBotException(event: MessageEvent, e: BotException) {
        if (e.hasImage()) {
            event.replyAsync(e.image!!)
        } else {
            event.replyAndRecallAsync(e.message ?: return, RECALL_TIME)
        }
    }

    private fun handleNetworkException(event: MessageEvent, e: Exception) {
        log.info("连接超时：", e)
        event.replyAndRecallAsync("请求超时。", RECALL_TIME)
    }

    private fun handleOtherException(event: MessageEvent, e: Throwable) {
        if (Permission.isSuperAdmin(event.sender.contactID)) {
            event.replyAndRecallAsync(e, RECALL_TIME)
        }
        log.error("捕捉其他异常：", e)
    }

    companion object {
        val RECALL_TIME: Duration = 100.seconds
        private var messageServiceMap: MutableMap<String?, MessageService<*>?>? = null
    }
}

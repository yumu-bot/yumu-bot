package com.now.nowbot.listener

import com.mikuac.shiro.annotation.GroupMessageHandler
import com.mikuac.shiro.annotation.common.Shiro
import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.event.message.GroupMessageEvent
import com.now.nowbot.config.Permission
import com.now.nowbot.permission.PermissionImplement
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.IdempotentService
import com.now.nowbot.service.MessageService
import com.now.nowbot.throwable.BotException
import com.now.nowbot.throwable.botRuntimeException.LogException
import com.now.nowbot.util.ContextUtil
import jakarta.annotation.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.core.annotation.Order
import org.springframework.core.codec.DecodingException
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.client.UnknownHttpStatusCodeException
import java.lang.Boolean
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.ExecutionException
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Throwable
import kotlin.Throws

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

@Shiro @Order(9) @Component("OneBotListener") @Suppress("UNUSED")
class OneBotListener {
    var log: Logger = LoggerFactory.getLogger(OneBotListener::class.java)

    @Resource
    var idempotentService: IdempotentService? = null

    @Throws(BeansException::class) fun init(beanMap: MutableMap<String?, MessageService<*>?>?) {
        messageServiceMap = beanMap
    }

    @GroupMessageHandler @Async fun handle(bot: Bot?, onebotEvent: GroupMessageEvent) {
        val groupId = onebotEvent.groupId
        // val message = ShiroUtils.unescape(onebotEvent.message)
        val messageId = String.format(
            "[%s|%s]%s(%s)",
            groupId.toString(),
            onebotEvent.sender.userId.toString(),
            onebotEvent.subType,
            onebotEvent.time.toString()
        )
        if (!idempotentService!!.checkByMessageId(messageId)) {
            return
        }
        log.debug("收到消息 {}", messageId)
        var nowTime = System.currentTimeMillis()
        if (onebotEvent.time < 1e10) {
            nowTime /= 1000
        }
        // 对于超过 30秒 的消息直接舍弃, 解决重新登陆后疯狂刷命令
        if (nowTime - onebotEvent.time > 30) return
        val event = com.now.nowbot.qq.onebot.event.GroupMessageEvent(bot, onebotEvent)
        // if (event.getGroup().getId() != 746671531) return;
        if (event.sender.id == 365246692L) {
            ContextUtil.setContext("isTest", Boolean.TRUE)
        }
        try {
            PermissionImplement.onMessage(event) { event: MessageEvent, e: Throwable -> this.errorHandle(event, e) }
        } finally {
            ContextUtil.remove()
        }
    }

    fun errorHandle(event: MessageEvent, e: Throwable) {
        if (e is BotException) {
            // 有些网络请求异常被包装在这里
            if (e.hasImage()) {
                event.reply(e.image)
            } else {
                event.reply(e.message).recallIn(RECALL_TIME.toLong())
            }
        } else if (e is SocketTimeoutException || e is ConnectException || e is UnknownHttpStatusCodeException) {
            log.info("连接超时:", e)
            event.reply("请求超时 (HTTP 408 Request Timeout)\n可能是 Bot 达到了 API 请求上限。\n请稍后再试。").recallIn(
                RECALL_TIME.toLong()
            )
        } else if (e is LogException) {
            log.info(e.message)
        } else if (e is ExecutionException) {
            event.reply(MessageChain(e.cause!!.message!!))
        } else if (e is IllegalArgumentException) {
            log.error("正则异常", e)
        } else if (e is DecodingException) {
            log.error("JSON 解码异常", e)
        } else {
            if (Permission.isSuperAdmin(event.getSender().getId())) {
                event.reply(e.message).recallIn(RECALL_TIME.toLong())
            }
            log.error("捕捉其他异常", e)
        }
    }

    companion object {
        var RECALL_TIME: Int = 1000 * 100
        private var messageServiceMap: MutableMap<String?, MessageService<*>?>? = null
    }
}

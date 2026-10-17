package com.now.nowbot.service

import com.now.nowbot.qq.event.MessageEvent

/**
 * 处理「不是指令」的普通消息。
 *
 * 部分平台收不到一次一问一答里的普通回复（例如 !am 手动补录别名），
 * 需要按会话接收用户的下一条直接输入。实现该接口的服务会在消息不构成指令时被调用。
 */
interface PassiveMessageHandler {
    /**
     * @param messageText 去掉首尾空白后的消息原文。
     * @return true 表示这条消息已经被消费，不再交给其他服务。
     */
    fun handlePassiveMessage(event: MessageEvent, messageText: String): Boolean
}

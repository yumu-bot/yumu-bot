package com.now.nowbot.util

import com.now.nowbot.config.YumuConfig
import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.Message
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import java.nio.ByteBuffer
import java.util.*

object QQMsgUtil {
    private val base64Util: Base64.Encoder = Base64.getEncoder()
    private val FILE_DATA: MutableMap<String, FileData> = HashMap()
    private var LocalBotList: List<Long>? = null
    private var LocalUrl: String? = null
    private var PublicUrl: String? = null

    @JvmStatic fun init(yumuConfig: YumuConfig) {
        LocalBotList = yumuConfig.privateDevice
        LocalUrl = "${yumuConfig.privateUrl}/pub/file/%s"
        PublicUrl = "${yumuConfig.publicUrl}/pub/file/%s"
    }

    @JvmStatic fun byte2str(data: ByteArray?): String {
        if (Objects.isNull(data)) return ""
        return base64Util.encodeToString(data)
    }

    inline fun <reified T : Message> getType(msg: MessageChain, clazz: Class<T>): T? {
        return msg.messageList.filter { clazz.isAssignableFrom(it.javaClass) }.filterIsInstance<T>().firstOrNull()
    }

    fun getImage(image: ByteArray): MessageChain {
        return MessageChainBuilder().addImage(image).build()
    }

    fun getTextAndImage(text: String, image: ByteArray): MessageChain {
        return MessageChainBuilder().addText(text).addImage(image).build()
    }

    fun getImages(images: List<ByteArray>): List<MessageChain> {
        var builder = MessageChainBuilder()
        val bs = ArrayList<MessageChain>()

        for (i in images.indices) {
            val image = images[i]

            // qq 一次性只能发 20 张图
            if (i >= 20 && i % 20 == 0) {
                bs.add(builder.build())
                builder = MessageChainBuilder()
            }

            builder.addImage(image)
        }

        bs.add(builder.build())

        return bs
    }

    fun sendImages(event: MessageEvent, images: List<ByteArray>) {
        val bs = getImages(images)

        for (b in bs) {
            event.reply(b)
            Thread.sleep(1000L)
        }
    }

    /*
    private fun beforeContact(from: Contact) {
        from.sendMessage("正在处理图片请稍候...");
    }
     */

    inline fun <reified T : Message> getTypeAll(msg: MessageChain, clazz: Class<T>): List<T> {
        return msg.messageList.filter { clazz.isAssignableFrom(it.javaClass) }.filterIsInstance<T>()
    }

    @JvmStatic fun sendImageAndText(event: MessageEvent, image: ByteArray, text: String) {
        val from = event.subject
        sendImageAndText(from, image, text)
    }

    @JvmStatic fun sendImageAndText(from: Contact, image: ByteArray, text: String) {
        // beforeContact(from)
        from.sendMessage(MessageChainBuilder().addImage(image).addText(text).build())
    }

    @JvmStatic fun sendGroupFile(event: MessageEvent, name: String?, data: ByteArray?) {
        val from = event.subject

        if (from is Group) {
            from.sendFile(data, name ?: "Yumu-file")
        }
    }

    @JvmStatic fun sendGroupFile(group: Group, name: String?, data: ByteArray?) {
        group.sendFile(data, name)
    }

    @JvmStatic fun getFileUrl(data: ByteArray, name: String?): String {
        val key = UUID.randomUUID().toString()
        FILE_DATA[key] = FileData(name, ByteBuffer.wrap(data))
        return String.format(LocalUrl!!, key)
    }

    @JvmStatic fun getFilePubUrl(data: ByteArray, name: String?): String {
        val key = UUID.randomUUID().toString()
        FILE_DATA[key] = FileData(name, ByteBuffer.wrap(data))
        return String.format(PublicUrl!!, key)
    }

    @JvmStatic fun getFileData(key: String): FileData? {
        return FILE_DATA[key]
    }

    @JvmStatic fun removeFileUrl(url: String) {
        val index = url.lastIndexOf("/pub/file") + 10
        val key = url.substring(index)
        println(key)
        FILE_DATA.remove(key)
    }

    @JvmStatic fun botInLocal(botQQ: Long): Boolean {
        return LocalBotList!!.contains(botQQ)
    }

    @JvmRecord data class FileData(val name: String?, val bytes: ByteBuffer)
}

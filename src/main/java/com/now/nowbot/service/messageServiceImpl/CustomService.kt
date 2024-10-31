package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.FileConfig
import com.now.nowbot.dao.BindDao
import com.now.nowbot.mapper.UserProfileMapper
import com.now.nowbot.model.BinUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.ReplyMessage
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.CustomService.CustomParam
import com.now.nowbot.service.messageServiceImpl.CustomService.Operate.*
import com.now.nowbot.service.messageServiceImpl.CustomService.Type.*
import com.now.nowbot.throwable.serviceException.BindException
import com.now.nowbot.throwable.serviceException.CustomException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.DataUtil.getMarkdownFile
import com.now.nowbot.util.Instruction
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.client.RestTemplate

@Service("CUSTOM")
class CustomService(
        private val restTemplate: RestTemplate,
        private val bindDao: BindDao,
        private val imageService: ImageService,
        private val userProfileMapper: UserProfileMapper,
        fileConfig: FileConfig,
) : MessageService<CustomParam> {

    init {
        FILE_DIV_PATH = Path.of(fileConfig.bgdir, "user-profile")
    }

    // url == null 是删除图片
    @JvmRecord
    data class CustomParam(
            val uid: Long,
            val type: Type,
            val url: String?,
    )

    @Throws(Throwable::class)
    override fun isHandle(
            ev: MessageEvent,
            messageText: String,
            data: DataValue<CustomParam>,
    ): Boolean {
        var event: MessageEvent? = ev
        val from = event!!.subject

        val matcher = Instruction.CUSTOM.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        val u: BinUser
        try {
            u = bindDao.getUserFromQQ(event.sender.id, true)
        } catch (e: BindException) {
            throw CustomException(CustomException.Type.CUSTOM_Me_TokenExpired)
        }

        val firstMessage = event.message.messageList.first

        var imgPath: String? = null

        val operateStr = matcher.group("operate")
        val typeStr = matcher.group("type")

        var operate: Operate
        val type: Type

        if (StringUtils.hasText(typeStr)) {
            operate =
                    when (operateStr) {
                        "s",
                        "save",
                        "a",
                        "add" -> ADD
                        "c",
                        "clear",
                        "d",
                        "delete",
                        "r",
                        "remove" -> DELETE
                        else -> UNKNOWN
                    }

            type =
                    when (typeStr) {
                        "c",
                        "card",
                        "cards" -> CARD
                        "m",
                        "mascot",
                        "mascots" -> MASCOT
                        else -> BANNER
                    }
        } else {
            // 只有一个字段，默认添加，直接跳出
            operate = ADD

            type =
                    when (operateStr) {
                        "c",
                        "card" -> CARD
                        "m",
                        "mascot",
                        "mascots" -> MASCOT
                        else -> BANNER
                    }
        }

        var reply: ReplyMessage? = null

        if (operate == ADD) {
            if (firstMessage is ReplyMessage) {
                reply = firstMessage
            } else {
                operate = UNKNOWN
            }
        }

        when (operate) {
            ADD -> {
                // 正常
                if (event.bot == null) {
                    throw CustomException(CustomException.Type.CUSTOM_Receive_NoBot)
                }

                val msg = event.bot.getMessage(reply!!.id)

                if (msg == null || !event.isImage) {
                    // 消息为空，并且不是回复的图片。询问是否删除
                    val receipt =
                            from.sendMessage(CustomException.Type.CUSTOM_Question_Clear.message)

                    val lock = ASyncMessageUtil.getLock(event, (30 * 1000).toLong())
                    event = lock.get()

                    if (
                            event == null ||
                                    !event.rawMessage.uppercase(Locale.getDefault()).contains("OK")
                    ) {
                        // 不删除。失败撤回
                        from.recall(receipt)
                        return false
                    } else {
                        // 确定删除
                        from.recall(receipt)
                    }
                } else {
                    // 成功
                    imgPath = event.image!!.path // img = QQMsgUtil.getType(msg, ImageMessage.class)
                }
            }
            UNKNOWN -> {
                // 不是回复。发送引导
                try {
                    val md = getMarkdownFile("Help/custom.md")
                    val image = imageService.getPanelA6(md, "help")
                    from.sendImage(image)
                    return false
                } catch (e: Exception) {
                    throw CustomException(CustomException.Type.CUSTOM_Instructions)
                }
            }

            DELETE -> {
                imgPath = null
            }
        }

        data.value = CustomParam(u.osuID, type, imgPath)

        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: CustomParam) {
        val fileName = "${param.uid}-${param.type}.png"
        val path = FILE_DIV_PATH.resolve(fileName)

        var imgBytes: ByteArray? = byteArrayOf()

        if (param.url != null) {
            imgBytes = restTemplate.getForObject(param.url, ByteArray::class.java)
            if (imgBytes == null) {
                throw CustomException(CustomException.Type.CUSTOM_Receive_PictureFetchFailed)
            }
        }

        val profile = userProfileMapper.getProfileById(param.uid)

        if (param.url != null) {
            // 保存
            try {
                Files.write(path, imgBytes!!)
            } catch (e: Exception) {
                log.error("自定义：文件添加失败", e)
                throw CustomException(CustomException.Type.CUSTOM_Set_Failed, param.type)
            }

            val pathStr = path.toAbsolutePath().toString()
            when (param.type) {
                CARD -> profile.card = pathStr
                BANNER -> profile.banner = pathStr
                MASCOT -> profile.mascot = pathStr
            }
            userProfileMapper.saveAndFlush(profile)
            throw CustomException(CustomException.Type.CUSTOM_Set_Success, param.type)
        } else {
            // 删除
            try {
                Files.delete(path)
            } catch (e: NoSuchFileException) {
                throw CustomException(CustomException.Type.CUSTOM_Clear_NoSuchFile, param.type)
            } catch (e: Exception) {
                log.error("自定义：文件删除失败", e)
                throw CustomException(CustomException.Type.CUSTOM_Clear_Failed, param.type)
            }
            when (param.type) {
                CARD -> profile.card = null
                BANNER -> profile.banner = null
                MASCOT -> profile.mascot = null
            }
            userProfileMapper.saveAndFlush(profile)
            throw CustomException(CustomException.Type.CUSTOM_Clear_Success, param.type)
        }
    }

    internal enum class Operate {
        ADD,
        DELETE,
        UNKNOWN,
    }

    enum class Type {
        BANNER,
        CARD,
        MASCOT,
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(CustomService::class.java)
        private var FILE_DIV_PATH: Path = Path("")
    }
}

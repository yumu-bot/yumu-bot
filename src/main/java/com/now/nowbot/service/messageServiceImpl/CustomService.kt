package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.FileConfig
import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.UserProfileItem
import com.now.nowbot.entity.UserProfileKey
import com.now.nowbot.mapper.UserProfileItemRepository
import com.now.nowbot.mapper.UserProfileMapper
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.UserProfile
import com.now.nowbot.model.UserProfile.Type.*
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.ReplyMessage
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.CustomService.CustomParam
import com.now.nowbot.service.messageServiceImpl.CustomService.Operate.*
import com.now.nowbot.throwable.botException.CustomException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.DataUtil.getMarkdownFile
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.*

@Service("CUSTOM")
class CustomService(
        private val webClient: WebClient,
        private val bindDao: BindDao,
        private val imageService: ImageService,
        private val userProfileMapper: UserProfileMapper,
        // todo: 等待数据迁移后使用
        private val userProfileItemRepository: UserProfileItemRepository,
        fileConfig: FileConfig,
) : MessageService<CustomParam> {

    init {
        FILE_DIV_PATH = Path.of(fileConfig.bgdir, "user-profile")
    }

    // url == null 是删除图片
    @JvmRecord
    data class CustomParam(
        val uid: Long,
        val type: UserProfile.Type,
        val url: String?,
    )

    @Throws(Throwable::class)
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<CustomParam>,
    ): Boolean {
        var ev: MessageEvent? = event
        val from = ev!!.subject

        val matcher = Instruction.CUSTOM.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        val u: BindUser = bindDao.getBindFromQQ(ev.sender.id, true)

        val firstMessage = ev.message.messageList.first()

        val imgPath: String?

        val operateStr = matcher.group("operate")
        val typeStr = matcher.group("type")

        var operate: Operate
        val type: UserProfile.Type

        if (typeStr.isNullOrBlank().not()) {
            operate = when (operateStr) {
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

            type = when (typeStr) {
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

            type = when (operateStr) {
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
                if (ev.bot == null) {
                    throw CustomException(CustomException.Type.CUSTOM_Receive_NoBot)
                }

                val msg = ev.bot.getMessage(reply!!.id)

                if (msg == null || !ev.isImage) {
                    // 消息为空，并且不是回复的图片。询问是否删除
                    val receipt =
                        from.sendMessage(CustomException.Type.CUSTOM_Question_Clear.message)

                    val lock = ASyncMessageUtil.getLock(ev, (30 * 1000).toLong())
                    ev = lock.get()

                    if (
                        ev == null ||
                        !ev.rawMessage.uppercase(Locale.getDefault()).contains("OK")
                    ) {
                        // 不删除。失败撤回
                        from.recall(receipt)
                        return false
                    } else {
                        // 确定删除
                        from.recall(receipt)
                        imgPath = null
                    }
                } else {
                    // 成功
                    imgPath = ev.image!!.path // img = QQMsgUtil.getType(msg, ImageMessage.class)
                }
            }

            UNKNOWN -> {
                // 不是回复。发送引导
                try {
                    val md = getMarkdownFile("Help/custom.md")
                    val image = imageService.getPanelA6(md, "help")
                    ev.reply(image)
                    return false
                } catch (e: Exception) {
                    throw CustomException(CustomException.Type.CUSTOM_Instructions)
                }
            }

            DELETE -> {
                imgPath = null
            }
        }

        data.value = CustomParam(u.userID, type, imgPath)

        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: CustomParam) {
        val fileName = "${param.uid}-${param.type}.png"
        val path = FILE_DIV_PATH.resolve(fileName)

        var imgBytes: ByteArray? = byteArrayOf()

        if (param.url != null) {
            imgBytes = webClient.get().uri(param.url).retrieve().bodyToMono(ByteArray::class.java).block()
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
                MASCOT -> profile.mascot = pathStr
                else -> profile.banner = pathStr
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
                MASCOT -> profile.mascot = null
                else -> profile.banner = null
            }
            userProfileMapper.saveAndFlush(profile)
            throw CustomException(CustomException.Type.CUSTOM_Clear_Success, param.type)
        }
    }

    /**
     * 设置新的自定义配置, 并等待审核
     */
    fun pendingVerificationQueue(userId:Long, type: UserProfile.Type, path:Path) {
        val pathStr = path.toAbsolutePath().toString()
        val item = UserProfileItem(userId, type.column, pathStr)
        userProfileItemRepository.saveAndFlush(item)
    }

    /**
     * 删除某类型的自定义配置
     */
    fun removeProfileItem(userId: Long, type: UserProfile.Type) {
        userProfileItemRepository.deleteByUserIdAndType(UserProfileKey(userId, type.column))
    }

    /**
     * 删除所有的自定义配置
     */
    fun removeAllProfileItem(userId: Long) {
        userProfileItemRepository.deleteAllByUserId(userId)
    }

    internal enum class Operate {
        ADD,
        DELETE,
        UNKNOWN,
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(CustomService::class.java)
        lateinit var FILE_DIV_PATH: Path
    }
}

package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.FileConfig
import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.entity.UserProfileItem
import com.now.nowbot.entity.UserProfileKey
import com.now.nowbot.entity.UserProfileLite
import com.now.nowbot.mapper.UserProfileItemRepository
import com.now.nowbot.mapper.UserProfileRepository
import com.now.nowbot.model.UserProfile
import com.now.nowbot.model.UserProfile.UserProfileType.*
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.CustomService.CustomParam
import com.now.nowbot.service.messageServiceImpl.CustomService.CustomOperate.*
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.Instruction
import okio.IOException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.regex.Matcher

@Service("CUSTOM")
class CustomService(
    private val webClient: WebClient,
    private val bindDao: BindDao,
    private val userProfileRepository: UserProfileRepository,
        // todo: 等待数据迁移后使用
    private val userProfileItemRepository: UserProfileItemRepository,
    fileConfig: FileConfig,
) : MessageService<CustomParam> {

    init {
        FILE_DIV_PATH = Path.of(fileConfig.bgdir, "user-profile")
    }

    // url == null 是删除图片
    data class CustomParam(
        val uid: Long,
        val type: UserProfile.UserProfileType,
        val url: String?,
    )

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<CustomParam>): Boolean {

        val matcher = Instruction.CUSTOM.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        data.value = getParam(event, matcher)

        return true
    }

    private fun getParam(event: MessageEvent, matcher: Matcher): CustomParam {
        val operate = CustomOperate.getOperate(matcher.group("operate"))
        val type = UserProfile.UserProfileType.getType(matcher.group("type"))

        val id = bindDao.getBindFromQQ(event.sender.id).userID

        return when (operate) {
            ADD -> if (event.hasImage() && !event.image?.path.isNullOrEmpty()) {
                val url = event.image!!.path!!

                CustomParam(id, type, url)
            } else {
                waitImage(id, type, event)
            }

            DELETE -> confirmDelete(id, type, event)
            UNKNOWN -> waitImage(id, type, event)
        }
    }

    private fun confirmDelete(id: Long, type: UserProfile.UserProfileType, event: MessageEvent): CustomParam {
        val receipt =
            event.reply("""
                您想要清除你的自定义 ${type.name} 吗？回复 OK 确认。
                如果并不想，请无视。
                """.trimIndent())

        val lock = ASyncMessageUtil.getLock(event, 60 * 1000)
        val ev = lock.get()

        if (ev != null && ev.rawMessage.contains("OK", ignoreCase = true)) {

            receipt.recall()
            return CustomParam(id, type, null)
        } else {
            receipt.recall()
            throw TipsException("清除过程已中止。")
        }
    }

    private fun waitImage(id: Long, type: UserProfile.UserProfileType, event: MessageEvent): CustomParam {
        // 消息为空，并且不是回复的图片。询问是否删除
        val receipt =
            event.reply("请发送您需要上传的图片 (${type.name})。")

        val lock = ASyncMessageUtil.getLock(event, 60 * 1000)
        val ev = lock.get()

        if (ev != null && ev.hasImage() && !ev.image?.path.isNullOrEmpty()) {
            val url = ev.image!!.path!!


            receipt.recall()
            return CustomParam(id, type, url)
        } else {

            receipt.recall()
            throw TipsException("没有获取到您发送的图片。请重试。")
        }
    }

    override fun handleMessage(event: MessageEvent, param: CustomParam): ServiceCallStatistic? {
        val fileName = "${param.uid}-${param.type}.png"
        val path = FILE_DIV_PATH.resolve(fileName)

        val imgBytes: ByteArray = if (param.url != null) try {
            webClient
                .get()
                .uri(param.url).retrieve()
                .bodyToMono(ByteArray::class.java).block()!!

        } catch (_: Exception) {
            throw IllegalStateException.Fetch("自定义图片")
        } else byteArrayOf()

        val profile = userProfileRepository.findTopById(param.uid) ?:
        UserProfileLite()
            .apply {
                this.userId = param.uid
                this.id = param.uid
            }

        if (param.url != null) {
            if (!Files.exists(FILE_DIV_PATH)) {
                try {
                    Files.createDirectory(FILE_DIV_PATH)
                } catch (e: IOException) {
                    log.error("自定义：创建目录失败", e)
                }
            }

            // 保存
            try {
                Files.write(path, imgBytes)
            } catch (e: Exception) {
                log.error("自定义：文件添加失败", e)
                event.reply("设置 ${param.type} 失败。错误已记录。")
                return ServiceCallStatistic.building(event)
            }

            profile.applyType(param.type, path.toAbsolutePath().toString())

            userProfileRepository.saveAndFlush(profile)
            event.reply("设置 ${param.type} 成功！")
            return ServiceCallStatistic.building(event)
        } else {
            // 删除
            try {
                Files.delete(path)
            } catch (_: NoSuchFileException) {
                event.reply("""
                    删除 ${param.type} 失败。
                    数据库里不存在你设置的自定义图片呢。
                """.trimIndent())
                return ServiceCallStatistic.building(event)
            } catch (e: Exception) {
                log.error("自定义：文件删除失败", e)
                event.reply("删除 ${param.type} 失败。错误已记录。")
                return ServiceCallStatistic.building(event)
            }

            profile.applyType(param.type, null)

            userProfileRepository.saveAndFlush(profile)
            event.reply("删除 ${param.type} 成功！")
            return ServiceCallStatistic.building(event)
        }
    }

    /**
     * 设置新的自定义配置, 并等待审核
     */
    fun pendingVerificationQueue(userId:Long, type: UserProfile.UserProfileType, path:Path) {
        val pathStr = path.toAbsolutePath().toString()
        val item = UserProfileItem(userId, type.column, pathStr)
        userProfileItemRepository.saveAndFlush(item)
    }

    /**
     * 删除某类型的自定义配置
     */
    fun removeProfileItem(userId: Long, type: UserProfile.UserProfileType) {
        userProfileItemRepository.deleteByUserIdAndType(UserProfileKey(userId, type.column))
    }

    /**
     * 删除所有的自定义配置
     */
    fun removeAllProfileItem(userId: Long) {
        userProfileItemRepository.deleteAllByUserId(userId)
    }

    internal enum class CustomOperate {
        ADD,
        DELETE,
        UNKNOWN,

        ;
        companion object {
            fun getOperate(input: String?): CustomOperate {
                return when (input) {
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
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(CustomService::class.java)
        lateinit var FILE_DIV_PATH: Path


        private fun UserProfileLite.applyType(type: UserProfile.UserProfileType, path: String? = null) {

            when (type) {
                CARD -> card = path
                MASCOT -> mascot = path
                AVATAR_FRAME -> avatarFrame = path

                INFO -> infoPanel = path
                SCORE -> scorePanel = path
                PPM -> ppmPanel = path

                else -> banner = path
            }
        }
    }
}
